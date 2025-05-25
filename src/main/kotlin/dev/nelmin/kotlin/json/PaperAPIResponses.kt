package dev.nelmin.kotlin.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaperVersionsResponse(
    @SerialName("project_id")
    val projectId: String,
    @SerialName("project_name")
    val projectName: String,
    val version: String = "",
    val versions: List<String> = listOf(),
    val builds: List<Int>?
) {
    fun getLatestBuild(): Int {
        if (builds!!.isEmpty()) return 1
        return builds.max()
    }
}