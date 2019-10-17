package net.zero9178

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.exists
import java.io.File
import java.nio.file.Paths

const val CLI_PATH_KEY = "MBED_CLI_PATH"
const val LAST_TARGET_KEY = "MBED_LAST_TARGET"

class MbedSettingsImpl : MbedSettings() {

    init {
        myCliPath.addBrowseFolderListener(
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
        myCliPath.text = PropertiesComponent.getInstance().getValue(CLI_PATH_KEY) ?: ""
        myCliPath.text = myCliPath.text.ifEmpty {
            System.getenv("PATH").split(File.pathSeparatorChar).firstOrNull {
                Paths.get(it).resolve(if (SystemInfo.isWindows) "mbed.exe" else "mbed").exists()
            }?.plus(File.separatorChar + if (SystemInfo.isWindows) "mbed.exe" else "mbed") ?: ""
        }
    }

    override fun isModified() = myCliPath.text != PropertiesComponent.getInstance().getValue(CLI_PATH_KEY) ?: ""

    override fun getDisplayName() = "mbed"

    override fun apply() {
        PropertiesComponent.getInstance().setValue(CLI_PATH_KEY, myCliPath.text)
    }
}