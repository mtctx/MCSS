package dev.nelmin.kotlin

import dev.nelmin.kotlin.json.PaperProjectResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import dev.nelmin.kotlin.json.PaperVersionResponse
import dev.nelmin.kotlin.json.PurpurProjectResponse

object VersionFetcher {

    fun paper(): List<String> = runBlocking {
        val response = httpClient.get("https://api.papermc.io/v2/projects/paper/").body<PaperProjectResponse>()
        response.versions
    }

    fun velocity(): List<String> = runBlocking {
        val response = httpClient.get("https://api.papermc.io/v2/projects/velocity/").body<PaperProjectResponse>()
        response.versions
    }

    fun purpur(): List<String> = runBlocking {
        val response = httpClient.get("https://api.purpurmc.org/v2/purpur/").body<PurpurProjectResponse>()
        response.versions
    }

}