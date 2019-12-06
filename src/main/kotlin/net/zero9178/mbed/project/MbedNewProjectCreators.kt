package net.zero9178.mbed.project

import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.writeChild
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.CLionProjectGenerator
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import icons.MbedIcons
import net.zero9178.mbed.ModalTask
import net.zero9178.mbed.packages.changeTarget
import net.zero9178.mbed.packages.changeTargetDialog
import net.zero9178.mbed.state.MbedState
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler
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
                    exec.streamHandler = PumpStreamHandler(object : LogOutputStream() {
                        override fun processLine(line: String?, logLevel: Int) {
                            it.text = line ?: return
                        }
                    })
                    //TODO: Error handling. execute throws upon non 0 exit code
                    exec.execute(cl)
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

