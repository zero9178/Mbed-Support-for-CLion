package net.zero9178

import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ValidationInfo
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

class MbedProjectPeerImpl constructor(private val myProjectSettingsStepBase: ProjectSettingsStepBase<String>) : MbedProjectPeer() {

    init {
        m_versionSelection.addItemListener {
            myProjectSettingsStepBase.checkValid()
        }
        setLoading(true)
        m_versionSelection.isEnabled = false
        getMbedOSReleaseVersionsAsync().thenApply {
            ApplicationManager.getApplication().invokeLater {
                m_versionSelection.model = DefaultComboBoxModel(it.toTypedArray())
                setLoading(false)
                m_versionSelection.isEnabled = true
                myProjectSettingsStepBase.checkValid()
            }
        }
    }

    override fun validate(): ValidationInfo? = if (!m_versionSelection.isEnabled) {
        ValidationInfo("No mbed-os version selected")
    } else {
        null
    }

    override fun getSettings(): String = m_versionSelection.selectedItem as? String ?: ""

    /** Deprecated in 2017.3 But We must override it. */
    @Deprecated("", ReplaceWith("addSettingsListener"), level = DeprecationLevel.ERROR)
    override fun addSettingsStateListener(@Suppress("DEPRECATION") listener: com.intellij.platform.WebProjectGenerator.SettingsStateListener) =
        Unit

    override fun buildUI(settingsStep: SettingsStep) = settingsStep.addExpertPanel(component)

    override fun isBackgroundJobRunning(): Boolean = false

    override fun getComponent(): JComponent = panel
}