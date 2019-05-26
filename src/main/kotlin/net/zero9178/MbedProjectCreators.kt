package net.zero9178

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.ProjectGeneratorPeer
import icons.MbedIcons
import javax.swing.Icon

class MbedProjectCreators : DirectoryProjectGeneratorBase<MbedProjectOptions>(),
    CustomStepProjectGenerator<MbedProjectOptions> {

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<MbedProjectOptions>?,
        callback: AbstractNewProjectStep.AbstractCallback<MbedProjectOptions>?
    ): AbstractActionWithPanel =
        ProjectSettingsStepBase(projectGenerator, AbstractNewProjectStep.AbstractCallback<MbedProjectOptions>())


    override fun generateProject(project: Project, file: VirtualFile, options: MbedProjectOptions, module: Module) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getName(): String {
        return "mbed-os"
    }

    override fun createPeer(): ProjectGeneratorPeer<MbedProjectOptions> {
        return super.createPeer()
    }

    override fun getLogo(): Icon? {
        return MbedIcons.MBED_ICON_16x16
    }
}