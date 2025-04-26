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
    private var currentFilter: SearchFilter = SearchFilter.TRACKS

    enum class SearchFilter {
        TRACKS, ALBUMS
    }

    fun setFilter(filter: SearchFilter) {
        currentFilter = filter
        lastQuery?.let { searchAll(it) }
    }

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
                when (currentFilter) {
                    SearchFilter.TRACKS -> {
                        val tracks = RetrofitInstance.api.searchTracks(query).data
                        _itemsLiveData.value = tracks.map { SearchResultItem.TrackItem(it) }
                    }
                    SearchFilter.ALBUMS -> {
                        val albums = RetrofitInstance.api.searchAlbums(query).data
                        _itemsLiveData.value = albums.map { SearchResultItem.AlbumItem(it) }
                    }
                }
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
                when (currentFilter) {
                    SearchFilter.TRACKS -> {
                        val tracks = RetrofitInstance.api.getTopTracks().data
                        _itemsLiveData.value = tracks.map { SearchResultItem.TrackItem(it) }
                    }
                    SearchFilter.ALBUMS -> {
                        val albums = RetrofitInstance.api.getTopAlbums().data
                        _itemsLiveData.value = albums.map { SearchResultItem.AlbumItem(it) }
                    }
                }
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
