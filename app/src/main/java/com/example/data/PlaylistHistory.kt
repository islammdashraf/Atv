package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_history")
data class PlaylistHistory(
    @PrimaryKey val url: String,
    val name: String,
    val addedAt: Long = System.currentTimeMillis()
)
