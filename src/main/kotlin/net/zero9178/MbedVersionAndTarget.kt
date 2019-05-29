package net.zero9178

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import org.kohsuke.github.GitHub
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

const val ARM_MBED_REPOSITORY = "ARMmbed/mbed-os"

fun getMbedOSReleaseVersionsAsync() = CompletableFuture.supplyAsync {
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
    result
}!!

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