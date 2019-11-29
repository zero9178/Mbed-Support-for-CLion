package net.zero9178.mbed.packages

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import net.zero9178.mbed.MbedNotification
import net.zero9178.mbed.ModalTask
import net.zero9178.mbed.editor.MbedAppLibDirtyMarker
import net.zero9178.mbed.gui.MbedTargetSelectImpl
import net.zero9178.mbed.state.MbedProjectState
import net.zero9178.mbed.state.MbedState
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.PumpStreamHandler
import org.apache.commons.lang.StringUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Queries compatible targets for GCC_ARM
 *
 * @param mbedOsPath path to mbed-os
 */
fun queryCompatibleTargets(mbedOsPath: String): List<String> {
    val map = mutableSetOf<String>()
    val targetJson = Paths.get(mbedOsPath).resolve("targets").resolve("targets.json")
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

/**
 * Convenience function that queries targets and pops up a dialog with the query result
 * @return null if the user closed the dialog, chosen item if not
 */
fun changeTargetDialog(project: Project): String? {
    val projectPath = project.basePath ?: return null
    var list: List<String> = emptyList()
    var result: String? = null
    ProgressManager.getInstance().run(ModalTask(project, "Querying targets", {
        list = queryCompatibleTargets(Paths.get(projectPath).resolve("mbed-os").toString())
    }) {
        val dialog = MbedTargetSelectImpl(list, project)
        result = if (dialog.showAndGet()) {
            dialog.selectedTarget
        } else {
            null
        }
    })
    return result
}

/**
 * Changes target for the project. Shows progress bar and blocks
 *
 * @param target Target to switch to
 * @param project Project
 */
fun changeTarget(target: String, project: Project) {
    val projectPath = project.basePath ?: return
    val cli = MbedState.getInstance().cliPath
    ProgressManager.getInstance().run(ModalTask(project, "Generating cmake", {
        val process = ProcessBuilder().directory(File(projectPath)).command(cli, "target", target).start()
        if (process.waitFor() != 0) {
            val output = process.inputStream.bufferedReader().readLines().joinToString("\n")
            Notifications.Bus.notify(
                MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                    "mbed failed to set target with error code ${process.exitValue()} and output: $output",
                    NotificationType.ERROR
                ), project
            )
            return@ModalTask
        }
        exportToCmake(project)
    }))
}

/**
 * Generates config headers and cmake file from mbed jsons. Does not return until generation is done
 *
 * @param project Project
 */
fun exportToCmake(project: Project) {
    val projectPath = project.basePath ?: return
    val cli = MbedState.getInstance().cliPath
    val process = ProcessBuilder().directory(File(projectPath)).redirectErrorStream(true)
        .command(
            cli,
            "export",
            "-i",
            "cmake_gcc_arm",
            "--profile",
            if (MbedProjectState.getInstance(project).isRelease) "release" else "debug"
        ).start()
    if (process.waitFor() == 0) {
        project.putUserData(MbedAppLibDirtyMarker.NEEDS_RELOAD, false)
        CMakeWorkspace.getInstance(project).scheduleReload(true)
    } else {
        val output = process.inputStream.bufferedReader().readLines().joinToString("\n")
        Notifications.Bus.notify(
            MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                "mbed export to cmake failed with error code ${process.exitValue()} and output: $output",
                NotificationType.ERROR
            ),
            project
        )
    }
}

/**
 * @param project Project
 * @return target that has last been used to generate header and cmake files or null if no generation has ever taken
 */
fun getLastTarget(project: Project): String? {
    val projectPath = project.basePath ?: return null
    val cli = MbedState.getInstance().cliPath
    if (!File(cli).exists()) {
        return null
    }
    val start = ProcessBuilder().directory(File(projectPath)).command(cli, "target").start()
    if (start.waitFor() != 0) {
        return null
    }
    val line = start.inputStream.bufferedReader().readLines().lastOrNull()?.trim() ?: return null
    return if (line.startsWith("[mbed] No default target")) null else line.substringAfterLast(' ')
}

sealed class QueryPackageResult

class QueryPackageError(val message: String) : QueryPackageResult()

class MbedPackage(val name: String, val repo: String?, val dependencies: MutableList<MbedPackage> = mutableListOf()) {
    override fun toString() = name
}

class QueryPackageSuccess(val topMbedPackage: MbedPackage) : QueryPackageResult()

/**
 * Queries all packages of the project recursively and async
 * @param project Project
 * @return Future containing either a QueryPackageError with an error string or a QueryPackageSuccess with the top level
 * package
 */
