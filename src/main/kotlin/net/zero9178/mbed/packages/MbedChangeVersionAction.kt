package net.zero9178.mbed.packages

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class MbedChangeVersionAction : AnAction() {

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun actionPerformed(event: AnActionEvent) {

    }

    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = virtualFile != null && virtualFile.name == "mbed-os"
    }
}