package net.zero9178.mbed.state

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "net.zero9178.mbed.state.MbedProjectState")
class MbedProjectState : PersistentStateComponent<MbedProjectState>, Disposable {
    override fun getState() = this

    override fun loadState(state: MbedProjectState) = XmlSerializerUtil.copyBean(state, this)

    var isRelease: Boolean = false

    companion object {
        fun getInstance(project: Project) = project.service<MbedProjectState>()
    }

    override fun dispose() {}
}