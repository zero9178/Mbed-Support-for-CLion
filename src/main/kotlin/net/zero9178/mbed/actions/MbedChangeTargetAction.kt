package net.zero9178.mbed.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import net.zero9178.mbed.ModalTask
import net.zero9178.mbed.packages.changeTarget
import net.zero9178.mbed.packages.changeTargetDialog
import java.io.File
import javax.swing.SwingUtilities.invokeAndWait

/**
 * Action showing up either in the 'Tools' menu or when right clicking the project base path.
 * Pops up a dialog that let's one change target
 */
class MbedChangeTargetAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val workspace = CMakeWorkspace.getInstance(project)
        val target = changeTargetDialog(project) ?: return
        ProgressManager.getInstance().run(ModalTask(project, "Changing target", {
            invokeAndWait {
                runWriteAction {
                    workspace.unload(false)
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