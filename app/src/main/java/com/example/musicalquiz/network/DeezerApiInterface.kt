package com.example.musicalquiz.network

import com.example.musicalquiz.model.Album
import com.example.musicalquiz.model.DeezerSearchResponse
import com.example.musicalquiz.model.Track
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
     * @return Réponse contenant une liste
     */

    @GET("search")
    suspend fun searchTracks(@Query("q") query: String): DeezerSearchResponse<Track>

    @GET("search/album")
    suspend fun searchAlbums(@Query("q") query: String): DeezerSearchResponse<Album>

    @GET("chart/0/tracks")
    suspend fun getTopTracks(): DeezerSearchResponse<Track>

    @GET("chart/0/albums")
    suspend fun getTopAlbums(): DeezerSearchResponse<Album>


}
