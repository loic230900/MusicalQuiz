package com.example.musicalquiz.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicalquiz.model.Album
import com.example.musicalquiz.model.Track
import com.example.musicalquiz.network.RetrofitInstance
import kotlinx.coroutines.launch
import retrofit2.Response
import com.example.musicalquiz.model.DeezerSearchResponse

/**
 * ViewModel responsible for managing music search functionality and track data.
 * This class handles:
 * - Searching for tracks and albums via the Deezer API
 * - Managing search results with pagination support
 * - Filtering between tracks and albums
 * - Loading top charts
 * - Error handling and loading states
 * 
 * The ViewModel uses LiveData to notify observers of data changes and maintains
 * the current state of the search, including pagination information.
 * 
 * Pagination is implemented using a page-based approach where:
 * - Each page contains 25 items by default
 * - The next page URL is provided by the API
 * - Results are accumulated when loading more items
 * - Loading state is tracked to prevent duplicate requests
 */
class TracksViewModel : ViewModel() {
    private val _tracksLiveData = MutableLiveData<List<Track>>()
    val tracksLiveData: LiveData<List<Track>> = _tracksLiveData

    private val _albumsLiveData = MutableLiveData<List<Album>>()
    val albumsLiveData: LiveData<List<Album>> = _albumsLiveData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var lastQuery: String? = null
    private var currentFilter: SearchFilter = SearchFilter.TRACKS
    
    // Pagination properties
    private var currentPage = 0
    private var hasMoreResults = true
    private var isLoadingMore = false

    enum class SearchFilter {
        TRACKS, ALBUMS
    }

    fun setFilter(filter: SearchFilter) {
        currentFilter = filter
        resetPagination()
        lastQuery?.let { searchAll(it) }
    }

    private fun resetPagination() {
        currentPage = 0
        hasMoreResults = true
        isLoadingMore = false
    }

    private fun <T> handleResponse(
        response: Response<DeezerSearchResponse<T>>,
        onSuccess: (List<T>) -> Unit
    ) {
        if (response.isSuccessful) {
            val data = response.body()?.data ?: emptyList()
            hasMoreResults = data.isNotEmpty() && response.body()?.next != null
            onSuccess(data)
        } else {
            _error.value = "API Error: ${response.code()}"
        }
    }

    /**
     * Performs a search for tracks and albums via the Deezer API.
     * Results are filtered based on the current filter and displayed in a single list.
     * Supports pagination for loading more results.
     * 
     * @param query The search term entered by the user
     * @param loadMore Whether to load more results (true) or start a new search (false)
     */
    fun searchAll(query: String, loadMore: Boolean = false) {
        if (loadMore && (!hasMoreResults || isLoadingMore)) return
        
        if (!loadMore) {
            lastQuery = query
            resetPagination()
        }
        
        viewModelScope.launch {
            if (!loadMore) {
                _isLoading.value = true
            }
            isLoadingMore = true
            _error.value = null
            
            try {
                when (currentFilter) {
                    SearchFilter.TRACKS -> {
                        val response = RetrofitInstance.api.searchTracks(query, currentPage * 25)
                        handleResponse(response) { tracks ->
                            if (loadMore) {
                                _tracksLiveData.value = (_tracksLiveData.value ?: emptyList()) + tracks
                            } else {
                                _tracksLiveData.value = tracks
                            }
                            if (hasMoreResults) currentPage++
                        }
                    }
                    SearchFilter.ALBUMS -> {
                        val response = RetrofitInstance.api.searchAlbums(query, currentPage * 25)
                        handleResponse(response) { albums ->
                            if (loadMore) {
                                _albumsLiveData.value = (_albumsLiveData.value ?: emptyList()) + albums
                            } else {
                                _albumsLiveData.value = albums
                            }
                            if (hasMoreResults) currentPage++
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
                isLoadingMore = false
            }
        }
    }

    /**
     * Loads the next page of results if available.
     * This method is called when the user scrolls to the bottom of the list.
     */
    fun loadMoreResults() {
        lastQuery?.let { searchAll(it, true) }
    }

    /**
     * Loads the most popular tracks and albums from the Deezer API.
     * Used to display initial content and after an empty search.
     */
    fun loadTopCharts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                when (currentFilter) {
                    SearchFilter.TRACKS -> {
                        val response = RetrofitInstance.api.getTopTracks()
                        handleResponse(response) { tracks ->
                            _tracksLiveData.value = tracks
                        }
                    }
                    SearchFilter.ALBUMS -> {
                        val response = RetrofitInstance.api.getTopAlbums()
                        handleResponse(response) { albums ->
                            _albumsLiveData.value = albums
                        }
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
     * Reloads the last search or top charts if no search was performed.
     * Useful for restoring state after configuration changes.
     */
    fun reloadLastSearch() {
        lastQuery?.let { searchAll(it) } ?: loadTopCharts()
    }

    /**
     * Fetches tracks for a specific album from the Deezer API.
     * @param albumId The ID of the album to fetch tracks for
     * @return A list of tracks associated with the album
     */
    suspend fun getAlbumTracks(albumId: Long): List<Track> {
        return try {
            val response = RetrofitInstance.api.getAlbumTracks(albumId)
            if (response.isSuccessful) {
                response.body()?.data ?: emptyList()
            } else {
                _error.value = "Error fetching album tracks: ${response.code()}"
                emptyList()
            }
        } catch (e: Exception) {
            _error.value = e.message
            emptyList()
        }
    }
}
