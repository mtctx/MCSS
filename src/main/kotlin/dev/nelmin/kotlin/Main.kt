package dev.nelmin.kotlin

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import off.kys.kli.dsl.kli.kli
import off.kys.kli.dsl.progress.progressBar
import off.kys.kli.io.confirm
import off.kys.kli.io.readInput
import off.kys.kli.io.select
import off.kys.kli.ui.progress.util.ProgressType
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolutePathString

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = kli(args) {
            configure {
                name = "Minecraft Server Setup"
                version = "1.0.0"
                description = "A simple tool to setup a Minecraft Server"
            }

            command("t") {
                action {
                    println(VersionFetcher.paper())
                }
            }

            command("start") {
                description = "Setup a Minecraft Server"

                argument("software", "The Server Software (Paper, Spigot, ...)")
                argument("version", "The Server Version")
                argument("accept-eula", "Accepts the Minecraft EULA")

                action {
                    val software = get("software")
                        ?: select(listOf("Paper", "Spigot", "Purpur", "Velocity"))

                    choiceConfirmation(software)

                    val passedVersion = get("version")
                    val selectVersion = select(ServerVersions.VALUES[software]!!)
                    val mcVersion =
                        if (passedVersion == null || !ServerVersions.VALUES.containsKey(software)) selectVersion else passedVersion
                    choiceConfirmation(mcVersion)

                    val acceptEula: Boolean = get("accept-eula")?.toBoolean() ?: confirm(
                        "Do you accept the Minecraft EULA? (https://www.minecraft.net/${
                            Locale.getDefault().toLanguageTag()
                        }/eula)"
                    )
                    if (!acceptEula) {
                        println("You must accept the Minecraft EULA to use this software.")
                        return@action
                    }
                    choiceConfirmation("Yes")

                    var path = get("path") ?: readInput("Where should the server be created?")
                    if (path.isBlank()) path = System.getProperty("user.dir")
                    if (path.startsWith("." + File.separator)) path = path.replace("." + File.separator, System.getProperty("user.dir") + File.separator)
                    val serverPath = Path.of(path)
                    choiceConfirmation(serverPath.absolutePathString())

                    val javaPath = findJavaExecutable(mcVersion)
                    if (javaPath == null) {
                        println(errorJavaNotFound(mcVersion))
                        return@action
                    }

                    println("Using Java $javaPath")

                    progressBar {
                        type = ProgressType.BAR
                        width = 100
                        prefix = "Working"
                        suffix = "%"
                        start {
                            when (software) {
                                "Paper" -> Downloader.paper(
                                    serverPath.absolutePathString(),
                                    mcVersion,
                                    onProgress = { progress -> update(progress) },
                                    onError = { message -> error(message) }
                                )

                                "Spigot" -> Downloader.spigot(
                                    serverPath.absolutePathString(),
                                    mcVersion,
                                    onProgress = { progress -> update(progress) },
                                    onError = { message -> error(message) })

                                "Purpur" -> Downloader.purpur(
                                    serverPath.absolutePathString(),
                                    mcVersion,
                                    onProgress = { progress -> update(progress) },
                                    onError = { message -> error(message) })

                                "Velocity" -> Downloader.velocity(
                                    serverPath.absolutePathString(),
                                    mcVersion,
                                    onProgress = { progress -> update(progress) },
                                    onError = { message -> error(message) })
                            }
                        }
                    }

                    val memoryInGB = readInput("How much memory should the server have? (In GB, Default = 4)").toIntOrNull() ?: 4
                    val gui = confirm("Do you want to enable the server gui?")

                    if (software == "velocity") {
                        CreateServer.proxy(path, javaPath, memoryInGB, gui)
                    } else {
                        CreateServer.server(path, javaPath, memoryInGB, gui)
                    }
                }
            }
        }

        @JvmStatic
        fun choiceConfirmation(choice: String) = println("You chose: $choice")
    }
}

val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
    engine {
        requestTimeout = 0 // Disable timeout
        endpoint {
            connectTimeout = 60_000
            socketTimeout = 300_000
            keepAliveTime = 60_000
        }
    }
}

fun errorJavaNotFound(mcVersion: String): String {
    return when {
        mcVersion.startsWith("1.20.5") -> "Required Java 21 not found. Currently installed versions are not compatible. Please install Java 21 or newer."
        mcVersion.startsWith("1.18") || mcVersion.startsWith("1.19") || mcVersion.startsWith("1.20") -> "Required Java 17 not found. Currently installed versions are not compatible. Please install Java 17 or newer."
        mcVersion.startsWith("1.17") -> "Required Java 16 not found. Currently installed versions are not compatible. Please install Java 16 or newer."
        mcVersion.startsWith("1.13") || mcVersion.startsWith("1.14") || mcVersion.startsWith("1.15") || mcVersion.startsWith(
            "1.16"
        ) -> "Required Java 11 not found. Currently installed versions are not compatible. Please install Java 11 or newer."

        mcVersion.startsWith("1.12") || mcVersion.startsWith("1.11") || mcVersion.startsWith("1.10") || mcVersion.startsWith(
            "1.9"
        ) || mcVersion.startsWith("1.8") -> "Required Java 8 not found. Currently installed versions are not compatible. Please install Java 8 or newer."

        else -> "Required Java version not found for Minecraft $mcVersion. Please install the appropriate Java version and try again."
    }
}