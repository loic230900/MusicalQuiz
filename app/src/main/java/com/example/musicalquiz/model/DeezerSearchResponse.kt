package com.example.musicalquiz.model

/**
 * Classe générique représentant une réponse de l’API Deezer.
 * @param T Le type des objets retournés dans la liste (Track, Album, etc.)
 */
data class DeezerSearchResponse<T>(
    val data: List<T>
)
