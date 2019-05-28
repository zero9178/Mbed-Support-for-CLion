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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.ProjectGeneratorPeer
import icons.MbedIcons
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.kohsuke.github.GitHub
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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
        val zipFile = "${project.basePath + File.separator}..${File.separator + releaseTag}.zip"
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
                        val size = (zipUrl.openConnection() as? HttpURLConnection)?.contentLengthLong ?: 0
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
                                indicator.fraction = bytesWritten.toDouble() / size
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
            }
        }
        ProgressManager.getInstance().run(task)
    }

    override fun getName(): String = "mbed-os"

    override fun createPeer(): ProjectGeneratorPeer<String> = MbedProjectPeerImpl(myProjectSettingsStepBase)

    override fun getLogo(): Icon? = MbedIcons.MBED_ICON_16x16
}