package net.zero9178.mbed.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import net.zero9178.mbed.editor.MbedAppLibDaemon
import net.zero9178.mbed.state.MbedProjectState

class MbedSwitchProfile : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        MbedProjectState.getInstance(project).isRelease = !MbedProjectState.getInstance(project).isRelease
        MbedReloadChangesAction.update(project)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project?.getUserData(MbedAppLibDaemon.PROJECT_IS_MBED_PROJECT) != true) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        if (e.presentation.isEnabledAndVisible) {
            e.presentation.text =
                "Switch to ${if (MbedProjectState.getInstance(project).isRelease) "Debug" else "Release"}"
        }
    }
}