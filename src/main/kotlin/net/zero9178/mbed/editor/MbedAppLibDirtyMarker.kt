package net.zero9178.mbed.editor

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.EditorNotifications

class MbedAppLibDirtyMarker(private val myProject: Project) {

    companion object {
        val NEEDS_RELOAD = Key<Boolean>("MBED_NEEDS_RELOAD")
    }

    init {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val vfs =
                    FileDocumentManager.getInstance().getFile(event.document) ?: return super.documentChanged(event)
                if (vfs.name == "mbed_lib.json" || vfs.name == "mbed_app.json") {
                    myProject.putUserData(NEEDS_RELOAD, true)
                    EditorNotifications.getInstance(myProject).updateNotifications(vfs)
                }
                super.documentChanged(event)
            }
        }, myProject)
    }
}