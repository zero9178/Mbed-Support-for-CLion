package net.zero9178

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.LocalFileSystem
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import java.io.File

class MbedChangeVersionAction : AnAction() {

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val vf = LocalFileSystem.getInstance().findFileByIoFile(File(project.basePath)) ?: return
        val dialog = MbedVersionSelectImpl(project)
//        getMbedOSReleaseVersionsAsync().thenAccept { (list, _) ->
//            ApplicationManager.getApplication().invokeLater({
//                dialog.setVersions(list)
//            }, ModalityState.stateForComponent(dialog.rootPane))
//        }
        if (dialog.showAndGet()) {
            CMakeWorkspace.getInstance(project).shutdown()
//            val deleteTask = ModalCanceableTask(project, "Deleting old mbed-os", { indicator ->
//                indicator.isIndeterminate = true
//                Paths.get(project.basePath).resolve("mbed-os").toFile().walkBottomUp().fold(true, { res, it ->
//                    indicator.checkCanceled()
//                    (it.delete() || !it.exists()) && res
//                })
//            })
//            {
//                changeMbedVersion(project, vf) {
//                    CMakeWorkspace.getInstance(project)
//                        .selectProjectDir(VfsUtilCore.virtualToIoFile(vf).toPath().resolve("mbed-os").toFile())
//                }
//            }
//            ProgressManager.getInstance().run(deleteTask)
        }
    }

    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = virtualFile != null && virtualFile.name == "mbed-os"
    }
}