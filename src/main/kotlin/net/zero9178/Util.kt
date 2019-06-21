package net.zero9178

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

class ModalCanceableTask(
    project: Project,
    title: String,
    val runner: (ProgressIndicator) -> Unit,
    val successRunner: () -> Unit = {}
) : Task.Modal(project, title, true) {
    override fun run(indicator: ProgressIndicator) {
        runner(indicator)
    }

    override fun onSuccess() {
        successRunner()
    }
}