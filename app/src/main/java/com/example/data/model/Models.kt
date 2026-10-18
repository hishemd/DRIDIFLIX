package com.example.data.model

enum class StreamType {
    LIVE,
    VOD,     // Movie
    SERIES
}

enum class ExternalPlayerType {
    BUILT_IN,
    VLC,
    MX_PLAYER,
    JUST_PLAYER,
    SYSTEM
}

data class SourceProfile(
    val id: String,
    val name: String,
    val type: String, // "XTREAM" or "M3U"
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val m3uContent: String = "",
    val isDemo: Boolean = false
)

data class MediaItem(
    val id: String,
    val streamId: String,
    val name: String,
    val streamType: StreamType,
    val categoryId: String = "",
    val categoryName: String = "",
    val posterUrl: String = "",
    val rating: String = "",
    val year: String = "",
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    val containerExtension: String = "mp4",
    val streamUrl: String = "",
    val isFavorite: Boolean = false
)

data class CategoryItem(
    val id: String,
    val name: String,
    val streamType: StreamType,
    val isLocked: Boolean = false
)

data class EpisodeItem(
    val id: String,
    val episodeNum: Int,
    val seasonNum: Int,
    val title: String,
    val containerExtension: String = "mp4",
    val duration: String = "",
    val plot: String = "",
    val posterUrl: String = "",
    val streamUrl: String = ""
)

data class SeriesDetail(
    val id: String,
    val title: String,
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    val releaseDate: String = "",
    val rating: String = "",
    val seasons: List<Int> = emptyList(),
    val episodes: Map<Int, List<EpisodeItem>> = emptyMap()
)

data class UserInfo(
    val username: String = "",
    val status: String = "Active",
    val expDate: String = "",
    val activeCons: String = "1",
    val maxConnections: String = "1",
    val isTrial: Boolean = false,
    val createdAt: String = ""
)

data class ServerInfo(
    val url: String = "",
    val port: String = "",
    val httpsPort: String = "",
    val serverProtocol: String = "http",
    val timezone: String = ""
)

data class WatchHistoryItem(
    val streamId: String,
    val name: String,
    val posterUrl: String,
    val streamType: StreamType,
    val positionMs: Long,
    val durationMs: Long,
    val streamUrl: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val language: String = "ar", // "ar" or "en"
    val defaultPlayer: ExternalPlayerType = ExternalPlayerType.BUILT_IN,
    val corsProxyUrl: String = "",
    val parentalPin: String = "",
    val parentalLockEnabled: Boolean = false,
    val lockedCategories: Set<String> = emptySet()
)
