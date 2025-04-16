package com.example.musicalquiz.model

/**
 * Classe représentant la réponse de recherche de l’API Deezer.
 *
 * @property data Liste des pistes retournées par la recherche
 */
data class DeezerSearchResponse(
    val data: List<Track>
)
