package net.zero9178

import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.ui.ValidationInfo
import java.io.IOException
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

class MbedProjectPeerImpl constructor(private val myProjectSettingsStepBase: ProjectSettingsStepBase<String>) :
    MbedProjectPeer() {

    init {
        m_versionSelection.addItemListener {
            myProjectSettingsStepBase.checkValid()
        }
        setLoading(true)
        m_versionSelection.isEnabled = false
        getMbedOSReleaseVersionsAsync().thenAccept { (list, fromCache) ->
            ApplicationManager.getApplication().invokeLater({
                setLoading(false)
                m_versionSelection.model = DefaultComboBoxModel(list.toTypedArray())
                m_versionSelection.isEnabled = true
                if (fromCache) {
                    m_errorLabel.icon = AllIcons.General.Warning
                    m_errorLabel.text =
                        "Failed to retrieve releases from online. Displaying releases in cache that are available offline"
                }
                myProjectSettingsStepBase.checkValid()
            }, ModalityState.stateForComponent(component))
        }
    }

    private var myCliValidated = false

    override fun validate(): ValidationInfo? = if (!m_versionSelection.isEnabled) {
        if (m_versionSelection.model.size == 0) {
            ValidationInfo("Failed to retrieve github releases")
        } else {
            ValidationInfo("No mbed-os version selected")
        }
    } else if (!myCliValidated) {
        val cliPath = PropertiesComponent.getInstance().getValue(CLI_PATH_KEY, "")
        try {
            val ret = ProcessBuilder().command(cliPath, "--version").start().waitFor()
            if (ret == 0) {
                myCliValidated = true
                null
            } else {
                ValidationInfo("mbed-cli exited with error code $ret")
            }
        } catch (e: IOException) {
            ValidationInfo("mbed-cli is not set")
        }
    } else {
        null
    }

    override fun getSettings(): String = m_versionSelection.selectedItem as? String ?: ""

    /** Deprecated in 2017.3 But We must override it. */
    @Deprecated("", ReplaceWith("addSettingsListener"), level = DeprecationLevel.ERROR)
    override fun addSettingsStateListener(@Suppress("DEPRECATION") listener: com.intellij.platform.WebProjectGenerator.SettingsStateListener) =
        Unit

    override fun buildUI(settingsStep: SettingsStep) = settingsStep.addExpertPanel(component)

    override fun isBackgroundJobRunning(): Boolean = true

    override fun getComponent(): JComponent = panel
}