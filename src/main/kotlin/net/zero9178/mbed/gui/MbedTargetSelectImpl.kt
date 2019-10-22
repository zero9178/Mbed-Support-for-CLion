package net.zero9178.mbed.gui

import com.intellij.openapi.project.Project
import net.zero9178.mbed.packages.getLastTarget
import net.zero9178.mbed.state.MbedState
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
        myTargets.model = DefaultComboBoxModel(targets.toTypedArray())
        myTargets.selectedItem = getLastTarget(project) ?: MbedState.getInstance().lastTarget
    }

    var selectedTarget: String
        get() = myTargets.selectedItem as String
        set(value) {
            myTargets.selectedItem = value
        }

    override fun showAndGet(): Boolean {
        val value = super.showAndGet()
        if (value) {
            MbedState.getInstance().lastTarget = selectedTarget
        }
        return value
    }

    override fun getPreferredFocusedComponent(): JComponent = myTargets
}