package com.example.musicalquiz.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a playlist in the local database.
 * @property id Unique playlist identifier (auto-generated)
 * @property name Playlist name
 * @property createdAt Timestamp when the playlist was created
 */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
