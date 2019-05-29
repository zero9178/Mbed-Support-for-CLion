package net.zero9178

import com.intellij.openapi.project.Project
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

class MbedTargetSelectImpl(
    targets: List<String>,
    project: Project,
    canBeParent: Boolean = false,
    ideModalityType: IdeModalityType = IdeModalityType.PROJECT
) : MbedTargetSelect(project, canBeParent, ideModalityType) {
    init {
        title = "Select target"
        m_targets.model = DefaultComboBoxModel(targets.toTypedArray())
    }

    val selectedTarget: String
        get() = m_targets.selectedItem as String

    override fun getPreferredFocusedComponent(): JComponent = m_targets
}