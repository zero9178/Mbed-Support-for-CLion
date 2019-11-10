package net.zero9178.mbed.state

import com.intellij.execution.Platform
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.io.exists
import java.io.File
import java.nio.file.Paths

@State(
    name = "net.zero9178.state.MbedState",
    storages = [Storage("zero978.mbedState.xml", roamingType = RoamingType.DISABLED)]
)
class MbedState : PersistentStateComponent<MbedState.State> {

    data class State(var cliPath: String = "", var lastTarget: String = "", var lastDirectory: String = "")

    private var myState: State = State()

    override fun getState() = myState

    override fun loadState(state: State) {
        myState = state
        ensurePopulated()
    }

    override fun noStateLoaded() {
        ensurePopulated()
    }

    private fun ensurePopulated() {
        myState.cliPath = myState.cliPath.ifBlank {
            val mbedCli = "mbed" + if (Platform.current() == Platform.WINDOWS) ".exe" else ""
            System.getenv("PATH").split(File.pathSeparatorChar).firstOrNull {
                Paths.get(it).resolve(mbedCli).exists()
            }?.let {
                Paths.get(it).resolve(mbedCli).toString()
            } ?: ""
        }
    }

    var cliPath: String
        get() = myState.cliPath
        set(value) {
            myState.cliPath = value
        }

    var lastTarget: String
        get() = myState.lastTarget
        set(value) {
            myState.lastTarget = value
        }

    var lastDirectory: String
        get() = myState.lastDirectory
        set(value) {
            myState.lastDirectory = value
        }

    companion object {
        fun getInstance() = ApplicationManager.getApplication().getComponent(MbedState::class.java)!!
    }
}