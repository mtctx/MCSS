package dev.nelmin.kotlin.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaperVersionResponse(
    @SerialName("project_id")
    val projectId: String,
    @SerialName("project_name")
    val projectName: String,
    val version: String,
    val builds: List<Int>
) {
    fun getLatestBuild(): Int {
        if (builds.isEmpty()) return 1
        return builds.max()
    }
}

@Serializable
data class PaperProjectResponse(
    @SerialName("project_id")
    val projectId: String,
    @SerialName("project_name")
    val projectName: String,
    @SerialName("version_groups")
    val versionGroups: List<String> = listOf(),
    val versions: List<String> = listOf(),
)