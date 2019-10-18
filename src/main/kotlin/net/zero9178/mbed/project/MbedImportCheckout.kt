package net.zero9178.mbed.project

import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.util.io.exists
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import git4idea.GitVcs
import net.zero9178.mbed.ModalCanceableTask
import net.zero9178.mbed.packages.changeTargetDialog
import net.zero9178.mbed.state.MbedState
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler
import org.zmlx.hg4idea.HgVcs
import java.io.File
import java.nio.file.Paths

class MbedImportCheckout : CheckoutProvider {

    override fun doCheckout(project: Project, listener: CheckoutProvider.Listener?) {
        val dialog = MbedImportCheckoutDialogImpl(project)
        if (dialog.showAndGet()) {
            ProgressManager.getInstance().run(
                ModalCanceableTask(
                    project,
                    "Importing mbed project",
                    {
                        val cli = MbedState.getInstance().cliPath
                        val cl = CommandLine.parse("$cli import ${dialog.getImportTarget()} ${dialog.getDirectory()}")
                        val exec = DefaultExecutor()
                        exec.streamHandler = PumpStreamHandler(object : LogOutputStream() {
                            override fun processLine(line: String?, logLevel: Int) {
                                it.text = line ?: return
                            }
                        })
                        exec.execute(cl)
                    })
            )
            TransactionGuard.getInstance().submitTransactionAndWait {
                if (Paths.get(dialog.getDirectory()).resolve(".git").exists()) {
                    listener?.directoryCheckedOut(File(dialog.getDirectory()), GitVcs.getKey())
                } else {
                    listener?.directoryCheckedOut(File(dialog.getDirectory()), HgVcs.getKey())
                }
                listener?.checkoutCompleted()
                val newProject = ProjectManager.getInstance().openProjects.find {
                    it.basePath == dialog.getDirectory()
                }
                if (newProject != null) {
                    changeTargetDialog(newProject)
                    CMakeWorkspace.getInstance(newProject).selectProjectDir(newProject.basePath?.let { File(it) })
                }
            }
        }
    }

    override fun getVcsName(): String = "mbed import"
}