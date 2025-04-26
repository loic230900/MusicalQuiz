package com.example.musicalquiz.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicalquiz.model.SearchResultItem
import com.example.musicalquiz.network.RetrofitInstance
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * ViewModel responsable de la gestion des données de recherche musicale.
 * Gère les requêtes vers l'API Deezer et maintient l'état des résultats de recherche.
 * Utilise LiveData pour notifier la vue des changements d'état.
 */
class TracksViewModel : ViewModel() {
    private val _itemsLiveData = MutableLiveData<List<SearchResultItem>>()
    val itemsLiveData: LiveData<List<SearchResultItem>> = _itemsLiveData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var lastQuery: String? = null

    /**
     * Effectue une recherche de morceaux et d'albums via l'API Deezer.
     * Les résultats sont mélangés et affichés dans une liste unique.
     * @param query Le terme de recherche saisi par l'utilisateur
     */
    fun searchAll(query: String) {
        lastQuery = query
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val tracks = async { RetrofitInstance.api.searchTracks(query).data }
                val albums = async { RetrofitInstance.api.searchAlbums(query).data }

                val trackItems = tracks.await().map { SearchResultItem.TrackItem(it) }
                val albumItems = albums.await().map { SearchResultItem.AlbumItem(it) }

                _itemsLiveData.value = (trackItems + albumItems).shuffled()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Charge les morceaux et albums les plus populaires depuis l'API Deezer.
     * Utilisé pour afficher le contenu initial et après une recherche vide.
     */
    fun loadTopCharts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val topTracks = async { RetrofitInstance.api.getTopTracks().data }
                val topAlbums = async { RetrofitInstance.api.getTopAlbums().data }

                val tracks = topTracks.await().map { SearchResultItem.TrackItem(it) }
                val albums = topAlbums.await().map { SearchResultItem.AlbumItem(it) }

                _itemsLiveData.value = (tracks + albums).shuffled()
                lastQuery = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Recharge la dernière recherche effectuée ou les top charts si aucune recherche n'a été faite.
     * Utile pour restaurer l'état après un changement de configuration.
     */
    fun reloadLastSearch() {
        lastQuery?.let { searchAll(it) } ?: loadTopCharts()
    }
}
