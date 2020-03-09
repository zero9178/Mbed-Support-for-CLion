package net.zero9178.mbed.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.EditorNotifications
import com.intellij.ui.content.ContentFactory
import com.intellij.util.io.exists
import icons.MbedIcons
import net.zero9178.mbed.gui.MbedPackagesView
import java.nio.file.Paths

/**
 * Per project component which records if any files called mbed_lib.json or mbed_app.json have changed since last
 * cmake regeneration. Sets NEEDS_RELOAD on the project when needed
 */
class MbedAppLibDirtyMarker : StartupActivity.Background {

    companion object {
        val NEEDS_RELOAD = Key<Boolean>("MBED_NEEDS_RELOAD")
        private const val ID = "MBED_PACKAGE_VIEW"
    }

    override fun runActivity(project: Project) {
        val basePath = project.basePath ?: return
        val create = {
            ApplicationManager.getApplication().invokeLater {
                val toolWindow = ToolWindowManager.getInstance(project)
                    .registerToolWindow(ID, false, ToolWindowAnchor.BOTTOM, project, true)
                toolWindow.icon = MbedIcons.MBED_ICON_13x13
                val packageView = MbedPackagesView.getInstance(project)
                toolWindow.title = "Mbed"
                toolWindow.stripeTitle = toolWindow.title
                val content = ContentFactory.SERVICE.getInstance().createContent(
                    packageView.panel,
                    "",
                    false
                )
                content.preferredFocusableComponent = packageView.panel
                toolWindow.contentManager.addContent(
                    content
                )
            }
        }
        if (Paths.get(basePath).resolve("mbed_app.json").exists()) {
            create()
        }
        project.messageBus.connect(project).subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                events.forEach {
                    when (it) {
                        is VFileCreateEvent -> {
                            if (it.file?.name == "mbed_app.json") {
                                create()
                            }
                        }
                        is VFileDeleteEvent -> {
                            if (it.file.name == "mbed_app.json") {
                                ToolWindowManager.getInstance(project).unregisterToolWindow(ID)
                            }
                        }
                    }
                }
            }
        })
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val vfs =
                    FileDocumentManager.getInstance().getFile(event.document) ?: return super.documentChanged(event)
                if (vfs.name == "mbed_lib.json" || vfs.name == "mbed_app.json") {
                    project.putUserData(NEEDS_RELOAD, true)
                    EditorNotifications.getInstance(project).updateNotifications(vfs)
                }
                super.documentChanged(event)
            }
        }, project)
    }
}