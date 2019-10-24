package net.zero9178.mbed.project

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.action.RefreshExternalProjectAction
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.openapi.project.Project

class MbedAppLibRefresh : RefreshExternalProjectAction() {

    init {

    }

    override fun perform(
        project: Project,
        projectSystemId: ProjectSystemId,
        externalEntityData: AbstractExternalEntityData,
        e: AnActionEvent
    ) {
        super.perform(project, projectSystemId, externalEntityData, e)
    }

    override fun isEnabled(e: AnActionEvent): Boolean {
        return super.isEnabled(e)
    }

    override fun isVisible(e: AnActionEvent): Boolean {
        return super.isVisible(e)
    }
}