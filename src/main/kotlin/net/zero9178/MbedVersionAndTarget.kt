package net.zero9178

import org.kohsuke.github.GitHub
import java.util.concurrent.CompletableFuture

val ARM_MBED_REPOSITORY = "ARMmbed/mbed-os"

fun getMbedOSReleaseVersionsAsync() = CompletableFuture.supplyAsync {
    val github = GitHub.connectAnonymously()
    github.getRepository(ARM_MBED_REPOSITORY).listReleases().map {
        it.name
    }
}!!