package dev.nelmin.kotlin;

import off.kys.kli.dsl.kli.kli
import off.kys.kli.io.confirm
import off.kys.kli.io.readInput
import off.kys.kli.io.select
import java.nio.file.Path
import java.util.Locale
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

            command("start") {
                description = "Setup a Minecraft Server"

                argument("software", "The Server Software (Paper, Spigot, ...)")
                argument("version", "The Server Version")
                argument("accept-eula", "Accepts the Minecraft EULA")

                action {
                    val software = get("software")
                        ?: select(listOf("Paper", "Spigot", "Pufferfish", "Purpur", "Velocity"))

                    choiceConfirmation(software)

                    val passedVersion = get("version")
                    val selectVersion = select(ServerVersions.VALUES[software]!!)
                    val version = if (passedVersion == null || !ServerVersions.VALUES.containsKey(software)) selectVersion else passedVersion
                    choiceConfirmation(version)

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
                    val serverPath = Path.of(path)
                    choiceConfirmation(serverPath.absolutePathString())
                }
            }
        }

        @JvmStatic
        fun choiceConfirmation(choice: String) = println("You chose: $choice")
    }
}

object Downloader {
    private fun download(apiUrl: String) {

    }

    fun paper(version: String) {
        val latestBuild = "latest"
        download("https://api.papermc.io/v2/projects/paper/$version/builds/$latestBuild")
    }
    fun spigot(version: String) {}
    fun purpur(version: String) {}
    fun pufferfish(version: String) {}
    fun velocity(version: String) {}
}

object ServerVersions {
    val PAPER = listOf(
        "1.21.5", "1.21.4", "1.21.3", "1.21.1", "1.21",
        "1.20.6", "1.20.5", "1.20.4", "1.20.2", "1.20.1", "1.20",
        "1.19.4", "1.19.3", "1.19.2", "1.19.1", "1.19",
        "1.18.2", "1.18.1", "1.18",
        "1.17.1", "1.17",
        "1.16.5", "1.16.4", "1.16.3", "1.16.2", "1.16.1",
        "1.15.1", "1.15",
        "1.14.4", "1.14.3", "1.14.2", "1.14.1", "1.14",
        "1.13.2", "1.13.1", "1.13", "1.13-pre7",
        "1.12.2", "1.12.1", "1.12",
        "1.11.2",
        "1.10.2",
        "1.9.4",
        "1.8.8",
    )

    val SPIGOT = listOf(
        "1.21.5", "1.21.4", "1.21.3", "1.21.1",
        "1.20.6", "1.20.4", "1.20.2", "1.20.1",
        "1.19.4", "1.19.3", "1.19.2", "1.19.1", "1.19",
        "1.18.2", "1.18.1", "1.18",
        "1.17.1", "1.17",
        "1.16.5", "1.16.4", "1.16.3", "1.16.2", "1.16.1",
        "1.15.1", "1.15",
        "1.14.4", "1.14.3", "1.14.2", "1.14.1", "1.14",
        "1.13.2", "1.13.1", "1.13",
        "1.12.2", "1.12.1", "1.12",
        "1.11.2", "1.11.1", "1.11",
        "1.10.2",
        "1.9.4", "1.9.2", "1.9",
        "1.8.8", "1.8.3", "1.8"
    )

    val PURPUR = PAPER;

    val PUFFERFISH = listOf(
        "1.21.3", "1.20.4", "1.19.4", "1.18.2", "1.17.1"
    )

    val VELOCITY = listOf(
        "3.4.0-SNAPSHOT",
        "3.3.0-SNAPSHOT",
        "3.2.0-SNAPSHOT",
        "3.1.2-SNAPSHOT",
        "3.1.1-SNAPSHOT",
        "3.1.0-SNAPSHOT",
        "3.1.1",
        "3.1.0",
        "1.1.9",
        "1.0.10",
    )

    val VALUES: Map<String, List<String>> = mapOf(
        "Paper" to PAPER,
        "Spigot" to SPIGOT,
        "Pufferfish" to PUFFERFISH,
        "Purpur" to PURPUR,
        "Velocity" to VELOCITY
    )
}