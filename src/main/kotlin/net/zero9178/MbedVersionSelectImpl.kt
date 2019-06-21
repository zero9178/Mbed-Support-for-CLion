package net.zero9178

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

class MbedVersionSelectImpl(
    project: Project,
    canBeParent: Boolean = false,
    ideModalityType: IdeModalityType = IdeModalityType.PROJECT
) : MbedVersionSelect(project, canBeParent, ideModalityType) {

    init {
        setLoading(true)

    }

    fun setVersions(versions: List<String>) {
        m_version.model = DefaultComboBoxModel(versions.toTypedArray())
        setLoading(versions.isEmpty())
    }

    var selectedVersion: String
        get() = m_version.selectedItem as String
        set(value) {
            m_version.selectedItem = value
        }

    override fun getPreferredFocusedComponent(): JComponent = m_version

    override fun doValidate(): ValidationInfo? {
        return if (m_version.selectedIndex == -1) {
            ValidationInfo("Version needs to be selected")
        } else {
            null
        }
    }
}