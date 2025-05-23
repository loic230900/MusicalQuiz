package com.example.musicalquiz.model

/**
 * Generic class representing a response from the Deezer API.
 * This class encapsulates:
 * - The list of items returned by the API (tracks, albums, etc.)
 * - Pagination information through the next URL
 * 
 * @param T The type of objects returned in the list (Track, Album, etc.)
 * @property data The list of items returned by the API
 * @property next URL for the next page of results, null if no more results
 */
data class DeezerSearchResponse<T>(
    val data: List<T>,
    val next: String? = null
)
