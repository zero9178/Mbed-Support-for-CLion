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
        title = "Select initial target"
        m_targets.model = DefaultComboBoxModel(targets.toTypedArray())
    }

    var selectedTarget: String
        get() = m_targets.selectedItem as String
        set(value) {
            m_targets.selectedItem = value
        }

    override fun getPreferredFocusedComponent(): JComponent = m_targets
}