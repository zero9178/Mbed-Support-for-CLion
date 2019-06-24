package net.zero9178

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
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
import java.util.concurrent.CompletableFuture

const val ARM_MBED_REPOSITORY = "ARMmbed/mbed-os"

fun getMbedOSReleaseVersionsAsync(): CompletableFuture<Pair<List<String>, Boolean>> = CompletableFuture.supplyAsync {
    val tagsPath = Paths.get(CACHE_DIRECTORY).resolve(".gittags")
    var result = if (Files.exists(tagsPath)) {
        Files.readAllLines(tagsPath).map { it as String }
    } else {
        listOf()
    }
    val prevSize = result.size
    result = GitHub.connectAnonymously().getRepository(ARM_MBED_REPOSITORY).listReleases().asSequence().filter {
        it.name.contains(".*mbed.os.*".toRegex())
    }.takeWhile {
        it.name > result.firstOrNull() ?: ""
    }.map {
        it.name
    }.toList() + result
    if (result.size != prevSize) {
        tagsPath.toFile().parentFile.mkdirs()
        Files.write(tagsPath, result)
    }
    result to false
}!!.exceptionally { throwable ->
    if (throwable !is IOException) {
        if (PropertiesComponent.getInstance().getBoolean(USE_CACHE_KEY)) {
            File(CACHE_DIRECTORY).listFiles().map {
                it.name.removeSuffix(".zip")
            } to true
        } else {
            emptyList<String>() to true
        }
    } else {
        throw throwable
    }
}

fun queryCompatibleTargets(projectPath: String): List<String> {
    val map = mutableSetOf<String>()
    val targetJson = Paths.get(projectPath).resolve("targets").resolve("targets.json")
    val jsonObject = Parser.default().parse(FileInputStream(targetJson.toFile())) as JsonObject
    for (target in jsonObject) {
        val targetObject = target.value as? JsonObject
        targetObject ?: continue
        val toolchains = targetObject.getOrDefault("supported_toolchains", null) as? JsonArray<*>
        if (toolchains != null) {
            if (toolchains.map { it as String }.contains("GCC_ARM")) {
                map.add(target.key)
            }
        } else {
            val parent = targetObject.getOrDefault("inherits", null) as? JsonArray<*> ?: continue
            if (parent.map { it as String }.any { map.contains(it) }) {
                map.add(target.key)
            }
        }
    }
    return map.toList()
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

fun changeMbedVersion(project: Project, virtualFile: VirtualFile, releaseTag: String, runAfter: () -> Unit = {}) {
    project.basePath ?: return
    val zipFile = Paths.get(virtualFile.path).resolve("..").resolve("$releaseTag.zip").toString()
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
                        if (!indicator.isIndeterminate) {
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
        ProgressManager.getInstance().run(
            ModalCanceableTask(
                project, "Unzipping and generating cmake project",
                unzipper@{ indicator ->
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
                        dialog.selectedTarget = PropertiesComponent.getInstance(project).getValue(LAST_TARGET_KEY)
                            ?: PropertiesComponent.getInstance().getValue(LAST_TARGET_KEY) ?: dialog.selectedTarget

                        if (dialog.showAndGet()) {
                            target = dialog.selectedTarget
                            PropertiesComponent.getInstance(project).setValue(LAST_TARGET_KEY, target)
                            PropertiesComponent.getInstance().setValue(LAST_TARGET_KEY, target)
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
                        val notification = MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                            "mbed cli failed to generate cmake project\n please verify that the cli works by running \"$cliPath export -i cmake_gcc_arm -m $target\" in the $mbedPath directory",
                            NotificationType.ERROR
                        )
                        Notifications.Bus.notify(notification,project)
                        return@unzipper
                    }
                    Files.write(
                        Paths.get(mbedPath).resolve("project.cmake"),
                        listOf(
                            "cmake_policy(SET CMP0076 NEW)",
                            "set_target_properties(mbed-os PROPERTIES CXX_STANDARD 17)",
                            "target_compile_options(mbed-os PUBLIC \$<\$<COMPILE_LANGUAGE:CXX>:-Wno-register>)",
                            "add_subdirectory(.. \${CMAKE_CURRENT_BINARY_DIR}/${Paths.get(project.basePath).fileName})",
                            ""
                        )
                    )
                    indicator.fraction = 1.0
                }, runAfter
            )
        )
    }
    ProgressManager.getInstance().run(downloadTask)
}