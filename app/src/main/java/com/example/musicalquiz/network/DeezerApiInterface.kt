package com.example.musicalquiz.network

import com.example.musicalquiz.model.Album
import com.example.musicalquiz.model.DeezerSearchResponse
import com.example.musicalquiz.model.Track
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interface describing the Deezer API endpoints used by the application.
 */
interface DeezerApiInterface {

    /**
     * Search for music tracks based on a text query.
     *
     * @param query Search string entered by the user
     * @param index Starting index for pagination (default: 0)
     * @param limit Number of items per page (default: 25)
     * @return Response containing a list of tracks
     */
    @GET("search")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("index") index: Int = 0,
        @Query("limit") limit: Int = 25
    ): Response<DeezerSearchResponse<Track>>

    /**
     * Search for music albums based on a text query.
     *
     * @param query Search string entered by the user
     * @param index Starting index for pagination (default: 0)
     * @param limit Number of items per page (default: 25)
     * @return Response containing a list of albums
     */
    @GET("search/album")
    suspend fun searchAlbums(
        @Query("q") query: String,
        @Query("index") index: Int = 0,
        @Query("limit") limit: Int = 25
    ): Response<DeezerSearchResponse<Album>>

    /**
     * Get the top tracks from the Deezer charts.
     *
     * @param index Starting index for pagination (default: 0)
     * @param limit Number of items per page (default: 25)
     * @return Response containing a list of top tracks
     */
    @GET("chart/0/tracks")
    suspend fun getTopTracks(
        @Query("index") index: Int = 0,
        @Query("limit") limit: Int = 25
    ): Response<DeezerSearchResponse<Track>>

    /**
     * Get the top albums from the Deezer charts.
     *
     * @param index Starting index for pagination (default: 0)
     * @param limit Number of items per page (default: 25)
     * @return Response containing a list of top albums
     */
    @GET("chart/0/albums")
    suspend fun getTopAlbums(
        @Query("index") index: Int = 0,
        @Query("limit") limit: Int = 25
    ): Response<DeezerSearchResponse<Album>>

    /**
     * Get details for a specific track.
     *
     * @param trackId ID of the track to fetch
     * @return Response containing track details
     */
    @GET("track/{trackId}")
    suspend fun getTrack(@Path("trackId") trackId: Long): Response<Track>

    /**
     * Get details for a specific album.
     *
     * @param albumId ID of the album to fetch
     * @return Response containing album details
     */
    @GET("album/{albumId}")
    suspend fun getAlbum(@Path("albumId") albumId: Long): Response<Album>

    /**
     * Get all tracks from a specific album.
     *
     * @param albumId ID of the album to fetch tracks from
     * @return Response containing a list of tracks
     */
    @GET("album/{albumId}/tracks")
    suspend fun getAlbumTracks(@Path("albumId") albumId: Long): Response<DeezerSearchResponse<Track>>
}
