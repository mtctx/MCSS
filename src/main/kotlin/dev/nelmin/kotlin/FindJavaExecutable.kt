package dev.nelmin.kotlin

import java.io.File

// MC 1.12 and below is java 8
// MC 1.16 and below is java 11
// MC 1.17 and above is java 16
// MC 1.18 and above is java 17
// MC 1.20.5 and above is java 21
// Based on mcVersion search the system for the fitting java executable in the fastest way
// When the fitting version isn't found, use an updated version for example: Java 8 isn't found, use Java 11...

fun findJavaExecutable(mcVersion: String): String? {
    val (major, minor, patch) = parseVersion(mcVersion)
    val requiredVersion = determineRequiredJavaVersion(major, minor, patch)
    val candidates = findJavaCandidates()

    // First try to find exact version match
    val exactMatch = candidates.firstOrNull { it.version == requiredVersion }

    // Then find the lowest compatible version
    val suitable = candidates.filter { it.version >= requiredVersion }.sortedBy { it.version }

    return exactMatch?.path ?: suitable.firstOrNull()?.path ?: candidates.maxByOrNull { it.version }?.path ?: null
}

private fun parseVersion(version: String): Triple<Int, Int, Int> {
    val parts = version.split('.', '-')
    return Triple(
        parts.getOrElse(0) { "0" }.toIntOrNull() ?: 0,
        parts.getOrElse(1) { "0" }.toIntOrNull() ?: 0,
        parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0
    )
}

private fun determineRequiredJavaVersion(major: Int, minor: Int, patch: Int): Int = when {
    major == 1 && minor <= 12 -> 8
    major == 1 && minor <= 16 -> 11
    major == 1 && minor == 17 -> 16
    major == 1 && minor in 18..19 -> 17
    major == 1 && minor == 20 && patch < 5 -> 17
    major >= 1 && minor >= 17 -> 21  // Updated logic for modern versions
    else -> 21
}

private fun findJavaCandidates(): List<JavaInstallation> {
    val candidates = mutableListOf<JavaInstallation>()

    // Check common installation locations
    val searchRoots = listOf(
        File(System.getProperty("user.home")),  // ~/.jdks, ~/.sdkman, etc.
        File("/usr/lib/jvm"),
        File("/opt"),
        File("C:\\Program Files\\Java")
    ) + listOf("JAVA_HOME", "JDK_HOME").mapNotNull { System.getenv(it)?.let { File(it) } }

    searchRoots.forEach { root ->
        if (root.exists()) {
            root.walk()
                .maxDepth(3)
                .filter { it.isDirectory }
                .forEach { dir ->
                    val javaExe = detectJavaExecutable(dir)
                    if (javaExe != null) {
                        detectJavaVersion(javaExe)?.let {
                            candidates.add(JavaInstallation(javaExe.absolutePath, it))
                        }
                    }
                }
        }
    }

    // Check PATH entries
    System.getenv("PATH").split(File.pathSeparator).forEach { pathDir ->
        val javaExe = File(pathDir, if (File.separator == "/") "java" else "java.exe")
            .takeIf { it.exists() && it.canExecute() }
        javaExe?.let { exe ->
            detectJavaVersion(exe)?.let {
                candidates.add(JavaInstallation(exe.absolutePath, it))
            }
        }
    }

    return candidates.distinctBy { it.path }
}

private fun detectJavaExecutable(dir: File): File? {
    val unixExe = File(dir, "bin/java").takeIf { it.exists() && it.canExecute() }
    val winExe = File(dir, "bin\\java.exe").takeIf { it.exists() && it.canExecute() }
    return unixExe ?: winExe
}

private fun detectJavaVersion(javaExe: File): Int? {
    return try {
        val process = ProcessBuilder(javaExe.absolutePath, "-version").start()
        val errorOutput = process.errorStream.bufferedReader().readText()
        process.waitFor()
        parseJavaVersion(errorOutput)
    } catch (e: Exception) {
        null
    }
}

private fun parseJavaVersion(output: String): Int? {
    return Regex("version \"(1\\.)?(\\d+)(\\.\\d+)*\"")
        .find(output)
        ?.groups?.get(2)
        ?.value
        ?.toIntOrNull()
}

private data class JavaInstallation(val path: String, val version: Int)