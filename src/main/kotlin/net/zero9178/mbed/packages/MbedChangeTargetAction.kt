package net.zero9178.mbed.packages

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import net.zero9178.mbed.ModalTask
import java.io.File

class MbedChangeTargetAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val workspace = CMakeWorkspace.getInstance(project)
        val target = changeTargetDialog(project) ?: return
        ProgressManager.getInstance().run(ModalTask(project, "Changing target", {
            ApplicationManager.getApplication().invokeAndWait {
                ApplicationManager.getApplication().runWriteAction {
                    workspace.shutdown()
                    changeTarget(target, project)
                    workspace.selectProjectDir(project.basePath?.let { File(it) })
                }
            }
        }))
    }

    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = virtualFile?.path == e.project?.basePath
    }
}