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
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.ProjectGeneratorPeer
import com.jetbrains.cidr.cpp.cmake.projectWizard.CLionProjectWizardUtils
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

    override fun generateProject(project: Project, virtualFile: VirtualFile, releaseTag: String, module: Module) {
        val zipFile = "${virtualFile.path + File.separator}..${File.separator + releaseTag}.zip"
        val task = object : Task.Modal(project, "Downloading mbed-os...", true) {
            override fun run(indicator: ProgressIndicator) {
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
                        return
                    }
                } else {
                    Files.copy(cachePath, Paths.get(zipFile), StandardCopyOption.REPLACE_EXISTING)
                }
                if (PropertiesComponent.getInstance().getBoolean(USE_CACHE_KEY)) {
                    cachePath.toFile().parentFile.mkdirs()
                    Files.copy(Paths.get(zipFile), cachePath, StandardCopyOption.REPLACE_EXISTING)
                }
            }

            override fun onSuccess() {
                JBPopupFactory.getInstance().createMessage("Unzipping mbed-os, UI might freeze")
                ApplicationManager.getApplication().runWriteAction {
                    ZipArchiveInputStream(FileInputStream(zipFile)).use {
                        var topDirectoryEncountered = false
                        var topDirectoryName = ""
                        var zipEntry: ZipArchiveEntry? = it.nextZipEntry
                        while (zipEntry != null) {
                            val file = File(
                                project.basePath,
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
                            zipEntry = it.nextZipEntry
                        }
                    }
                    Files.delete(Paths.get(zipFile))
                }
                var target = ""
                ApplicationManager.getApplication().runReadAction {
                    val targets = queryCompatibleTargets(virtualFile.path)
                    val dialog = MbedTargetSelectImpl(targets, project)
                    if (dialog.showAndGet()) {
                        target = dialog.selectedTarget
                    }
                }
                ApplicationManager.getApplication().runWriteAction {
                    val cliPath = PropertiesComponent.getInstance().getValue(CLI_PATH_KEY, "")
                    val exitCode = ProcessBuilder().command(
                        cliPath,
                        "export",
                        "-i",
                        "cmake_gcc_arm",
                        "-m",
                        target
                    ).directory(File(project.basePath)).start().waitFor()
                    if (exitCode != 0) {
                        return@runWriteAction
                    }
                    Files.write(
                        Paths.get(project.basePath).resolve("main.cpp"),
                        listOf("#include <mbed.h>", "", "int main()", "{", "", "}", "")
                    )
                    Files.write(
                        Paths.get(project.basePath).resolve("project.cmake"),
                        listOf(
                            "",
                            "cmake_policy(SET CMP0076 NEW)",
                            "",
                            "set(OWN_SOURCES main.cpp)",
                            "target_sources(${project.name} PUBLIC \${OWN_SOURCES})",
                            "set_target_properties(${project.name} PROPERTIES CXX_STANDARD 17 ENABLE_EXPORTS 0)",
                            "set_source_files_properties(\${OWN_SOURCES} PROPERTIES COMPILE_DEFINITIONS MBED_NO_GLOBAL_USING_DIRECTIVE)",
                            "target_compile_options(${project.name} PUBLIC \$<\$<COMPILE_LANGUAGE:CXX>:-Wno-register>)"
                        )
                    )
                }
                val file = LocalFileSystem.getInstance().findFileByPath(
                    project.basePath?.plus(File.separatorChar + "CMakeLists.txt") ?: ""
                ) ?: return
                CLionProjectWizardUtils.reformatProjectFiles(project, file, false)
            }
        }
        ProgressManager.getInstance().run(task)
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