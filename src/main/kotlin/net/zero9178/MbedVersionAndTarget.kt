package net.zero9178

import org.kohsuke.github.GitHub
import java.util.concurrent.CompletableFuture

fun getMbedOSReleaseVersionsAsync() = CompletableFuture.supplyAsync {
    val github = GitHub.connectAnonymously()
    github.getRepository("ARMmbed/mbed-os").listReleases().map {
        it.name
    }
}!!