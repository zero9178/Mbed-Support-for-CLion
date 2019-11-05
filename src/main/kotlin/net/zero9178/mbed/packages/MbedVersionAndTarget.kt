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
import net.zero9178.mbed.state.MbedState
import java.io.File
import java.io.FileInputStream
import java.nio.file.Paths
import java.util.*

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

fun exportToCmake(project: Project) {
    val projectPath = project.basePath ?: return
    val cli = MbedState.getInstance().cliPath
    val process = ProcessBuilder().directory(File(projectPath)).redirectErrorStream(true)
        .command(cli, "export", "-i", "cmake_gcc_arm").start()
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
    return line.substringAfterLast(' ')
}

sealed class QueryResult

class QueryError(val message: String) : QueryResult()

class Package(val name: String, val dependencies: MutableList<Package> = mutableListOf()) {
    override fun toString() = name
}

class QuerySuccess(val packages: List<Package>) : QueryResult()

fun queryPackages(project: Project): QueryResult {
    val projectPath = project.basePath ?: return QueryError("Project has no base path")
    val cli = MbedState.getInstance().cliPath
    if (!File(cli).exists()) {
        return QueryError("CLI Path invalid")
    }
    val start = ProcessBuilder().directory(File(projectPath)).command(cli, "ls", "-a").start()
    if (start.waitFor() != 0) {
        val lines = start.inputStream.bufferedReader().readLines()
        return QueryError("CLI failed with error ${lines.joinToString("\n")}")
    }
    val lines = start.inputStream.bufferedReader().readLines().dropWhile { it.first() == '[' }
    val topLevelPackage = mutableListOf<Package>()
    val packages = TreeMap<Int, Package>()
    for (line in lines) {
        val offset = line.takeWhile { !it.isLetterOrDigit() }.length
        if (offset == 0) {
            topLevelPackage += Package(line.substring(offset).substringBefore('(').trim())
            packages[0] = topLevelPackage.last()
        } else {
            val newPackage = Package(line.substring(offset).substringBefore('(').trim())
            packages[offset] = newPackage
            val parent = packages.entries.findLast { it.key < offset } ?: continue
            parent.value.dependencies += newPackage
        }
    }

    return QuerySuccess(topLevelPackage)
}