fun queryPackages(project: Project): CompletableFuture<QueryPackageResult> {
    val projectPath =
        project.basePath ?: return CompletableFuture.completedFuture(QueryPackageError("Project has no base path"))
    val cli = MbedState.getInstance().cliPath
    if (!File(cli).exists()) {
        return CompletableFuture.completedFuture(QueryPackageError("CLI Path invalid"))
    }

    return CompletableFuture.supplyAsync {
        val start = ProcessBuilder().directory(File(projectPath)).command(cli, "ls", "-a").start()
        if (start.waitFor() != 0) {
            val lines = start.inputStream.bufferedReader().readLines()
            return@supplyAsync QueryPackageError("CLI failed with error ${lines.joinToString("\n")}")
        }
        val lines = start.inputStream.bufferedReader().readLines().dropWhile { it.first() == '[' }
        val topLevelPackage = mutableListOf<MbedPackage>()
        val packages = TreeMap<Int, MbedPackage>()
        for (line in lines) {
            val indent = line.takeWhile { !it.isLetterOrDigit() }.length
            val substring = line.substring(indent)
            if (indent == 0) {
                topLevelPackage += MbedPackage(
                    substring.substringBefore('(').trim(),
                    StringUtils.substringBetween(substring, "(", ")")
                )
                packages[0] = topLevelPackage.last()
            } else {
                val newPackage =
                    MbedPackage(
                        substring.substringBefore('(').trim(),
                        StringUtils.substringBetween(substring, "(", ")")
                    )
                packages[indent] = newPackage
                val parent = packages.entries.findLast { it.key < indent } ?: continue
                parent.value.dependencies += newPackage
            }
        }

        QueryPackageSuccess(topLevelPackage[0])
    }
}

/**
 * Queries releases of all packages of the project recursively and async
 *
 * @param project Project
 * @return Future containing a map with package name as key and a pair containing optionally the current version as
 * first value and all available versions as second
 */
fun queryReleases(project: Project): CompletableFuture<Map<String, Pair<String?, List<String>>>> {
    val projectPath = project.basePath ?: return CompletableFuture.completedFuture(emptyMap())
    val cli = MbedState.getInstance().cliPath
    if (!File(cli).exists()) {
        return CompletableFuture.completedFuture(emptyMap())
    }

    return CompletableFuture.supplyAsync {
        val start = ProcessBuilder().directory(File(projectPath)).command(cli, "releases", "-r").start()
        if (start.waitFor() != 0) {
            return@supplyAsync emptyMap<String, Pair<String?, List<String>>>()
        }
        val lines = start.inputStream.bufferedReader().readLines().dropWhile { it.first() == '[' }

        var current: Pair<String?, MutableList<String>>? = null
        var currentName: String? = null
        val map = mutableMapOf<String, Pair<String?, List<String>>>()
        for (line in lines) {
            val index = line.indexOfFirst { it.isLetterOrDigit() }
            if (index == 0 || (index > 1 && (line[index - 2] == '-'))) {
                current = null to mutableListOf()
                val trim = line.substring(index).substringBefore('(').trim()
                map[trim] = current
                currentName = trim
            } else if (line.substring(0, index).contains('*')) {
                val tag = line.substring(index)
                if (tag.endsWith("<- current") && currentName != null) {
                    val fixed = tag.removeSuffix("<- current").trim()
                    current = fixed to (current?.second?.let { it + fixed }?.toMutableList() ?: mutableListOf())
                    map[currentName] = current
                } else {
                    current?.second?.add(tag)
                }
            }
        }

        map
    }
}

/**
 * Updates package to specified version
 *
 * @param path Path to the directory of the package
 * @param version Version to update to
 * @param project Project
 * @return True if successful
 */
fun updatePackage(path: String, version: String, project: Project): Boolean {
    val cli = MbedState.getInstance().cliPath
    if (!File(cli).exists()) {
        return false
    }
    val cl = CommandLine.parse("$cli update $version")
    val exec = DefaultExecutor()
    exec.workingDirectory = File(path)
    val output = ByteArrayOutputStream()
    exec.streamHandler = PumpStreamHandler(output)
    val exitCode = exec.execute(cl)
    return if (exitCode != 0) {
        Notifications.Bus.notify(
            MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                "mbed update failed with error code $exitCode and output: $output",
                NotificationType.ERROR
            ),
            project
        )
        false
    } else {
        true
    }
}
