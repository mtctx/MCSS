package dev.nelmin.kotlin

import dev.nelmin.kotlin.json.PaperVersionResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

object Downloader {
    private suspend fun download(
        serverPath: String,
        apiUrl: String,
        forcedFilename: String? = null,
        onProgress: ((Int) -> Unit),
        onError: ((String) -> Unit)
    ) {
        var tempFile: File? = null
        val maxRetries = 3
        var retryCount = 0

        while (true) {
            try {
                val response = httpClient.get(apiUrl)

                if (!response.status.isSuccess()) {
                    withContext(Dispatchers.Main) {
                        onError("HTTP error ${response.status.value}: ${response.status.description}")
                    }
                    return
                }

                val fileName = forcedFilename ?: apiUrl.substringAfterLast('/')
                val file = File(serverPath, fileName).canonicalFile
                val contentLength = response.contentLength() ?: 0
                var bytesReceived = 0L

                file.parentFile?.mkdirs()
                tempFile = File(file.parentFile, "${fileName}.tmp.${UUID.randomUUID()}")

                response.bodyAsChannel().toInputStream().use { input ->
                    tempFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            bytesReceived += bytesRead

                            withContext(Dispatchers.Main) {
                                onProgress(
                                    if (contentLength > 0) ((bytesReceived.toDouble() / contentLength) * 100).toInt()
                                    else -1
                                )
                            }
                        }
                        output.flush()
                    }
                }

                // Validate complete download
                if (contentLength > 0 && bytesReceived != contentLength) {
                    throw IOException("Incomplete download: $bytesReceived/$contentLength bytes")
                }

                // Atomic file replacement
                if (file.exists() && !file.delete()) {
                    throw IOException("Failed to remove existing file")
                }
                if (!tempFile.renameTo(file)) {
                    throw IOException("Failed to finalize downloaded file")
                }

                // Final verification
                if (file.length() != bytesReceived) {
                    throw IOException("File verification failed after write")
                }

                return
            } catch (e: Exception) {
                tempFile?.delete()
                retryCount++

                if (retryCount >= maxRetries) {
                    withContext(Dispatchers.Main) {
                        onError("Download failed: ${e.message ?: "Unknown error"}")
                    }
                    val fileName = forcedFilename ?: apiUrl.substringAfterLast('/')
                    File(serverPath, fileName).takeIf { it.exists() }?.delete()
                    return
                }
            }
        }
    }

    fun paper(serverPath: String, version: String, onProgress: ((Int) -> Unit), onError: ((String) -> Unit)) = runBlocking(Dispatchers.IO) {
        try {
            val response = httpClient.get("https://api.papermc.io/v2/projects/paper/versions/$version").apply {
                if (!status.isSuccess()) {
                    onError("Paper API error: ${status.value} ${status.description}")
                    return@runBlocking
                }
            }

            val versionsResponse = try {
                response.body<PaperVersionResponse>()
            } catch (e: Exception) {
                onError("Failed to parse Paper API response: ${e.message}")
                return@runBlocking
            }

            val latestBuild = try {
                versionsResponse.getLatestBuild()
            } catch (e: Exception) {
                onError("Invalid build number: ${e.message}")
                return@runBlocking
            }

            download(
                serverPath,
                "https://api.papermc.io/v2/projects/paper/versions/$version/builds/$latestBuild/downloads/" +
                        "paper-$version-$latestBuild.jar",
                "server.jar",
                onProgress,
                onError
            )
        } catch (e: Exception) {
            onError("Paper setup failed: ${e.message ?: "Unknown error"}")
        }
    }

    fun spigot(serverPath: String, version: String, onProgress: ((Int) -> Unit), onError: ((String) -> Unit)) =
        runBlocking(Dispatchers.IO) {
            fun isGitInstalled(): Boolean {
                return try {
                    ProcessBuilder("git", "--version")
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start()
                        .waitFor() == 0
                } catch (e: Exception) {
                    false
                }
            }

            if (!isGitInstalled()) {
                withContext(Dispatchers.Main) {
                    onError("Git is not installed. Please install Git and try again.")
                }
                return@runBlocking
            }

            val javaPath = findJavaExecutable(version)
            if (javaPath == null) {
                withContext(Dispatchers.Main) {
                    onError(errorJavaNotFound(version))
                }
                return@runBlocking
            }

            download(
                serverPath,
                "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar",
                "BuildTools.jar",
                onProgress,
                onError
            )

            if (!File(serverPath, "BuildTools.jar").exists()) {
                withContext(Dispatchers.Main) {
                    onError("BuildTools.jar download failed - file not found")
                }
                return@runBlocking
            }

            val exitCode = ProcessBuilder(javaPath, "-jar", "BuildTools.jar", "--rev", version)
                .directory(File(serverPath))
                .inheritIO()
                .start()
                .waitFor()

            if (exitCode != 0) {
                withContext(Dispatchers.Main) {
                    onError("BuildTools failed with exit code $exitCode")
                }
                return@runBlocking
            }

            val spigotJar = File(serverPath, "spigot-$version.jar")
            val serverJar = File(serverPath, "server.jar")
            if (!spigotJar.exists()) {
                withContext(Dispatchers.Main) {
                    onError("Build failed - spigot-$version.jar not generated")
                }
                File(serverPath).listFiles()?.forEach { it.delete() }
                return@runBlocking
            }

            serverJar.delete()
            if (!spigotJar.renameTo(serverJar)) {
                withContext(Dispatchers.Main) {
                    onError("Failed to rename spigot jar to server.jar")
                }
            }
        }

    fun velocity(serverPath: String, version: String, onProgress: ((Int) -> Unit), onError: ((String) -> Unit)) =
        runBlocking(Dispatchers.IO) {
            try {
                val response = httpClient.get("https://api.papermc.io/v2/projects/velocity/versions/$version").apply {
                    if (!status.isSuccess()) {
                        onError("Velocity API error: ${status.value} ${status.description}")
                        return@runBlocking
                    }
                }

                val versionsResponse = try {
                    response.body<PaperVersionResponse>()
                } catch (e: Exception) {
                    onError("Failed to parse Velocity API response: ${e.message}")
                    return@runBlocking
                }

                val latestBuild = try {
                    versionsResponse.getLatestBuild()
                } catch (e: Exception) {
                    onError("Invalid build number: ${e.message}")
                    return@runBlocking
                }

                download(
                    serverPath,
                    "https://api.papermc.io/v2/projects/velocity/versions/$version/builds/$latestBuild/downloads/" +
                            "velocity-$version-$latestBuild.jar",
                    "proxy.jar",
                    onProgress,
                    onError
                )
            } catch (e: Exception) {
                onError("Velocity setup failed: ${e.message ?: "Unknown error"}")
            }
        }

    fun purpur(serverPath: String, version: String, onProgress: ((Int) -> Unit), onError: ((String) -> Unit)) =
        runBlocking(Dispatchers.IO) {
            try {
                val response = httpClient.get("https://api.purpurmc.org/v2/purpur/$version").apply {
                    if (!status.isSuccess()) {
                        onError("Purpur API error: ${status.value} ${status.description}")
                        return@runBlocking
                    }
                }

                download(
                    serverPath,
                    "https://api.purpurmc.org/v2/purpur/$version/latest/download",
                    "server.jar",
                    onProgress,
                    onError
                )
            } catch (e: Exception) {
                onError("Purpur setup failed: ${e.message ?: "Unknown error"}")
            }
        }
}