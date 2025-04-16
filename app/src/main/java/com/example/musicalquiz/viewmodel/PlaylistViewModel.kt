package com.example.musicalquiz.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicalquiz.database.entities.Playlist
import kotlinx.coroutines.launch

/**
 * ViewModel responsable de la gestion des playlists.
 * DAO et logique Room à intégrer à l'étape 6.
 */
class PlaylistViewModel : ViewModel() {

    // Données observables (mockées ou vides pour l’instant)
    val playlists = MutableLiveData<List<Playlist>>(emptyList())

    // Exemple de structure préparée
    fun getAllPlaylists() {
        viewModelScope.launch {
            // À implémenter avec Room
        }
    }

    fun insertPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            // À implémenter plus tard
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            // À implémenter plus tard
        }
    }
}
