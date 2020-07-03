package net.zero9178.mbed.packages

import com.beust.klaxon.Json
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import net.zero9178.mbed.MbedAsyncTask
import net.zero9178.mbed.MbedNotification
import net.zero9178.mbed.MbedSyncTask
import net.zero9178.mbed.editor.MbedAppLibDaemon
import net.zero9178.mbed.gui.MbedTargetSelectImpl
import net.zero9178.mbed.state.MbedProjectState
import net.zero9178.mbed.state.MbedState
import org.apache.commons.lang.StringUtils
import java.io.File
import java.io.FileReader
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private object MbedVersionTargetLogger {
    val log = Logger.getInstance(Logger::class.java)
}

data class Target(
    val inherits: List<String>? = null,
    val core: String? = null,
    val public: Boolean? = null,
    val macros: List<String>? = null,
    @Json(name = "macros_add") val macrosAdded: List<String>? = null,
    @Json(name = "macros_remove") val macrosRemoved: List<String>? = null,
    @Json(name = "extra_labels") val extraLabels: List<String>? = null,
    @Json(name = "extra_labels_add") val extraLabelsAdd: List<String>? = null,
    @Json(name = "extra_labels_remove") val extraLabelsRemove: List<String>? = null,
    val features: List<String>? = null,
    @Json(name = "features_add") val featuresAdd: List<String>? = null,
    @Json(name = "features_remove") val featuresRemove: List<String>? = null,
    @Json(name = "supported_toolchains") val supportedToolchains: List<String>? = null,
    @Json(name = "device_name") val deviceName: String? = null,
    @Json(name = "OUTPUT_EXT") val outputExtension: String? = null,
    @Json(name = "default_lib") val cLibrary: String? = null
)

/**
 * Queries compatible targets for GCC_ARM
 *
 * @param mbedOsPath path to mbed-os
 */
fun queryCompatibleTargets(mbedOsPath: String): List<String> {
    val targetJsonPath = Paths.get(mbedOsPath).resolve("targets").resolve("targets.json")
    val jsonObject = FileReader(targetJsonPath.toFile()).use {
        Klaxon().parseJsonObject(it)
    }
    val targetMap = mutableMapOf<String, Target>()
    for ((name, obj) in jsonObject) {
        obj as? JsonObject ?: continue
        val target = try {
            Klaxon().parseFromJsonObject<Target>(obj) ?: continue
        } catch (e: KlaxonException) {
            MbedVersionTargetLogger.log.error(e)
            continue
        }
        targetMap[name] = target
    }

    // Returns null if the target does not have a supported toolchains property
    // True if it contains GCC_ARM and false if it doesn't
    fun isGCCCompatible(target: Target): Boolean? {
        if (target.supportedToolchains != null) {
            return target.supportedToolchains.contains("GCC_ARM")
        }
        if (target.inherits.isNullOrEmpty()) {
            return null
        }
        return target.inherits.any {
            val inherited = targetMap[it] ?: return@any false
            val result = isGCCCompatible(inherited)
            // Inheritance works like in Python
            // isGCCCompatible returns null if the support toolchain property does not exist in the target or
            // any of it's base targets. If it doesn't return null the property exists. Therefore the value of the
            // property is also the value of this target's supportedToolchains property and the search is cancelled
            if (result != null) {
                return result
            } else {
                return@any false
            }
        }
    }

    return targetMap.toList().fold(emptyList()) { result, (name, target) ->
        if (target.public != false && isGCCCompatible(target) == true) {
            result + name
        } else {
            result
        }
    }
}

/**
 * Convenience function that queries targets and pops up a dialog with the query result
 * @return null if the user closed the dialog, chosen item if not
 */
fun changeTargetDialog(project: Project): String? {
    val projectPath = project.basePath ?: return null
    var list: List<String> = emptyList()
    var result: String? = null
    ProgressManager.getInstance().run(MbedSyncTask(project, "Querying targets", {
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
    ProgressManager.getInstance().run(MbedAsyncTask(project, "Generating cmake", {
        val process = ProcessBuilder().directory(File(projectPath)).command(cli, "target", target).start()
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            val output = process.inputStream.bufferedReader().readLines().joinToString("\n")
            Notifications.Bus.notify(
                MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                    "mbed process timed out while setting target with output: $output",
                    NotificationType.ERROR
                ), project
            )
            return@MbedAsyncTask
        }
        if (process.exitValue() != 0) {
            val output = process.inputStream.bufferedReader().readLines().joinToString("\n")
            Notifications.Bus.notify(
                MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                    "mbed failed to set target with error code ${process.exitValue()} and output: $output",
                    NotificationType.ERROR
                ), project
            )
            return@MbedAsyncTask
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
            if (MbedProjectState.getInstance(project).isRelease) "release" else "debug",
            *MbedProjectState.getInstance(project).additionalProfiles.fold(emptyArray<String>()) { result, curr ->
                (result + "--profile") + curr
            }
        ).start()
    if (!process.waitFor(10, TimeUnit.SECONDS)) {
        val output = process.inputStream.bufferedReader().readLines().joinToString("\n")
        Notifications.Bus.notify(
            MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                "mbed export timed out with output: $output",
                NotificationType.ERROR
            ),
            project
        )
        return
    }
    if (process.exitValue() == 0) {
        project.putUserData(MbedAppLibDaemon.PROJECT_NEEDS_RELOAD, false)
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

class QueryPackageSuccess(val topMbedPackage: MbedPackage) : QueryPackageResult()

class MbedPackage(val name: String, val repo: String?, val dependencies: MutableList<MbedPackage> = mutableListOf()) {
    override fun toString() = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MbedPackage

        if (name != other.name) return false
        if (repo != other.repo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (repo?.hashCode() ?: 0)
        return result
    }
}

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
            val lines = start.errorStream.bufferedReader().readLines()
            return@supplyAsync QueryPackageError("CLI failed with error: ${lines.joinToString("\n")}")
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
            val indent = line.indexOfFirst { it.isLetterOrDigit() }
            if (indent == 0 || (indent > 1 && (line[indent - 2] == '-'))) {
                current = null to mutableListOf()
                val trim = line.substring(indent).substringBefore('(').trim()
                map[trim] = current
                currentName = trim
            } else if (line.substring(0, indent).contains('*')) {
                val tag = line.substring(indent).trim()
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
    val p = ProcessBuilder().directory(File(path)).command(cli, "update", version).redirectErrorStream(true).start()
    p.waitFor()
    return if (p.exitValue() != 0) {
        val lines = p.inputStream.bufferedReader().readLines()
        Notifications.Bus.notify(
            MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                "mbed update failed with error code ${p.exitValue()} and output: ${lines.joinToString("\n")}",
                NotificationType.ERROR
            ),
            project
        )
        false
    } else {
        true
    }
}
