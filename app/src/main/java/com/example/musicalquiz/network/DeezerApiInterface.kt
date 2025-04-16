package com.example.musicalquiz.network

import com.example.musicalquiz.model.DeezerSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface décrivant les endpoints de l’API Deezer utilisés par l’application.
 */
interface DeezerApiInterface {

    /**
     * Recherche des pistes musicales selon une requête textuelle.
     *
     * @param query Chaîne de recherche saisie par l’utilisateur
     * @return Réponse contenant une liste de pistes
     */
    @GET("search")
    suspend fun searchTracks(@Query("q") query: String): DeezerSearchResponse

}
