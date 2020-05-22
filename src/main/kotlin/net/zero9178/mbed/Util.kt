package net.zero9178.mbed

import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

class MbedAsyncTask(
    project: Project,
    title: String,
    val runner: (ProgressIndicator) -> Unit,
    val successRunner: () -> Unit = {}
) : Task.Backgroundable(project, title, false, PerformInBackgroundOption.DEAF) {
    override fun run(indicator: ProgressIndicator) {
        runner(indicator)
    }

    override fun onSuccess() {
        successRunner()
    }
}

class MbedSyncTask(
    project: Project,
    title: String,
    val runner: (ProgressIndicator) -> Unit,
    val successRunner: () -> Unit = {}
) : Task.Modal(project, title, false) {
    override fun run(indicator: ProgressIndicator) {
        runner(indicator)
    }

    override fun onSuccess() {
        successRunner()
    }
}