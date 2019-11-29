package net.zero9178.mbed.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.util.io.exists
import net.zero9178.mbed.state.MbedProjectState
import java.nio.file.Paths

class MbedSwitchProfile : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        MbedProjectState.getInstance(project).isRelease = !MbedProjectState.getInstance(project).isRelease
        MbedReloadChangesAction.update(project)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible =
            project?.let { it.basePath?.let { Paths.get(it).resolve("mbed_app.json").exists() } } ?: false
        if (e.presentation.isEnabledAndVisible && project != null) {
            e.presentation.text =
                "Switch to ${if (MbedProjectState.getInstance(project).isRelease) "Debug" else "Release"}"
        }
    }
}