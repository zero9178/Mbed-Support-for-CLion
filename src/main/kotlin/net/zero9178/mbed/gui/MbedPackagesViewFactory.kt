package net.zero9178.mbed.gui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.util.io.exists
import java.nio.file.Paths

class MbedPackagesViewFactory : ToolWindowFactory, DumbAware, Condition<Project> {
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

    override fun value(project: Project?): Boolean {
        project ?: return false
        val basePath = project.basePath ?: return false
        return Paths.get(basePath).resolve("mbed_app.json").exists()
    }
}