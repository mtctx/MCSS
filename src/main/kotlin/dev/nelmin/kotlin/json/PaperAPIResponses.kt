@file:OptIn(ExperimentalSerializationApi::class)

package dev.nelmin.kotlin.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@JsonIgnoreUnknownKeys
@Serializable
data class PaperVersionResponse(
    val builds: List<Int>
) {
    fun getLatestBuild(): Int {
        if (builds.isEmpty()) return 1
        return builds.max()
    }
}

@JsonIgnoreUnknownKeys
@Serializable
data class PaperProjectResponse(
    val versions: List<String> = listOf(),
)