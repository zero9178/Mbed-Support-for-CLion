package net.zero9178.mbed.project

import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.writeChild
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.CLionProjectGenerator
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import icons.MbedIcons
import net.zero9178.mbed.MbedNotification
import net.zero9178.mbed.MbedSyncTask
import net.zero9178.mbed.packages.changeTarget
import net.zero9178.mbed.packages.changeTargetDialog
import net.zero9178.mbed.state.MbedState
import java.io.File
import javax.swing.Icon

/**
 * Instantiated when creating a new project with the mbed-os wizard
 */
class MbedNewProjectCreators : CLionProjectGenerator<Any>() {

    override fun generateProject(project: Project, virtualFile: VirtualFile, settings: Any, module: Module) {
        ProgressManager.getInstance().run(
            MbedSyncTask(
                project,
                "Creating new Mbed os project",
                {
                    it.isIndeterminate = true
                    val cli = MbedState.getInstance().cliPath

                    val handle = OSProcessHandler(
                        PtyCommandLine(
                            listOf(
                                cli,
                                "new",
                                "-v",
                                "."
                            )
                        ).withWorkDirectory(virtualFile.path).withCharset(Charsets.UTF_8)
                    )
                    handle.addProcessListener(object : ProcessAdapter() {
                        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                            it.text += event.text
                        }
                    })
                    handle.startNotify()
                    handle.waitFor()
                }) {
                virtualFile.writeChild(
                    "main.cpp",
                    "#include <mbed.h>\n\nint main()\n{\n\n}\n\n"
                )
                if (virtualFile.findChild("mbed_app.json") == null) {
                    virtualFile.writeChild(
                        "mbed_app.json", """{}"""
                    )
                }
                changeTargetDialog(project)?.let { changeTarget(it, project) }
                if (project.name.contains("\\s".toRegex())) {
                    Notifications.Bus.notify(
                        MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                            "Project name contains spaces which might lead to failure when loading the CMake project",
                            NotificationType.WARNING
                        )
                    )
                }
                project.basePath?.let {
                    CMakeWorkspace.getInstance(project).selectProjectDir(File(it))
                }
            })
    }

    override fun getName(): String = "Mbed"

    override fun getLogo(): Icon? = MbedIcons.PLUGIN_ICON_16x16

    override fun getGroupName() = "mbed"
}

