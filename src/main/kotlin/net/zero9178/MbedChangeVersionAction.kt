package net.zero9178

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import java.io.File
import java.io.IOException

class MbedChangeVersionAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val vf = LocalFileSystem.getInstance().findFileByIoFile(File(project.basePath)) ?: return
        val dialog = MbedVersionSelectImpl(project)
        getMbedOSReleaseVersionsAsync().thenAccept { (list, _) ->
            ApplicationManager.getApplication().invokeLater {
                dialog.setVersions(list)
            }
        }
        if (dialog.showAndGet()) {
            val folder = vf.findChild("mbed-os") ?: return
            if (folder.isDirectory) {
                try {
                    folder.delete(this)
                } catch (e: IOException) {
                }
            }
            changeMbedVersion(project, vf, dialog.selectedVersion) {
                CMakeWorkspace.getInstance(project)
                        .selectProjectDir(VfsUtilCore.virtualToIoFile(vf).toPath().resolve("mbed-os").toFile())
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = virtualFile != null && virtualFile.name == "mbed-os"
    }
}