package com.example.musicalquiz.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton class that provides the Retrofit instance for making API calls to Deezer.
 * This class:
 * - Initializes Retrofit with the Deezer API base URL
 * - Configures Gson for JSON parsing
 * - Provides a lazy-initialized API interface instance
 * 
 * The singleton pattern ensures only one instance of the Retrofit client
 * is created throughout the application lifecycle, improving performance
 * and resource usage.
 */
object RetrofitInstance {
    /**
     * Base URL for the Deezer API.
     * All API endpoints will be appended to this base URL.
     */
    private const val BASE_URL = "https://api.deezer.com/"

    /**
     * Lazy-initialized Retrofit instance configured with:
     * - Deezer API base URL
     * - Gson converter for JSON parsing
     * 
     * The lazy initialization ensures the Retrofit instance is only created
     * when first accessed, improving startup performance.
     * 
     * @return Configured DeezerApiInterface instance
     */
    val api: DeezerApiInterface by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeezerApiInterface::class.java)
    }
}
