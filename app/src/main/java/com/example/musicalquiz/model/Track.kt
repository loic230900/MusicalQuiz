package com.example.musicalquiz.model

/**
 * Data class representing a music track in the application.
 * This class holds all essential information about a track retrieved from the Deezer API.
 * It includes metadata like title, duration, artist, and album information.
 *
 * @property id Unique track identifier from Deezer
 * @property title Track title
 * @property duration Track duration in seconds
 * @property artist Artist associated with the track
 * @property album Album the track belongs to
 * @property releaseDate Album release date in ISO 8601 format (nullable)
 * @property preview URL to 30-second mp3 preview (nullable)
 */
data class Track(
    val id: Long,
    val title: String,
    val duration: Int, // Duration in seconds
    val artist: Artist,
    val album: Album,
    val releaseDate: String?,
    val preview: String?
) {
    /**
     * Checks if the track title is the same as its album title.
     * This is useful for identifying single-track albums.
     *
     * @return true if the track title matches the album title (case-insensitive)
     */
    val isAlbum: Boolean
        get() = album.title.equals(title, ignoreCase = true)

    /**
     * Formats the track duration into a human-readable string.
     * Converts seconds into minutes:seconds format.
     *
     * @return Formatted duration string (e.g., "3:45")
     */
    fun getFormattedDuration(): String {
        val minutes = duration / 60
        val seconds = duration % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
