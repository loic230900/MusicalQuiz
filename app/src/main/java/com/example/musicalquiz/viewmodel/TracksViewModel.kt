package com.example.musicalquiz.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicalquiz.model.DeezerSearchResponse
import com.example.musicalquiz.network.RetrofitInstance
import kotlinx.coroutines.launch

/**
 * ViewModel responsable de la logique de recherche de pistes.
 * Il conserve les résultats et les expose à l’UI via LiveData.
 */
class TracksViewModel : ViewModel() {

    // Données observables : liste de pistes retournées par l’API
    val tracksResponse = MutableLiveData<DeezerSearchResponse>()

    // États supplémentaires : chargement et erreur
    val isLoading = MutableLiveData<Boolean>()
    val error = MutableLiveData<String?>()

    /**
     * Lance une recherche de pistes en appelant l’API Deezer via Retrofit.
     *
     * @param query Le mot-clé à rechercher
     */
    fun searchTracks(query: String) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                val response = RetrofitInstance.api.searchTracks(query)
                tracksResponse.value = response
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }
}
