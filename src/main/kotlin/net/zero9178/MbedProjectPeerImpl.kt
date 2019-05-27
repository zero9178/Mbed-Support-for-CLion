package net.zero9178

import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.util.PathUtilRt
import com.intellij.util.containers.computeIfAny
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths.get
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

class MbedProjectPeerImpl : MbedProjectPeer() {

    private val mySettings = MbedProjectOptions.instance()

    init {
        m_cliPath.addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptor(
                    true,
                    false,
                    false,
                    false,
                    false,
                    false
                )
            )
        )
        val exeName = when (PathUtilRt.Platform.CURRENT) {
            PathUtilRt.Platform.WINDOWS -> "mbed.exe"
            else -> "mbed"
        }
        m_cliPath.text = mySettings.cliPath.ifEmpty {
            System.getenv("PATH").split(File.pathSeparator).map {
                get(it)
            }.computeIfAny {
                if (Files.exists(it.resolve(exeName))) "$it/$exeName" else null
            } ?: ""
        }
        setLoading(true)
        m_versionSelection.isEnabled = false
        getMbedOSReleaseVersionsAsync().thenApply {
            ApplicationManager.getApplication().invokeLater {
                m_versionSelection.model = DefaultComboBoxModel(it.toTypedArray())
                setLoading(false)
                m_versionSelection.isEnabled = true
            }
        }
    }

    override fun validate(): ValidationInfo? {
        try {
            if (ProcessBuilder().command(m_cliPath.text, "--version").start().waitFor() != 0) {
                return ValidationInfo("\"${m_cliPath.text}\" could not be queried for the version")
            }
        } catch (e: Exception) {
            return ValidationInfo("\"${m_cliPath.text}\" could not be queried for the version")
        }
        if (!m_versionSelection.isEnabled) {
            return ValidationInfo("No mbed-os version selected")
        }

        mySettings.cliPath = m_cliPath.text
        mySettings.version = m_versionSelection.selectedItem as? String ?: ""

        return null
    }

    override fun getSettings(): MbedProjectOptions = mySettings

    /** Deprecated in 2017.3 But We must override it. */
    @Deprecated("", ReplaceWith("addSettingsListener"), level = DeprecationLevel.ERROR)
    override fun addSettingsStateListener(@Suppress("DEPRECATION") listener: com.intellij.platform.WebProjectGenerator.SettingsStateListener) =
        Unit

    override fun buildUI(settingsStep: SettingsStep) = settingsStep.addExpertPanel(component)

    override fun isBackgroundJobRunning(): Boolean = false

    override fun getComponent(): JComponent = panel
}