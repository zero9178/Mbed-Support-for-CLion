package net.zero9178.mbed.project

import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.util.io.exists
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import git4idea.GitVcs
import net.zero9178.mbed.MbedNotification
import net.zero9178.mbed.ModalTask
import net.zero9178.mbed.packages.changeTarget
import net.zero9178.mbed.packages.changeTargetDialog
import net.zero9178.mbed.state.MbedState
import org.apache.commons.exec.*
import org.zmlx.hg4idea.HgVcs
import java.io.File
import java.nio.file.Paths

class MbedImportCheckout : CheckoutProvider {

    override fun doCheckout(project: Project, listener: CheckoutProvider.Listener?) {
        val dialog = MbedImportCheckoutDialogImpl(project)
        if (dialog.showAndGet()) {
            MbedState.getInstance().lastDirectory = Paths.get(dialog.getDirectory()).parent.toString()
            ProgressManager.getInstance().run(
                ModalTask(
                    project,
                    "Importing mbed project",
                    {
                        val cli = MbedState.getInstance().cliPath
                        val cl = CommandLine.parse("$cli import ${dialog.getImportTarget()} ${dialog.getDirectory()}")
                        val exec = DefaultExecutor()
                        val output = mutableListOf<String>()
                        exec.streamHandler = PumpStreamHandler(object : LogOutputStream() {
                            override fun processLine(line: String?, logLevel: Int) {
                                it.text = line ?: return
                                output += line
                            }
                        })
                        try {
                            exec.execute(cl)
                        } catch (e: ExecuteException) {
                            Notifications.Bus.notify(
                                MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                                    "mbed failed with exit code ${e.exitValue}\n Output: ${output.joinToString("\n")}",
                                    NotificationType.ERROR
                                )
                            )
                        }
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
                    it.basePath == dialog.getDirectory().replace('\\', '/')
                }
                if (newProject != null) {
                    changeTargetDialog(newProject)?.let { changeTarget(it, newProject) }
                    CMakeWorkspace.getInstance(newProject).selectProjectDir(newProject.basePath?.let { File(it) })
                }
            }
        }
    }

    override fun getVcsName(): String = "mbed import"
}