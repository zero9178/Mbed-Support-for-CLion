package net.zero9178.mbed.gui

import com.intellij.openapi.project.Project
import net.zero9178.mbed.packages.getLastTarget
import net.zero9178.mbed.state.MbedState
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

/**
 * Dialog for selecting a target
 *
 * @param targets List of targets for the user to choose from
 * @param project Project for window modality
 * @param canBeParent If dialog can have sub windows
 * @param ideModalityType modality type
 */
class MbedTargetSelectImpl(
    targets: List<String>,
    project: Project,
    canBeParent: Boolean = false,
    ideModalityType: IdeModalityType = IdeModalityType.PROJECT
) : MbedTargetSelect(project, canBeParent, ideModalityType) {
    init {
        title = "Select initial target"
        myTargets.model = DefaultComboBoxModel(targets.sorted().toTypedArray())
        val lastTarget = getLastTarget(project)
        myTargets.selectedItem = lastTarget ?: MbedState.getInstance().lastTarget
        if (lastTarget == null) {
            myCurrentTarget.isVisible = false
        } else {
            myCurrentTarget.text += lastTarget
        }
    }

    /**
     * Name of target that was selected from the constructor supplied list
     */
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