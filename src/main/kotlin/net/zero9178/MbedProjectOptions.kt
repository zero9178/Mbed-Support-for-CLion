package net.zero9178

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "MbedProjectOptions", storages = [Storage("mbedWizard.xml",roamingType = RoamingType.DISABLED)])
data class MbedProjectOptions(var cliPath: String = "",var version: String = "",var target: String = "") : PersistentStateComponent<MbedProjectOptions> {
    override fun getState(): MbedProjectOptions = this

    override fun loadState(mbedProjectOptions: MbedProjectOptions) = XmlSerializerUtil.copyBean(mbedProjectOptions,this)

    companion object {
        fun instance(): MbedProjectOptions = ServiceManager.getService(MbedProjectOptions::class.java)
    }
}