package net.zero9178

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.ProjectGeneratorPeer
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import icons.MbedIcons
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.kohsuke.github.GitHub
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.swing.Icon

class MbedProjectCreators : DirectoryProjectGeneratorBase<String>(),
    CustomStepProjectGenerator<String> {

    lateinit var myProjectSettingsStepBase: ProjectSettingsStepBase<String>

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<String>?,
        callback: AbstractNewProjectStep.AbstractCallback<String>?
    ): AbstractActionWithPanel {
        myProjectSettingsStepBase =
            ProjectSettingsStepBase(projectGenerator, AbstractNewProjectStep.AbstractCallback<String>())
        return myProjectSettingsStepBase
    }

    private class ModalCanceableTask(
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

    override fun generateProject(project: Project, virtualFile: VirtualFile, releaseTag: String, module: Module) {
        val zipFile = "${virtualFile.path + File.separator}..${File.separator + releaseTag}.zip"
        val downloadTask = ModalCanceableTask(project, "Downloading mbed-os", { indicator ->
            val cachePath = Paths.get(CACHE_DIRECTORY + File.separator + "$releaseTag.zip")
            if (!PropertiesComponent.getInstance().getBoolean(USE_CACHE_KEY) || !Files.exists(cachePath)) {
                try {
                    val repo = GitHub.connectAnonymously().getRepository(ARM_MBED_REPOSITORY)
                    val zipUrl = URL(
                        if (releaseTag.isEmpty()) {
                            repo.latestRelease.zipballUrl
                        } else {
                            repo.getReleaseByTagName(releaseTag).zipballUrl
                        }
                    )
                    val size = getFileSize(zipUrl)
                    if (size <= 0L) {
                        indicator.isIndeterminate = true
                    }
                    object : ReadableByteChannel {

                        private var bytesWritten = 0
                        private val rbc = Channels.newChannel(zipUrl.openStream())

                        override fun isOpen() = rbc.isOpen

                        override fun close() = rbc.close()

                        override fun read(dst: ByteBuffer?): Int {
                            val length = rbc.read(dst)
                            bytesWritten += length
                            if (indicator.isIndeterminate) {
                                indicator.fraction = bytesWritten.toDouble() / size
                            }
                            indicator.checkCanceled()
                            return length
                        }
                    }.use { rbc ->
                        FileOutputStream(zipFile).use { f ->
                            f.channel.transferFrom(rbc, 0, Long.MAX_VALUE)
                        }
                    }
                } catch (e: Exception) {
                    if (e !is ProcessCanceledException) {
                        val notification = MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                            "Failed to download mbed-os from github repository",
                            NotificationType.ERROR
                        )
                        Notifications.Bus.notify(notification, project)
                    }
                    Files.delete(Paths.get(zipFile))
                    return@ModalCanceableTask
                }
            } else {
                Files.copy(cachePath, Paths.get(zipFile), StandardCopyOption.REPLACE_EXISTING)
            }
            if (PropertiesComponent.getInstance().getBoolean(USE_CACHE_KEY)) {
                cachePath.toFile().parentFile.mkdirs()
                Files.copy(Paths.get(zipFile), cachePath, StandardCopyOption.REPLACE_EXISTING)
            }
        }) {
            ProgressManager.getInstance()
                .run(ModalCanceableTask(project, "Unzipping and generating cmake project", unzipper@{ indicator ->
                    val length = File(zipFile).length()
                    val mbedPath = Paths.get(project.basePath).resolve("mbed-os").toString()
                    try {
                        ZipArchiveInputStream(FileInputStream(zipFile)).use {
                            var topDirectoryEncountered = false
                            var topDirectoryName = ""
                            var zipEntry: ZipArchiveEntry? = it.nextZipEntry
                            while (zipEntry != null) {
                                indicator.checkCanceled()
                                val file = File(
                                    mbedPath,
                                    if (topDirectoryEncountered) zipEntry.name.removePrefix(topDirectoryName) else zipEntry.name
                                )
                                if (zipEntry.isDirectory) {
                                    if (topDirectoryEncountered) {
                                        file.mkdirs()
                                    } else {
                                        topDirectoryEncountered = true
                                        topDirectoryName = zipEntry.name
                                    }
                                } else {
                                    val parent = file.parentFile
                                    if (!parent.exists()) {
                                        parent.mkdirs()
                                    }
                                    FileOutputStream(file).use { f ->
                                        it.copyTo(f)
                                    }
                                }
                                indicator.fraction = it.bytesRead.toDouble() / (length + 1)
                                zipEntry = it.nextZipEntry
                            }
                        }
                    } catch (e: ProcessCanceledException) {
                        return@unzipper
                    } finally {
                        Files.delete(Paths.get(zipFile))
                    }
                    var target = ""
                    val targets = queryCompatibleTargets(Paths.get(virtualFile.path).resolve("mbed-os").toString())
                    ApplicationManager.getApplication().invokeAndWait {
                        val dialog = MbedTargetSelectImpl(targets, project)
                        if (dialog.showAndGet()) {
                            target = dialog.selectedTarget
                        }
                    }
                    indicator.text = "Generating cmake files"
                    val cliPath = PropertiesComponent.getInstance().getValue(CLI_PATH_KEY, "")
                    val exitCode = ProcessBuilder().command(
                        cliPath,
                        "export",
                        "-i",
                        "cmake_gcc_arm",
                        "-m",
                        target
                    ).directory(File(mbedPath)).start().waitFor()
                    if (exitCode != 0) {
                        return@unzipper
                    }
                    Files.write(
                        Paths.get(project.basePath).resolve("main.cpp"),
                        listOf("#include <mbed.h>", "", "int main()", "{", "", "}", "")
                    )
                    Files.write(
                        Paths.get(mbedPath).resolve("project.cmake"),
                        listOf("add_subdirectory(.. \${CMAKE_CURRENT_BINARY_DIR}/${project.name})")
                    )
                    Files.write(
                        Paths.get(project.basePath).resolve("CMakeLists.txt"),
                        listOf(
                            "",
                            "cmake_policy(SET CMP0076 NEW)",
                            "",
                            "set(OWN_SOURCES main.cpp)",
                            "target_sources(mbed-os PUBLIC \${OWN_SOURCES})",
                            "set_target_properties(mbed-os PROPERTIES CXX_STANDARD 17)",
                            "set_source_files_properties(\${OWN_SOURCES} PROPERTIES COMPILE_DEFINITIONS MBED_NO_GLOBAL_USING_DIRECTIVE)",
                            "target_compile_options(mbed-os PUBLIC \$<\$<COMPILE_LANGUAGE:CXX>:-Wno-register>)"
                        )
                    )
                    indicator.fraction = 1.0
                })
                {
                    CMakeWorkspace.getInstance(project)
                        .selectProjectDir(VfsUtilCore.virtualToIoFile(virtualFile).toPath().resolve("mbed-os").toFile())
                })
        }
        ProgressManager.getInstance().run(downloadTask)
    }

    override fun getName(): String = "mbed-os"

    override fun createPeer(): ProjectGeneratorPeer<String> = MbedProjectPeerImpl(myProjectSettingsStepBase)

    override fun getLogo(): Icon? = MbedIcons.MBED_ICON_16x16
}

private fun getFileSize(url: URL): Long {
    var con: HttpURLConnection? = null
    return try {
        con = url.openConnection() as HttpURLConnection
        con.requestMethod = "HEAD"
        con.contentLengthLong
    } catch (e: IOException) {
        0
    } finally {
        con?.disconnect()
    }
}