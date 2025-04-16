package com.example.musicalquiz.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton permettant d'initialiser Retrofit avec l'URL de base de l’API Deezer.
 */
object RetrofitInstance {

    // URL de base de l’API Deezer
    private const val BASE_URL = "https://api.deezer.com/"

    /**
     * Objet Retrofit configuré avec Gson.
     */
    val api: DeezerApiInterface by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeezerApiInterface::class.java)
    }
}
