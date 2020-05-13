package net.zero9178.mbed.gui

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.content.ContentFactory
import net.zero9178.mbed.editor.MbedAppLibDaemon

class MbedPackagesViewFactory : ToolWindowFactory, DumbAware {

    override fun init(toolWindow: ToolWindow) {
        if (toolWindow !is ToolWindowEx) return
        val project = toolWindow.project
        toolWindow.isAvailable = project.getUserData(MbedAppLibDaemon.PROJECT_IS_MBED_PROJECT) ?: false
        project.messageBus.connect(project)
            .subscribe(MbedAppLibDaemon.MBED_PROJECT_CHANGED, object : MbedAppLibDaemon.MbedAppListener {
                override fun statusChanged(isMbedProject: Boolean) {
                    runInEdt {
                        // Initialize PackageView so that we can figure out which folders are packages
                        // and change their icon
                        if (isMbedProject) {
                            MbedPackagesView.getInstance(project).refreshTree()
                        } else {
                            MbedPackagesView.getInstance(project).clear()
                        }
                        toolWindow.isAvailable = isMbedProject
                    }
                }
            })
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val packageView = MbedPackagesView.getInstance(project)
        val content = ContentFactory.SERVICE.getInstance().createContent(
            packageView.panel,
            "",
            false
        )
        content.preferredFocusableComponent = packageView.panel
        toolWindow.contentManager.addContent(
            content
        )
    }
}