package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IptvDao {
    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE playlistUrl = :playlistUrl ORDER BY name ASC")
    fun getChannelsByPlaylist(playlistUrl: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteChannels(): Flow<List<Channel>>

    @Query("SELECT DISTINCT groupTitle FROM channels ORDER BY groupTitle ASC")
    fun getAllGroups(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE streamUrl = :streamUrl")
    suspend fun updateFavorite(streamUrl: String, isFavorite: Boolean)

    @Query("DELETE FROM channels WHERE playlistUrl = :playlistUrl")
    suspend fun deleteChannelsByPlaylist(playlistUrl: String)

    @Query("DELETE FROM channels")
    suspend fun clearAllChannels()

    // Playlist History queries
    @Query("SELECT * FROM playlist_history ORDER BY addedAt DESC")
    fun getPlaylistHistory(): Flow<List<PlaylistHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistHistory(history: PlaylistHistory)

    @Query("DELETE FROM playlist_history WHERE url = :url")
    suspend fun deletePlaylistHistoryByUrl(url: String)
}
