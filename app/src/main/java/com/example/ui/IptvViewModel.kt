package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Channel
import com.example.data.IptvRepository
import com.example.data.PlaylistHistory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IptvViewModel(private val repository: IptvRepository) : ViewModel() {

    private val _playlistUrl = MutableStateFlow("https://iptv-org.github.io/iptv/countries/bd.m3u")
    val playlistUrl: StateFlow<String> = _playlistUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGroup = MutableStateFlow("All")
    val selectedGroup: StateFlow<String> = _selectedGroup.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    private val _activeChannel = MutableStateFlow<Channel?>(null)
    val activeChannel: StateFlow<Channel?> = _activeChannel.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlistHistory: StateFlow<List<PlaylistHistory>> = repository.getPlaylistHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamically retrieve groups from parsed channels and filter/normalize them
    @OptIn(ExperimentalCoroutinesApi::class)
    val groups: StateFlow<List<String>> = _playlistUrl
        .flatMapLatest { url ->
            if (url.isBlank()) {
                repository.getAllChannels()
            } else {
                repository.getChannelsByPlaylist(url).flatMapLatest { list ->
                    if (list.isEmpty()) {
                        repository.getAllChannels()
                    } else {
                        flowOf(list)
                    }
                }
            }
        }
        .map { list ->
            val normalized = list.map { getNormalizedGroup(it) }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            listOf("All") + normalized
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf("All"))

    init {
        // Automatically load Bangladesh live TV IPTV stream list
        loadPlaylist("https://iptv-org.github.io/iptv/countries/bd.m3u")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val channels: StateFlow<List<Channel>> = combine(
        _playlistUrl.flatMapLatest { url ->
            if (url.isBlank()) {
                repository.getAllChannels()
            } else {
                repository.getChannelsByPlaylist(url).flatMapLatest { list ->
                    if (list.isEmpty()) {
                        repository.getAllChannels()
                    } else {
                        flowOf(list)
                    }
                }
            }
        },
        _searchQuery,
        _selectedGroup,
        _showFavoritesOnly
    ) { rawChannels, query, group, favsOnly ->
        var filtered = rawChannels
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }
        if (group != "All") {
            filtered = filtered.filter { getNormalizedGroup(it) == group }
        }
        if (favsOnly) {
            filtered = filtered.filter { it.isFavorite }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun getNormalizedGroup(channel: Channel): String {
        val group = channel.groupTitle.trim()
        val name = channel.name
        val playlist = channel.playlistUrl

        // 1. Check if it is a Sport channel (by name, groupTitle, or playlist origin)
        if (group.contains("sport", ignoreCase = true) || 
            group.contains("sports", ignoreCase = true) || 
            name.contains("sport", ignoreCase = true) || 
            name.contains("sports", ignoreCase = true) || 
            name.contains("cricket", ignoreCase = true) || 
            name.contains("football", ignoreCase = true) || 
            playlist.contains("sports.m3u", ignoreCase = true)) {
            return "Sports"
        }

        // 2. Check if it is a News channel 
        if (group.contains("news", ignoreCase = true) || 
            name.contains("news", ignoreCase = true) || 
            playlist.contains("/us.m3u", ignoreCase = true)) {
            return "News"
        }

        // 3. Other cleanups & category normalization
        return when {
            group.isNotBlank() && group.contains("music", ignoreCase = true) -> "Music"
            group.isNotBlank() && group.contains("movie", ignoreCase = true) -> "Movies"
            group.isNotBlank() && (group.contains("kids", ignoreCase = true) || group.contains("animation", ignoreCase = true)) -> "Kids"
            group.isNotBlank() && group.contains("entertainment", ignoreCase = true) -> "Entertainment"
            group.isNotBlank() && (group.contains("religious", ignoreCase = true) || group.contains("islam", ignoreCase = true)) -> "Religious"
            group.isNotBlank() && group.replaceFirstChar { it.lowercase() }.startsWith("bd") -> "BD TV"
            group.isBlank() || group.equals("General", ignoreCase = true) -> "General"
            else -> group.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }
        }
    }

    fun setPlaylistUrl(url: String) {
        _playlistUrl.value = url
        _errorMessage.value = null
    }

    fun loadPlaylist(url: String) {
        if (url.isBlank()) {
            _errorMessage.value = "Playlist URL cannot be empty."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            // Reset active search filters and group selectors when loading a new playlist
            _searchQuery.value = ""
            _selectedGroup.value = "All"
            _showFavoritesOnly.value = false
            
            // Instantly transition to the target playlist URL so that cached channels
            // already in the Room DB load immediately.
            _playlistUrl.value = url
            
            try {
                repository.fetchAndParsePlaylist(url)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to load/parse playlist"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectChannel(channel: Channel?) {
        _activeChannel.value = channel
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.streamUrl, !channel.isFavorite)
            // If the currently playing channel is the one toggle-favorited, keep current state active with updated favoriting
            if (_activeChannel.value?.streamUrl == channel.streamUrl) {
                _activeChannel.value = _activeChannel.value?.copy(isFavorite = !channel.isFavorite)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGroupFilter(group: String) {
        _selectedGroup.value = group
    }

    fun toggleFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun clearHistoryUrl(url: String) {
        viewModelScope.launch {
            repository.deletePlaylistHistory(url)
            if (_playlistUrl.value == url) {
                _playlistUrl.value = ""
                _activeChannel.value = null
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class IptvViewModelFactory(private val repository: IptvRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IptvViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IptvViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
