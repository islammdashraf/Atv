package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class IptvRepository(private val iptvDao: IptvDao) {

    fun getAllChannels(): Flow<List<Channel>> =
        iptvDao.getAllChannels()

    fun getChannelsByPlaylist(playlistUrl: String): Flow<List<Channel>> =
        iptvDao.getChannelsByPlaylist(playlistUrl)

    fun getFavoriteChannels(): Flow<List<Channel>> =
        iptvDao.getFavoriteChannels()

    fun getAllGroups(): Flow<List<String>> =
        iptvDao.getAllGroups()

    fun getPlaylistHistory(): Flow<List<PlaylistHistory>> =
        iptvDao.getPlaylistHistory()

    suspend fun toggleFavorite(streamUrl: String, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            iptvDao.updateFavorite(streamUrl, isFavorite)
        }
    }

    suspend fun clearPlaylist(playlistUrl: String) {
        withContext(Dispatchers.IO) {
            iptvDao.deleteChannelsByPlaylist(playlistUrl)
        }
    }

    suspend fun deletePlaylistHistory(url: String) {
        withContext(Dispatchers.IO) {
            iptvDao.deletePlaylistHistoryByUrl(url)
            iptvDao.deleteChannelsByPlaylist(url)
        }
    }

    suspend fun fetchAndParsePlaylist(url: String): List<Channel> = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        
        val request = Request.Builder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch M3U: HTTP ${response.code} ${response.message}")
            }
            val bodyString = response.body?.string() ?: throw Exception("Empty response body from M3U link")
            val channels = parseM3u(url, bodyString)
            if (channels.isEmpty()) {
                throw Exception("Invalid parse content. No audio/video streaming feeds starting with 'http' found format EXTM3U.")
            }
            
            // Core database actions
            iptvDao.deleteChannelsByPlaylist(url)
            iptvDao.insertChannels(channels)
            
            val filename = url.substringAfterLast("/").substringBefore("?").ifEmpty { "Playlist" }
            val cleanName = if (filename.contains(".")) filename.substringBeforeLast(".") else filename
            val friendlyName = cleanName.ifEmpty { "My IPTV Playlist" }
            
            iptvDao.insertPlaylistHistory(
                PlaylistHistory(
                    url = url,
                    name = friendlyName,
                    addedAt = System.currentTimeMillis()
                )
            )
            
            channels
        }
    }

    private fun parseM3u(playlistUrl: String, content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        var currentInfo: ChannelInfo? = null
        
        content.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                if (trimmed.startsWith("#EXTINF:")) {
                    currentInfo = parseExtInfLine(trimmed)
                } else if (!trimmed.startsWith("#")) {
                    // This is the actual stream URL! Must begin with http or https schema
                    if (trimmed.startsWith("http://", ignoreCase = true) || 
                        trimmed.startsWith("https://", ignoreCase = true) ||
                        trimmed.startsWith("rtmp://", ignoreCase = true) ||
                        trimmed.startsWith("rtsp://", ignoreCase = true)) {
                        
                        val logo = currentInfo?.logoUrl ?: ""
                        val group = currentInfo?.groupTitle ?: "General"
                        val titleName = currentInfo?.name ?: trimmed.substringAfterLast("/")
                        
                        channels.add(
                            Channel(
                                streamUrl = trimmed,
                                name = titleName,
                                logoUrl = logo,
                                groupTitle = group,
                                isFavorite = false,
                                playlistUrl = playlistUrl
                            )
                        )
                    }
                    currentInfo = null
                }
            }
        }
        return channels
    }

    private data class ChannelInfo(val name: String, val logoUrl: String, val groupTitle: String)

    private fun parseExtInfLine(line: String): ChannelInfo {
        // Parse tvg-logo
        val logoSearch = "tvg-logo=\""
        val logoUrl = if (line.contains(logoSearch)) {
            val startIndex = line.indexOf(logoSearch) + logoSearch.length
            val endIndex = line.indexOf("\"", startIndex)
            if (endIndex > startIndex) line.substring(startIndex, endIndex) else ""
        } else ""

        // Parse group-title
        val groupSearch = "group-title=\""
        val groupTitle = if (line.contains(groupSearch)) {
            val startIndex = line.indexOf(groupSearch) + groupSearch.length
            val endIndex = line.indexOf("\"", startIndex)
            if (endIndex > startIndex) line.substring(startIndex, endIndex) else ""
        } else "General"

        // Channel name follows after the very last ',' characters
        val rawName = line.substringAfterLast(",").trim()
        val finalName = if (rawName.isEmpty()) {
            val nameSearch = "tvg-name=\""
            if (line.contains(nameSearch)) {
                val startIndex = line.indexOf(nameSearch) + nameSearch.length
                val endIndex = line.indexOf("\"", startIndex)
                if (endIndex > startIndex) line.substring(startIndex, endIndex) else "Unnamed Live Channel"
            } else {
                "Unnamed Live Channel"
            }
        } else {
            rawName
        }

        return ChannelInfo(name = finalName, logoUrl = logoUrl, groupTitle = groupTitle)
    }
}
