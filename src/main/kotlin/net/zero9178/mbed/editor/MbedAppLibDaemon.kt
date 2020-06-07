package net.zero9178.mbed.editor

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.EditorNotifications
import com.intellij.util.io.exists
import com.intellij.util.messages.Topic
import net.zero9178.mbed.gui.MbedPackagesView
import net.zero9178.mbed.state.MbedProjectState
import java.nio.file.Paths

/**
 * Per project component which records if any files called mbed_lib.json or mbed_app.json have changed since last
 * cmake regeneration. Sets NEEDS_RELOAD on the project when needed
 */
class MbedAppLibDaemon : StartupActivity.Background {

    companion object {
        val PROJECT_NEEDS_RELOAD = Key<Boolean>("MBED_NEEDS_RELOAD")

        @JvmStatic
        val PROJECT_IS_MBED_PROJECT = Key<Boolean>("IS_MBED_PROJECT")

        @JvmStatic
        val MBED_PROJECT_CHANGED = Topic.create("MBED_PROJECT_CHANGED", MbedAppListener::class.java)
    }

    interface MbedAppListener {
        fun statusChanged(isMbedProject: Boolean) {}
    }

    override fun runActivity(project: Project) {
        val basePath = project.basePath ?: return
        val exists = Paths.get(basePath).resolve("mbed_app.json").exists()
        project.putUserData(PROJECT_IS_MBED_PROJECT, exists)
        project.messageBus.syncPublisher(MBED_PROJECT_CHANGED).statusChanged(exists)
        if (exists) {
            MbedPackagesView.getInstance(project).refreshTree()
        }
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val vfs =
                    FileDocumentManager.getInstance().getFile(event.document) ?: return super.documentChanged(event)
                if (vfs.name.toLowerCase() == "mbed_lib.json" || vfs.name.toLowerCase() == "mbed_app.json") {
                    project.putUserData(PROJECT_NEEDS_RELOAD, true)
                    EditorNotifications.getInstance(project).updateNotifications(vfs)
                }
                super.documentChanged(event)
            }
        }, MbedProjectState.getInstance(project))
    }
}

private class MbedAppFileListener(private val project: Project) : BulkFileListener {
    override fun after(events: MutableList<out VFileEvent>) {
        val basePath = project.basePath ?: return
        events.forEach {
            val path = it.file?.path ?: return@forEach
            if (Paths.get(path) == Paths.get(basePath).resolve("mbed_app.json")) {
                val value = when (it) {
                    is VFileCreateEvent -> true
                    is VFileDeleteEvent -> false
                    else -> return@forEach
                }
                project.putUserData(MbedAppLibDaemon.PROJECT_IS_MBED_PROJECT, value)
                project.messageBus.syncPublisher(MbedAppLibDaemon.MBED_PROJECT_CHANGED).statusChanged(value)
            }
        }
    }
}