package net.zero9178.mbed.project

import com.intellij.dvcs.DvcsRememberedInputs
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(name = "MbedRemembereedInputs", storages = [Storage("vcs.xml")])
class MbedRememberedInputs : DvcsRememberedInputs(), PersistentStateComponent<DvcsRememberedInputs.State> {
    companion object {
        fun getInstance() = service<MbedRememberedInputs>()
    }
}