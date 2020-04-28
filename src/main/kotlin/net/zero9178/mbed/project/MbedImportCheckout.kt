package net.zero9178.mbed.project

import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.util.io.exists
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import git4idea.GitVcs
import net.zero9178.mbed.MbedNotification
import net.zero9178.mbed.ModalTask
import net.zero9178.mbed.packages.changeTarget
import net.zero9178.mbed.packages.changeTargetDialog
import net.zero9178.mbed.state.MbedState
import org.zmlx.hg4idea.HgVcs
import java.io.File
import java.nio.file.Paths

/**
 * Class implementing the "Get from Version Control" functionality for mbed import
 */
class MbedImportCheckout : CheckoutProvider {

    override fun doCheckout(project: Project, listener: CheckoutProvider.Listener?) {
        val dialog = MbedImportCheckoutDialogImpl(project)
        if (!dialog.showAndGet()) {
            return
        }
        MbedState.getInstance().lastDirectory = Paths.get(dialog.getDirectory()).parent.toString()
        ProgressManager.getInstance().run(
            ModalTask(
                project,
                "Importing mbed project",
                {
                    val cli = MbedState.getInstance().cliPath
                    val handler = OSProcessHandler(
                        PtyCommandLine(
                            listOf(
                                cli,
                                "import",
                                dialog.getImportTarget(),
                                dialog.getDirectory()
                            )
                        )
                    )
                    var output = ""
                    handler.addProcessListener(object : ProcessAdapter() {
                        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                            it.text += event.text
                            output += event.text
                        }
                    })
                    handler.startNotify()
                    handler.waitFor()
                    if (handler.exitCode != 0) {
                        Notifications.Bus.notify(
                            MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                                "mbed failed with exit code ${handler.exitCode}\n Output: $output",
                                NotificationType.ERROR
                            )
                        )
                    }
                })
        )
        if (Paths.get(dialog.getDirectory()).resolve(".git").exists()) {
            listener?.directoryCheckedOut(File(dialog.getDirectory()), GitVcs.getKey())
        } else {
            listener?.directoryCheckedOut(File(dialog.getDirectory()), HgVcs.getKey())
        }
        listener?.checkoutCompleted()
        val newProject = ProjectManager.getInstance().openProjects.find {
            it.basePath == dialog.getDirectory().replace('\\', '/')
        } //finds just created project. We can't use the project variable passed from the function as its just a
        // temporary empty project for the checkout process
        if (newProject != null) {
            changeTargetDialog(newProject)?.let { changeTarget(it, newProject) }
            CMakeWorkspace.getInstance(newProject).selectProjectDir(newProject.basePath?.let { File(it) })
        }
    }

    override fun getVcsName(): String = "mbed import"
}