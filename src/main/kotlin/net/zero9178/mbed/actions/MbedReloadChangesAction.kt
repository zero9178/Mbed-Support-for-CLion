package net.zero9178.mbed.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotifications
import net.zero9178.mbed.ModalTask
import net.zero9178.mbed.packages.exportToCmake

class MbedReloadChangesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        update(project)
    }

    companion object {
        fun update(project: Project) {
            FileDocumentManager.getInstance().saveAllDocuments()
            ProgressManager.getInstance().run(ModalTask(project, "Generating cmake", {
                exportToCmake(project)
            }) {
                EditorNotifications.getInstance(project).updateAllNotifications()
            })
        }
    }
}