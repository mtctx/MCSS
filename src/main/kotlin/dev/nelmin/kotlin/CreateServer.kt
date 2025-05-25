package dev.nelmin.kotlin

import java.io.File
import java.io.FileWriter

object CreateServer {
    private fun setupServer(path: String, javaPath: String, memoryInGB: Int, gui: Boolean, serverJarName: String) {
        File(path).mkdirs()

        val eulaFile = File(path, "eula.txt")
        if (!eulaFile.exists()) {
            eulaFile.createNewFile()
            FileWriter(eulaFile).use { fileWriter ->
                fileWriter.write("eula=true")
            }
        }

        generateServerCommands(
            javaPath,
            serverJarName,
            memoryInGB,
            gui
        ).forEach { (type, autostart, content) ->
            val startFile = File(path, "start${if (!autostart.isBlank()) "-${autostart}" else ""}.$type")
            FileWriter(startFile).use { fileWriter ->
                fileWriter.write(content)
            }
            startFile.setExecutable(true)
        }
    }

    fun server(path: String, javaPath: String, memoryInGB: Int, gui: Boolean) =
        setupServer(path, javaPath, memoryInGB, gui, "server.jar")

    fun proxy(path: String, javaPath: String, memoryInGB: Int, gui: Boolean) =
        setupServer(path, javaPath, memoryInGB, gui, "proxy.jar")

    private const val SERVER_JAVA_COMMAND = "{{{java}}} -Xms{{{mem}}}M -Xmx{{{mem}}}M --add-modules=jdk.incubator.vector -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -jar {{{name}}}"
    private const val PROXY_JAVA_COMMAND = "{{{java}}} -Xms{{{mem}}}M -Xmx{{{mem}}}M -XX:+UseG1GC -XX:G1HeapRegionSize=4M -XX:+UnlockExperimentalVMOptions -XX:+ParallelRefProcEnabled -XX:+AlwaysPreTouch -XX:MaxInlineLevel=15 -jar {{{name}}}"

    private fun generateServerCommands(
        javaPath: String,
        serverJar: String,
        memoryInGB: Int,
        gui: Boolean
    ): List<Triple<String, String, String>> {  // Return script type and content
        val isWindows = System.getProperty("os.name").contains("Windows", true)

        val javaCommand = (if (serverJar == "proxy.jar") PROXY_JAVA_COMMAND else SERVER_JAVA_COMMAND)
            .replace("{{{java}}}", javaPath)
            .replace("{{{name}}}", serverJar)
            .replace("{{{mem}}}", (memoryInGB * 1024).toString())
            .plus(if (!gui) " --nogui" else "")

        return listOf(
            Triple("bat", "autostart", """
                    @echo off
                    :start
                    $javaCommand
                    echo Server restarting...
                    echo Press CTRL + C to stop.
                    goto :start
                """.trimIndent()),
            Triple("sh", "autostart", """
                    #!/bin/bash
                    while true; do
                        $javaCommand
                        echo "Server restarting..."
                        echo "Press CTRL + C to stop."
                    done
                """.trimIndent()),
            Triple("bat", "", """
                    @echo off
                    $javaCommand
                    pause
                """.trimIndent()),
            Triple("sh", "", """
                    #!/bin/bash
                    $javaCommand
                """.trimIndent())
        )
    }
}