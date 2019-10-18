package net.zero9178

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import java.io.File
import java.io.FileInputStream
import java.nio.file.Paths

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

fun changeTargetDialog(project: Project) {
    val projectPath = project.basePath ?: return
    val list = queryCompatibleTargets(Paths.get(projectPath).resolve("mbed-os").toString())
    val dialog = MbedTargetSelectImpl(list, project)
    if (dialog.showAndGet()) {
        val cli = PropertiesComponent.getInstance().getValue(
            CLI_PATH_KEY
        )
        if (cli == null) {
            Notifications.Bus.notify(
                MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                    "Invalid mbed cli specified in settings",
                    NotificationType.ERROR
                ), project
            )
            return
        }
        ProcessBuilder().directory(File(projectPath))
            .command(cli, "export", "-i", "cmake_gcc_arm", "-m", dialog.selectedTarget).start().waitFor()
    }
}