package net.zero9178.mbed.project

import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.writeChild
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.CLionProjectGenerator
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import icons.MbedIcons
import net.zero9178.mbed.MbedNotification
import net.zero9178.mbed.ModalTask
import net.zero9178.mbed.packages.changeTarget
import net.zero9178.mbed.packages.changeTargetDialog
import net.zero9178.mbed.state.MbedState
import org.apache.commons.exec.*
import java.io.File
import javax.swing.Icon

/**
 * Instantiated when creating a new project with the mbed-os wizard
 */
class MbedNewProjectCreators : CLionProjectGenerator<Any>() {

    override fun generateProject(project: Project, virtualFile: VirtualFile, settings: Any, module: Module) {
        ProgressManager.getInstance().run(
            ModalTask(
                project,
                "Creating new Mbed os project",
                {
                    val cli = MbedState.getInstance().cliPath
                    val cl = CommandLine.parse("$cli new .")
                    val exec = DefaultExecutor()
                    exec.workingDirectory = File(virtualFile.path)
                    val err = mutableListOf<String>()
                    exec.streamHandler = PumpStreamHandler(object : LogOutputStream() {
                        override fun processLine(line: String?, logLevel: Int) {
                            it.text = line ?: return
                            err += line
                        }
                    })

                    try {
                        exec.execute(cl)
                    } catch (e: ExecuteException) {
                        // As far as I am aware it is impossible to fail a projekt generation according to CLion
                        // Therefore we can't really cancel and must just notify the user of the error instead in the
                        // hope that they will try again later
                        val notification = MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                            "Failed to create mbed Project",
                            "mbed exited with code ${e.exitValue} and stderr ${err.joinToString("\n")}",
                            NotificationType.ERROR, null
                        )
                        Notifications.Bus.notify(notification, project)
                    }
                }) {
                virtualFile.writeChild(
                    "main.cpp",
                    """#include <mbed.h>

int main()
{

}

"""
                )
                if (virtualFile.findChild("mbed_app.json") == null) {
                    virtualFile.writeChild(
                        "mbed_app.json", """{}"""
                    )
                }
                changeTargetDialog(project)?.let { changeTarget(it, project) }
                CMakeWorkspace.getInstance(project).selectProjectDir(project.basePath?.let { File(it) })
            })
    }

    override fun getName(): String = "mbed-os"

    override fun getLogo(): Icon? = MbedIcons.MBED_ICON_16x16

    override fun getGroupName() = "mbed"
}

