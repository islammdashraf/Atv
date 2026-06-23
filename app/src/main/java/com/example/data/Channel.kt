package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey val streamUrl: String,
    val name: String,
    val logoUrl: String,
    val groupTitle: String,
    val isFavorite: Boolean = false,
    val playlistUrl: String = ""
)
