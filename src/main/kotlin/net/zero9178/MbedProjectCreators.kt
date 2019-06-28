package net.zero9178

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.ProjectGeneratorPeer
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.cpp.execution.debugger.embedded.OpenOcdConfiguration
import com.jetbrains.cidr.cpp.execution.debugger.embedded.OpenOcdConfigurationType
import com.jetbrains.cidr.execution.BuildTargetAndConfigurationData
import com.jetbrains.cidr.execution.BuildTargetData
import com.jetbrains.cidr.execution.ExecutableData
import icons.MbedIcons
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.Icon

class MbedProjectCreators : DirectoryProjectGeneratorBase<String>(),
    CustomStepProjectGenerator<String> {

    private lateinit var myProjectSettingsStepBase: ProjectSettingsStepBase<String>

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<String>?,
        callback: AbstractNewProjectStep.AbstractCallback<String>?
    ): AbstractActionWithPanel {
        myProjectSettingsStepBase =
            ProjectSettingsStepBase(projectGenerator, AbstractNewProjectStep.AbstractCallback<String>())
        return myProjectSettingsStepBase
    }

    override fun generateProject(project: Project, virtualFile: VirtualFile, releaseTag: String, module: Module) {
        changeMbedVersion(project, virtualFile, releaseTag) {
            Files.write(
                Paths.get(project.basePath).resolve("main.cpp"),
                listOf("#include <mbed.h>", "", "int main()", "{", "", "}", "")
            )
            Files.write(
                Paths.get(project.basePath).resolve("CMakeLists.txt"),
                listOf(
                    "",
                    "set(OWN_SOURCES main.cpp)",
                    "target_sources(mbed-os PUBLIC \${OWN_SOURCES})",
                    "set_source_files_properties(\${OWN_SOURCES} PROPERTIES COMPILE_DEFINITIONS MBED_NO_GLOBAL_USING_DIRECTIVE)",
                    ""
                )
            )
            val cMakeWorkspace = CMakeWorkspace.getInstance(project)
            cMakeWorkspace
                .selectProjectDir(VfsUtilCore.virtualToIoFile(virtualFile).toPath().resolve("mbed-os").toFile())
            ApplicationManager.getApplication().executeOnPooledThread {
                cMakeWorkspace.waitForReloadsToFinish(0)
                val runManager = RunManager.getInstance(project)
                val configuration = runManager
                    .createConfiguration("upload mbed-os", runConfigurationType<OpenOcdConfigurationType>().factory)
                runManager.addConfiguration(configuration)
                val runConf = configuration.configuration as? OpenOcdConfiguration ?: return@executeOnPooledThread
                runConf.boardConfigFile = "board/htlhl_mdds_uC_103rb.cfg"
                val mbedOsBuildTarget = BuildTargetData(cMakeWorkspace.modelTargets.find {
                    it.name == "mbed-os"
                } ?: return@executeOnPooledThread)
                runConf.targetAndConfigurationData =
                    BuildTargetAndConfigurationData(mbedOsBuildTarget, cMakeWorkspace.profileInfos.first().profile.name)
                runConf.executableData = ExecutableData(mbedOsBuildTarget)
            }
        }
    }

    override fun getName(): String = "mbed-os"

    override fun createPeer(): ProjectGeneratorPeer<String> = MbedProjectPeerImpl(myProjectSettingsStepBase)

    override fun getLogo(): Icon? = MbedIcons.MBED_ICON_16x16
}

