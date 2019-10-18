package net.zero9178.mbed.project

import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.testFramework.writeChild
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import icons.MbedIcons
import net.zero9178.mbed.ModalCanceableTask
import net.zero9178.mbed.packages.changeTargetDialog
import net.zero9178.mbed.state.MbedState
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler
import java.io.File
import javax.swing.Icon

class MbedNewProjectCreators : DirectoryProjectGeneratorBase<Any>() {

    override fun generateProject(project: Project, virtualFile: VirtualFile, settings: Any, module: Module) {
        ProgressManager.getInstance().run(
            ModalCanceableTask(
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
                    exec.execute(cl)
                }) {
                changeTargetDialog(project)
                virtualFile.writeChild(
                    "main.cpp",
                    """#include <mbed.h>

int main()
{

}

"""
                )
                virtualFile.writeChild(
                    "project.cmake",
                    """
set(OWN_SOURCES main.cpp)
target_sources(${project.name} PUBLIC ${"$"}{OWN_SOURCES})
set_source_files_properties(${"$"}{OWN_SOURCES} PROPERTIES COMPILE_DEFINITIONS MBED_NO_GLOBAL_USING_DIRECTIVE)

"""
                )
                CMakeWorkspace.getInstance(project).selectProjectDir(project.basePath?.let { File(it) })
            })
    }

    override fun getName(): String = "mbed-os"

    override fun getLogo(): Icon? = MbedIcons.MBED_ICON_16x16
}

