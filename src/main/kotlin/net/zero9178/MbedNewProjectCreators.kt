package net.zero9178

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectGeneratorBase
import icons.MbedIcons
import java.io.File
import javax.swing.Icon

class MbedNewProjectCreators : DirectoryProjectGeneratorBase<Any>() {

    override fun generateProject(project: Project, virtualFile: VirtualFile, settings: Any, module: Module) {
        ProgressManager.getInstance().run(ModalCanceableTask(project, "Creating new Mbed os project", {
            val cli = PropertiesComponent.getInstance().getValue(
                CLI_PATH_KEY
            )
            if (cli == null) {
                Notifications.Bus.notify(
                    MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                        "Invalid mbed cli specified in settings",
                        NotificationType.ERROR
                    ), project
                )
                return@ModalCanceableTask
            }
            val p = ProcessBuilder().directory(File(virtualFile.path)).command(
                cli, "new", "."
            ).start()
            p.waitFor()
        }) {
            changeTargetDialog(project)
        })
//        changeMbedVersion(project, virtualFile) {
//            virtualFile.writeChild(
//                "main.cpp",
//                """#include <mbed.h>
//
//int main()
//{
//
//}
//
//"""
//            )
//            virtualFile.writeChild(
//                "CMakeLists.txt",
//                """
//set(OWN_SOURCES main.cpp)
//target_sources(mbed-os PUBLIC ${"$"}{OWN_SOURCES})
//set_source_files_properties(${"$"}{OWN_SOURCES} PROPERTIES COMPILE_DEFINITIONS MBED_NO_GLOBAL_USING_DIRECTIVE)
//
//"""
//            )
//            val cMakeWorkspace = CMakeWorkspace.getInstance(project)
//            cMakeWorkspace
//                .selectProjectDir(VfsUtilCore.virtualToIoFile(virtualFile).toPath().resolve("mbed-os").toFile())
//            ApplicationManager.getApplication().executeOnPooledThread {
//                cMakeWorkspace.waitForReloadsToFinish(0)
//                val runManager = RunManager.getInstance(project)
//                val configuration = runManager
//                    .createConfiguration("upload mbed-os", runConfigurationType<OpenOcdConfigurationType>().factory)
//                runManager.addConfiguration(configuration)
//                val runConf = configuration.configuration as? OpenOcdConfiguration ?: return@executeOnPooledThread
//                runConf.boardConfigFile = "board/htlhl_mdds_uC_103rb.cfg"
//                val mbedOsBuildTarget = BuildTargetData(cMakeWorkspace.modelTargets.find {
//                    it.name == "mbed-os"
//                } ?: return@executeOnPooledThread)
//                runConf.targetAndConfigurationData =
//                    BuildTargetAndConfigurationData(mbedOsBuildTarget, cMakeWorkspace.profileInfos.first().profile.name)
//                runConf.executableData = ExecutableData(mbedOsBuildTarget)
//            }
//        }
    }

    override fun getName(): String = "mbed-os"

    override fun getLogo(): Icon? = MbedIcons.MBED_ICON_16x16
}

