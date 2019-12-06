package net.zero9178.mbed.editor

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Key
import com.intellij.ui.EditorNotifications

/**
 * Per project component which records if any files called mbed_lib.json or mbed_app.json have changed since last
 * cmake regeneration. Sets NEEDS_RELOAD on the project when needed
 */
class MbedAppLibDirtyMarker : StartupActivity.Background {

    companion object {
        val NEEDS_RELOAD = Key<Boolean>("MBED_NEEDS_RELOAD")
    }

    override fun runActivity(project: Project) {
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