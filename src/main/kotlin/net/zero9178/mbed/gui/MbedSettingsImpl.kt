package net.zero9178.mbed.gui

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.exists
import net.zero9178.mbed.state.MbedState
import java.io.File
import java.nio.file.Paths

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
        myCliPath.text = MbedState.getInstance().cliPath
        myCliPath.text = myCliPath.text.ifEmpty {
            System.getenv("PATH").split(File.pathSeparatorChar).firstOrNull {
                Paths.get(it).resolve(if (SystemInfo.isWindows) "mbed.exe" else "mbed").exists()
            }?.plus(File.separatorChar + if (SystemInfo.isWindows) "mbed.exe" else "mbed") ?: ""
        }
    }

    override fun isModified() = myCliPath.text != MbedState.getInstance().cliPath

    override fun getDisplayName() = "mbed"

    override fun apply() {
        MbedState.getInstance().cliPath = myCliPath.text
    }
}