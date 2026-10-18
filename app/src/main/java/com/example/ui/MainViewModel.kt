package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.M3uParser
import com.example.data.repository.StorageRepository
import com.example.data.repository.XtreamApiClient
import com.example.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID

enum class AppScreen {
    LOGIN,
    MAIN,
    MEDIA_DETAIL,
    PLAYER
}

enum class MainTab {
    HOME,
    LIVE,
    MOVIES,
    SERIES,
    SETTINGS
}

data class UiState(
    val screen: AppScreen = AppScreen.MAIN,
    val currentTab: MainTab = MainTab.HOME,
    val activeProfile: SourceProfile? = null,
    val profiles: List<SourceProfile> = emptyList(),
    val userInfo: UserInfo = UserInfo(),
    val serverInfo: ServerInfo = ServerInfo(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,

    // Media collections
    val liveChannels: List<MediaItem> = emptyList(),
    val liveCategories: List<CategoryItem> = emptyList(),
    val movies: List<MediaItem> = emptyList(),
    val movieCategories: List<CategoryItem> = emptyList(),
    val seriesList: List<MediaItem> = emptyList(),
    val seriesCategories: List<CategoryItem> = emptyList(),

    // User collections
    val favorites: List<MediaItem> = emptyList(),
    val continueWatching: List<WatchHistoryItem> = emptyList(),

    // Detail Screen
    val selectedMedia: MediaItem? = null,
    val selectedSeriesDetail: SeriesDetail? = null,
    val selectedSeason: Int = 1,

    // Playback state
    val playingStreamUrl: String = "",
    val playingTitle: String = "",
    val playingStreamId: String = "",
    val playingStreamType: StreamType = StreamType.VOD,
    val playingPosterUrl: String = "",
    val playingStartPosMs: Long = 0L,

    // Search & Filter state
    val searchQuery: String = "",
    val selectedLiveCategoryId: String = "",
    val selectedMovieCategoryId: String = "",
    val selectedSeriesCategoryId: String = "",

    // Parental PIN dialog
    val isPinPromptVisible: Boolean = false,
    val pendingUnlockedCategory: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = StorageRepository(application)
    private val xtreamClient = XtreamApiClient()
    private val okHttpClient = OkHttpClient()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val settings = storage.getSettings()
        val profiles = storage.getProfiles()
        val activeProfile = storage.getActiveProfile()
        val favorites = storage.getFavorites()
        val history = storage.getWatchHistory()

        _uiState.value = _uiState.value.copy(
            settings = settings,
            profiles = profiles,
            activeProfile = activeProfile,
            favorites = favorites,
            continueWatching = history,
            screen = if (activeProfile != null) AppScreen.MAIN else AppScreen.LOGIN
        )

        if (activeProfile != null) {
            syncActiveProfile(activeProfile)
        }
    }

    fun syncActiveProfile(profile: SourceProfile) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                if (profile.isDemo) {
                    loadDemoData()
                } else if (profile.type == "XTREAM") {
                    loadXtreamData(profile)
                } else if (profile.type == "M3U") {
                    loadM3uData(profile)
                }
            } catch (e: Exception) {
                val errorMsg = Strings.get("error_timeout", _uiState.value.settings.language) + ": ${e.localizedMessage}"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
            }
        }
    }

    private fun loadDemoData() {
        val liveChannels = storage.getSampleLiveChannels()
        val movies = storage.getSampleMovies()
        val series = storage.getSampleSeries()

        val liveCats = listOf(
            CategoryItem("all", Strings.get("all_categories", _uiState.value.settings.language), StreamType.LIVE),
            CategoryItem("news", "أخبار (News)", StreamType.LIVE),
            CategoryItem("sports", "رياضة (Sports)", StreamType.LIVE),
            CategoryItem("docs", "وثائقي (Docs)", StreamType.LIVE)
        )

        val movieCats = listOf(
            CategoryItem("all", Strings.get("all_categories", _uiState.value.settings.language), StreamType.VOD),
            CategoryItem("action", "أكشن وإثارة", StreamType.VOD),
            CategoryItem("animation", "رسوم متحركة", StreamType.VOD),
            CategoryItem("scifi", "خيال علمي", StreamType.VOD),
            CategoryItem("fantasy", "فانتازيا", StreamType.VOD)
        )

        val seriesCats = listOf(
            CategoryItem("all", Strings.get("all_categories", _uiState.value.settings.language), StreamType.SERIES),
            CategoryItem("drama", "دراما", StreamType.SERIES),
            CategoryItem("scifi", "خيال علمي", StreamType.SERIES)
        )

        val demoUser = UserInfo(
            username = "demo_vip_guest",
            status = "Active (Demo VIP)",
            expDate = "2030-12-31 23:59:59",
            activeCons = "1",
            maxConnections = "5",
            isTrial = true
        )

        val demoServer = ServerInfo(
            url = "http://demo.dridiflix.com",
            port = "8080",
            serverProtocol = "http",
            timezone = "Europe/Paris"
        )

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            liveChannels = liveChannels,
            liveCategories = liveCats,
            movies = movies,
            movieCategories = movieCats,
            seriesList = series,
            seriesCategories = seriesCats,
            userInfo = demoUser,
            serverInfo = demoServer
        )
    }

    private suspend fun loadXtreamData(profile: SourceProfile) {
        val proxy = _uiState.value.settings.corsProxyUrl

        // Authenticate
        val (user, server) = xtreamClient.authenticate(
            serverUrl = profile.serverUrl,
            user = profile.username,
            pass = profile.password,
            corsProxy = proxy
        )

        // Fetch categories & initial streams
        val liveCats = xtreamClient.getLiveCategories(profile.serverUrl, profile.username, profile.password, proxy)
        val vodCats = xtreamClient.getVodCategories(profile.serverUrl, profile.username, profile.password, proxy)
        val seriesCats = xtreamClient.getSeriesCategories(profile.serverUrl, profile.username, profile.password, proxy)

        val liveStreams = xtreamClient.getLiveStreams(profile.serverUrl, profile.username, profile.password, corsProxy = proxy)
        val vodStreams = xtreamClient.getVodStreams(profile.serverUrl, profile.username, profile.password, corsProxy = proxy)
        val seriesList = xtreamClient.getSeries(profile.serverUrl, profile.username, profile.password, corsProxy = proxy)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            userInfo = user,
            serverInfo = server,
            liveChannels = liveStreams,
            liveCategories = liveCats,
            movies = vodStreams,
            movieCategories = vodCats,
            seriesList = seriesList,
            seriesCategories = seriesCats
        )
    }

    private suspend fun loadM3uData(profile: SourceProfile) {
        val content = if (profile.m3uContent.isNotBlank()) {
            profile.m3uContent
        } else if (profile.m3uUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                val req = Request.Builder().url(profile.m3uUrl).build()
                okHttpClient.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) throw Exception("HTTP ${res.code}")
                    res.body?.string() ?: ""
                }
            }
        } else {
            throw Exception(Strings.get("error_empty_playlist", _uiState.value.settings.language))
        }

        val parsed = withContext(Dispatchers.Default) {
            M3uParser.parse(content)
        }

        val live = parsed.items.filter { it.streamType == StreamType.LIVE }
        val vod = parsed.items.filter { it.streamType == StreamType.VOD }
        val series = parsed.items.filter { it.streamType == StreamType.SERIES }

        val liveCats = parsed.categories.filter { it.streamType == StreamType.LIVE }
        val movieCats = parsed.categories.filter { it.streamType == StreamType.VOD }
        val seriesCats = parsed.categories.filter { it.streamType == StreamType.SERIES }

        val m3uUser = UserInfo(
            username = profile.name,
            status = "Active (M3U)",
            expDate = "Unlimited",
            activeCons = "1",
            maxConnections = "Unlimited"
        )

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            userInfo = m3uUser,
            liveChannels = live,
            liveCategories = liveCats,
            movies = vod,
            movieCategories = movieCats,
            seriesList = series,
            seriesCategories = seriesCats
        )
    }

    fun loginXtream(serverUrl: String, user: String, pass: String, name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val proxy = _uiState.value.settings.corsProxyUrl
                val (userInfo, serverInfo) = xtreamClient.authenticate(serverUrl, user, pass, proxy)

                val profile = SourceProfile(
                    id = UUID.randomUUID().toString(),
                    name = name.ifBlank { "Xtream ($user)" },
                    type = "XTREAM",
                    serverUrl = serverUrl,
                    username = user,
                    password = pass
                )
                storage.saveProfile(profile)
                storage.setActiveProfileId(profile.id)

                val updatedProfiles = storage.getProfiles()
                _uiState.value = _uiState.value.copy(
                    profiles = updatedProfiles,
                    activeProfile = profile,
                    userInfo = userInfo,
                    serverInfo = serverInfo,
                    screen = AppScreen.MAIN
                )
                syncActiveProfile(profile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = Strings.get("error_wrong_credentials", _uiState.value.settings.language) + " (${e.localizedMessage})"
                )
            }
        }
    }

    fun loginM3u(url: String, content: String, name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val profile = SourceProfile(
                    id = UUID.randomUUID().toString(),
                    name = name.ifBlank { "M3U Playlist" },
                    type = "M3U",
                    m3uUrl = url,
                    m3uContent = content
                )
                storage.saveProfile(profile)
                storage.setActiveProfileId(profile.id)

                val updatedProfiles = storage.getProfiles()
                _uiState.value = _uiState.value.copy(
                    profiles = updatedProfiles,
                    activeProfile = profile,
                    screen = AppScreen.MAIN
                )
                syncActiveProfile(profile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = Strings.get("error_empty_playlist", _uiState.value.settings.language) + " (${e.localizedMessage})"
                )
            }
        }
    }

    fun loginDemo() {
        val demoProfile = storage.getProfiles().find { it.isDemo } ?: SourceProfile(
            id = "demo_profile_default",
            name = "DRIDI FLIX Demo (سيرفر تجريبي مجاني)",
            type = "XTREAM",
            serverUrl = "http://demo.dridiflix.com:8080",
            username = "demo_user",
            password = "demo_password",
            isDemo = true
        )
        storage.saveProfile(demoProfile)
        storage.setActiveProfileId(demoProfile.id)
        _uiState.value = _uiState.value.copy(
            activeProfile = demoProfile,
            profiles = storage.getProfiles(),
            screen = AppScreen.MAIN
        )
        syncActiveProfile(demoProfile)
    }

    fun switchProfile(profile: SourceProfile) {
        storage.setActiveProfileId(profile.id)
        _uiState.value = _uiState.value.copy(
            activeProfile = profile,
            screen = AppScreen.MAIN
        )
        syncActiveProfile(profile)
    }

    fun deleteProfile(id: String) {
        storage.deleteProfile(id)
        val profiles = storage.getProfiles()
        val active = storage.getActiveProfile()
        _uiState.value = _uiState.value.copy(
            profiles = profiles,
            activeProfile = active,
            screen = if (active != null) AppScreen.MAIN else AppScreen.LOGIN
        )
        if (active != null) {
            syncActiveProfile(active)
        }
    }

    fun logout() {
        storage.logout()
        _uiState.value = _uiState.value.copy(
            activeProfile = null,
            screen = AppScreen.LOGIN
        )
    }

    fun setScreen(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(screen = screen)
    }

    fun setTab(tab: MainTab) {
        _uiState.value = _uiState.value.copy(
            currentTab = tab,
            searchQuery = ""
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectLiveCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedLiveCategoryId = categoryId)
    }

    fun selectMovieCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedMovieCategoryId = categoryId)
    }

    fun selectSeriesCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedSeriesCategoryId = categoryId)
    }

    fun selectMedia(item: MediaItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedMedia = item,
                selectedSeriesDetail = null,
                selectedSeason = 1,
                screen = AppScreen.MEDIA_DETAIL
            )

            // If it's a series or VOD, fetch details if available
            val active = _uiState.value.activeProfile
            if (item.streamType == StreamType.SERIES) {
                if (active != null && active.isDemo) {
                    val detail = storage.getSampleSeriesDetail(item.streamId)
                    _uiState.value = _uiState.value.copy(selectedSeriesDetail = detail)
                } else if (active != null && active.type == "XTREAM") {
                    try {
                        val detail = xtreamClient.getSeriesInfo(
                            active.serverUrl,
                            active.username,
                            active.password,
                            item.streamId,
                            _uiState.value.settings.corsProxyUrl
                        )
                        _uiState.value = _uiState.value.copy(selectedSeriesDetail = detail)
                    } catch (_: Exception) {}
                }
            } else if (item.streamType == StreamType.VOD && item.plot.isBlank()) {
                if (active != null && active.type == "XTREAM") {
                    try {
                        val enriched = xtreamClient.getVodInfo(
                            active.serverUrl,
                            active.username,
                            active.password,
                            item.streamId,
                            _uiState.value.settings.corsProxyUrl
                        )
                        if (enriched != null) {
                            _uiState.value = _uiState.value.copy(selectedMedia = enriched)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun selectSeason(seasonNum: Int) {
        _uiState.value = _uiState.value.copy(selectedSeason = seasonNum)
    }

    fun playStream(
        streamUrl: String,
        title: String,
        streamType: StreamType,
        streamId: String,
        posterUrl: String
    ) {
        // Check if there is continue watching position
        val historyItem = storage.getWatchHistory().find { it.streamId == streamId }
        val startPos = historyItem?.positionMs ?: 0L

        _uiState.value = _uiState.value.copy(
            playingStreamUrl = streamUrl,
            playingTitle = title,
            playingStreamType = streamType,
            playingStreamId = streamId,
            playingPosterUrl = posterUrl,
            playingStartPosMs = startPos,
            screen = AppScreen.PLAYER
        )
    }

    fun toggleFavorite(item: MediaItem) {
        storage.toggleFavorite(item)
        val updated = storage.getFavorites()
        _uiState.value = _uiState.value.copy(favorites = updated)
    }

    fun saveProgress(posMs: Long, durMs: Long) {
        val streamId = _uiState.value.playingStreamId
        if (streamId.isBlank()) return
        storage.saveWatchProgress(
            streamId = streamId,
            name = _uiState.value.playingTitle,
            posterUrl = _uiState.value.playingPosterUrl,
            streamType = _uiState.value.playingStreamType,
            positionMs = posMs,
            durationMs = durMs,
            streamUrl = _uiState.value.playingStreamUrl
        )
        _uiState.value = _uiState.value.copy(continueWatching = storage.getWatchHistory())
    }

    fun updateSettings(newSettings: AppSettings) {
        storage.updateSettings(newSettings)
        _uiState.value = _uiState.value.copy(settings = newSettings)
    }

    fun clearCache() {
        storage.clearCache()
        _uiState.value = _uiState.value.copy(
            continueWatching = emptyList(),
            statusMessage = Strings.get("cache_cleared", _uiState.value.settings.language)
        )
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null, errorMessage = null)
    }

    fun verifyPin(pin: String): Boolean {
        return _uiState.value.settings.parentalPin == pin
    }

    fun setParentalPin(pin: String, enabled: Boolean) {
        val updated = _uiState.value.settings.copy(
            parentalPin = pin,
            parentalLockEnabled = enabled
        )
        updateSettings(updated)
    }
}
