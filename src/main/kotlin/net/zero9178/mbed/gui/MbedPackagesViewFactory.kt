package net.zero9178.mbed.gui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class MbedPackagesViewFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val packageView = MbedPackagesView.getInstance(project)
        toolWindow.contentManager.addContent(
            ContentFactory.SERVICE.getInstance().createContent(
                packageView.panel,
                "",
                false
            )
        )
    }
}