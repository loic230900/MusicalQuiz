package com.example.musicalquiz.model

/**
 * Generic class representing a response from the Deezer API.
 * @param T The type of objects returned in the list (Track, Album, etc.)
 */
data class DeezerSearchResponse<T>(
    val data: List<T>,
    val next: String? = null
)
