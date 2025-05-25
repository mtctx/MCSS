# Minecraft Server Setup

A command-line tool to simplify the setup of a Minecraft Server. This tool helps you download the server software, configure basic settings, and generate the necessary startup scripts.

## Features

*   **Supported Server Software**: Easily download and set up various Minecraft server types:
    *   Paper
    *   Spigot
    *   Purpur
    *   Velocity (Proxy)
*   **Version Selection**: Choose from available versions for the selected server software.
*   **EULA Acceptance**: Prompts for Minecraft EULA acceptance.
*   **Customizable Setup**:
    *   Specify the server installation path.
    *   Set the amount of RAM allocated to the server.
    *   Option to enable the server GUI.
*   **Java Version Check**: Automatically checks if a compatible Java version is installed for the chosen Minecraft version and provides guidance if not.
*   **Interactive Mode**: If arguments are not provided, the tool will guide you through the setup process with interactive prompts.
*   **Progress Bar**: Shows download progress for server files.

## Prerequisites

*   **Java Development Kit (JDK)**: A compatible version of Java is required depending on the Minecraft version you choose. The tool will inform you if your current Java version is not suitable.
    *   Minecraft 1.20.5 and newer: Java 21 or newer
    *   Minecraft 1.18.x - 1.20.x: Java 17 or newer
    *   Minecraft 1.17.x: Java 16 or newer
    *   Minecraft 1.13.x - 1.16.x: Java 11 or newer
    *   Minecraft 1.8.x - 1.12.x: Java 8 or newer

## Building the Tool

This project uses Gradle. To build the executable JAR:

1.  Make sure you have JDK 21 installed, as specified in the `build.gradle.kts` (`kotlin { jvmToolchain(21) }`).
2.  Navigate to the project's root directory.
3.  Run the Gradle `build` task. This will also execute the `shadowJar` task, creating a fat JAR.
    ```bash
    ./gradlew build
    ```
4.  The executable JAR file, `MinecraftServerSetup.jar`, will be located in the `build/libs/` directory.

## Usage

Run the tool from your terminal using the generated JAR file.
```
bash
java -jar MinecraftServerSetup.jar <command> [arguments...]
```
### Command: `start`

Sets up a new Minecraft Server.

**Synopsis:**
```
bash
java -jar MinecraftServerSetup.jar start [software] [version] [accept-eula] [path] [memory] [gui]
```
**Arguments:**

*   `software` (optional): The server software to install (e.g., `Paper`, `Spigot`, `Purpur`, `Velocity`). If not provided, you'll be prompted to select one.
*   `version` (optional): The version of the server software. If not provided, you'll be prompted to select from available versions for the chosen software.
*   `accept-eula` (optional): Set to `true` to automatically accept the Minecraft EULA, or `false`. If not provided, you will be prompted.
*   `path` (optional): The directory where the server should be created. If not provided, you'll be prompted. Defaults to the current directory if the input is blank.
*   `memory` (optional): The amount of RAM (in GB) to allocate to the server. Defaults to 4 GB if not provided or if an invalid value is entered.
*   `gui` (optional): Set to `true` to enable the server's GUI, or `false`. If not provided, you will be prompted.

**Interactive Mode:**

If you run `java -jar MinecraftServerSetup.jar start` without all the required arguments, the tool will prompt you for each necessary piece of information.

**Example:**

To set up a Paper server for Minecraft version 1.20.4, accept the EULA, install it in a folder named `MyPaperServer`, allocate 8GB of RAM, and enable the GUI:
```
bash
java -jar MinecraftServerSetup.jar start Paper 1.20.4 true ./MyPaperServer 8 true
```
If you prefer an interactive setup:
```
bash
java -jar MinecraftServerSetup.jar start
```
The tool will then ask for the server software, version, EULA confirmation, path, memory, and GUI preference one by one.

## Dependencies

This project utilizes the following key libraries:

*   [Ktor](https://ktor.io/): For making HTTP requests to download server files.
*   [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization): For JSON parsing.
*   [KLI](https://github.com/kys0ff/kli): For creating the command-line interface.
*   [SLF4J (Simple Logger)](https://www.slf4j.org/): For logging.