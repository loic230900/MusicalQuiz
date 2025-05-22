package com.example.musicalquiz.model

/**
 * Represents a music album in the application.
 * This data class holds essential information about an album retrieved from the Deezer API.
 *
 * @property id Unique identifier for the album
 * @property title Name of the album
 * @property cover URL to the album's cover art image
 * @property artist The artist who created the album
 * @property releaseDate The album's release date in ISO 8601 format (nullable)
 */

data class Album(
    val id: Long,
    val title: String,
    val cover: String,
    val artist: Artist,
    val releaseDate: String? // ISO 8601, nullable
)