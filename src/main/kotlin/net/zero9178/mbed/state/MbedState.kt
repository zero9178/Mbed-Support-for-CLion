package net.zero9178.mbed.state

import com.intellij.execution.Platform
import com.intellij.openapi.components.*
import com.intellij.util.io.exists
import java.io.File
import java.nio.file.Paths

/**
 * Application wide state that holds the CLI, the user's last used target as well as their last used directory
 * when checking out with mbed import from VCS
 */
@State(
    name = "net.zero9178.state.MbedState",
    storages = [Storage("zero978.mbedState.xml", roamingType = RoamingType.DISABLED)]
)
class MbedState : PersistentStateComponent<MbedState.State> {

    data class State(var cliPath: String = "", var lastTarget: String = "")

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

    companion object {
        fun getInstance() = service<MbedState>()
    }
}