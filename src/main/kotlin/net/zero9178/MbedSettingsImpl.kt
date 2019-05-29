package net.zero9178

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.exists
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import javax.swing.JComponent

val CACHE_DIRECTORY: String = System.getProperty("user.home") + File.separator + ".clionMbedPlugin"
const val CLI_PATH_KEY = "MBED_CLI_PATH"
const val USE_CACHE_KEY = "MBED_USE_CACHE"

class MbedSettingsImpl : MbedSettings() {

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
        m_cliPath.text = PropertiesComponent.getInstance().getValue(CLI_PATH_KEY) ?: ""
        m_cliPath.text = m_cliPath.text.ifEmpty {
            System.getenv("PATH").split(File.pathSeparatorChar).firstOrNull {
                Paths.get(it).resolve(if (SystemInfo.isWindows) "mbed.exe" else "mbed").exists()
            }?.plus(File.separatorChar + if (SystemInfo.isWindows) "mbed.exe" else "mbed") ?: ""
        }
        m_enableChaching.isSelected = PropertiesComponent.getInstance().getBoolean(USE_CACHE_KEY)
        m_clearCache.addActionListener {
            try {
                FileUtils.cleanDirectory(File(CACHE_DIRECTORY))
            } catch (e: IOException) {
                //TODO: log later
            }
        }
    }

    override fun isModified() = m_cliPath.text != PropertiesComponent.getInstance().getValue(CLI_PATH_KEY) ?: ""
            || m_enableChaching.isSelected != PropertiesComponent.getInstance().getBoolean(USE_CACHE_KEY)

    override fun getDisplayName() = "mbed"

    override fun apply() {
        PropertiesComponent.getInstance().setValue(CLI_PATH_KEY, m_cliPath.text)
        PropertiesComponent.getInstance().setValue(USE_CACHE_KEY, m_enableChaching.isSelected)
    }

    override fun createComponent(): JComponent = m_panel
}