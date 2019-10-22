package net.zero9178.mbed.packages

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project

class MbedJsonWatcher(project: Project) {
    init {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val document = event.document
                val vfs = FileDocumentManager.getInstance().getFile(document) ?: return
                if (vfs.name == "mbed_app.json" || vfs.name == "mbed_lib.json") {

                }
            }
        })
    }
}