package com.example.data.repository

import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class XtreamApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun cleanUrl(rawUrl: String): String {
        var clean = rawUrl.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        return clean.trimEnd('/')
    }

    private fun buildRequestUrl(baseUrl: String, params: Map<String, String>, corsProxy: String): String {
        val server = cleanUrl(baseUrl)
        val query = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        val target = "$server/player_api.php?$query"
        return if (corsProxy.isNotBlank()) {
            val cleanProxy = cleanUrl(corsProxy)
            "$cleanProxy/$target"
        } else {
            target
        }
    }

    suspend fun authenticate(
        serverUrl: String,
        user: String,
        pass: String,
        corsProxy: String = ""
    ): Pair<UserInfo, ServerInfo> = withContext(Dispatchers.IO) {
        val url = buildRequestUrl(serverUrl, mapOf("username" to user, "password" to pass), corsProxy)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "DRIDIFLIX/2.5.0 (Android; IPTV Player)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            val body = response.body?.string() ?: throw Exception("Empty response from server")
            val json = JSONObject(body)

            val userInfoObj = json.optJSONObject("user_info")
                ?: throw Exception("Invalid response: user_info missing")

            val auth = userInfoObj.optInt("auth", 0)
            val status = userInfoObj.optString("status", "")
            if (auth != 1 || status.equals("Disabled", ignoreCase = true) || status.equals("Banned", ignoreCase = true)) {
                throw Exception("Authentication failed: Status is $status")
            }

            val expDate = userInfoObj.optString("exp_date", "")
            val activeCons = userInfoObj.optString("active_cons", "0")
            val maxConnections = userInfoObj.optString("max_connections", "1")
            val isTrial = userInfoObj.optInt("is_trial", 0) == 1
            val createdAt = userInfoObj.optString("created_at", "")

            val serverInfoObj = json.optJSONObject("server_info")
            val serverInfo = if (serverInfoObj != null) {
                ServerInfo(
                    url = serverInfoObj.optString("url", serverUrl),
                    port = serverInfoObj.optString("port", ""),
                    httpsPort = serverInfoObj.optString("https_port", ""),
                    serverProtocol = serverInfoObj.optString("server_protocol", "http"),
                    timezone = serverInfoObj.optString("timezone", "")
                )
            } else {
                ServerInfo(url = serverUrl)
            }

            val userInfo = UserInfo(
                username = user,
                status = if (status.isEmpty()) "Active" else status,
                expDate = expDate,
                activeCons = activeCons,
                maxConnections = maxConnections,
                isTrial = isTrial,
                createdAt = createdAt
            )

            Pair(userInfo, serverInfo)
        }
    }

    suspend fun getLiveCategories(
        serverUrl: String,
        user: String,
        pass: String,
        corsProxy: String = ""
    ): List<CategoryItem> = withContext(Dispatchers.IO) {
        val url = buildRequestUrl(serverUrl, mapOf(
            "username" to user,
            "password" to pass,
            "action" to "get_live_categories"
        ), corsProxy)

        executeCategoriesRequest(url, StreamType.LIVE)
    }

    suspend fun getVodCategories(
        serverUrl: String,
        user: String,
        pass: String,
        corsProxy: String = ""
    ): List<CategoryItem> = withContext(Dispatchers.IO) {
        val url = buildRequestUrl(serverUrl, mapOf(
            "username" to user,
            "password" to pass,
            "action" to "get_vod_categories"
        ), corsProxy)

        executeCategoriesRequest(url, StreamType.VOD)
    }

    suspend fun getSeriesCategories(
        serverUrl: String,
        user: String,
        pass: String,
        corsProxy: String = ""
    ): List<CategoryItem> = withContext(Dispatchers.IO) {
        val url = buildRequestUrl(serverUrl, mapOf(
            "username" to user,
            "password" to pass,
            "action" to "get_series_categories"
        ), corsProxy)

        executeCategoriesRequest(url, StreamType.SERIES)
    }

    private fun executeCategoriesRequest(url: String, type: StreamType): List<CategoryItem> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "DRIDIFLIX/2.5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val list = mutableListOf<CategoryItem>()
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val catId = obj.optString("category_id", "")
                val catName = obj.optString("category_name", "")
                if (catId.isNotEmpty() && catName.isNotEmpty()) {
                    list.add(CategoryItem(id = catId, name = catName, streamType = type))
                }
            }
            return list
        }
    }

    suspend fun getLiveStreams(
        serverUrl: String,
        user: String,
        pass: String,
        categoryId: String = "",
        corsProxy: String = ""
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val params = mutableMapOf(
            "username" to user,
            "password" to pass,
            "action" to "get_live_streams"
        )
        if (categoryId.isNotEmpty()) params["category_id"] = categoryId

        val url = buildRequestUrl(serverUrl, params, corsProxy)
        val request = Request.Builder().url(url).header("User-Agent", "DRIDIFLIX/2.5.0").build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val list = mutableListOf<MediaItem>()
            val arr = JSONArray(body)
            val cleanServer = cleanUrl(serverUrl)

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val streamId = obj.optString("stream_id", "")
                val name = obj.optString("name", "Live Channel")
                val streamIcon = obj.optString("stream_icon", "")
                val catId = obj.optString("category_id", "")
                val streamUrl = "$cleanServer/live/$user/$pass/$streamId.m3u8"

                list.add(
                    MediaItem(
                        id = "live_$streamId",
                        streamId = streamId,
                        name = name,
                        streamType = StreamType.LIVE,
                        categoryId = catId,
                        posterUrl = streamIcon,
                        streamUrl = streamUrl
                    )
                )
            }
            list
        }
    }

    suspend fun getVodStreams(
        serverUrl: String,
        user: String,
        pass: String,
        categoryId: String = "",
        corsProxy: String = ""
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val params = mutableMapOf(
            "username" to user,
            "password" to pass,
            "action" to "get_vod_streams"
        )
        if (categoryId.isNotEmpty()) params["category_id"] = categoryId

        val url = buildRequestUrl(serverUrl, params, corsProxy)
        val request = Request.Builder().url(url).header("User-Agent", "DRIDIFLIX/2.5.0").build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val list = mutableListOf<MediaItem>()
            val arr = JSONArray(body)
            val cleanServer = cleanUrl(serverUrl)

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val streamId = obj.optString("stream_id", "")
                val name = obj.optString("name", "Movie")
                val streamIcon = obj.optString("stream_icon", "")
                val catId = obj.optString("category_id", "")
                val rating = obj.optString("rating", "")
                val ext = obj.optString("container_extension", "mp4").ifEmpty { "mp4" }
                val streamUrl = "$cleanServer/movie/$user/$pass/$streamId.$ext"

                list.add(
                    MediaItem(
                        id = "vod_$streamId",
                        streamId = streamId,
                        name = name,
                        streamType = StreamType.VOD,
                        categoryId = catId,
                        posterUrl = streamIcon,
                        rating = rating,
                        containerExtension = ext,
                        streamUrl = streamUrl
                    )
                )
            }
            list
        }
    }

    suspend fun getVodInfo(
        serverUrl: String,
        user: String,
        pass: String,
        vodId: String,
        corsProxy: String = ""
    ): MediaItem? = withContext(Dispatchers.IO) {
        val url = buildRequestUrl(serverUrl, mapOf(
            "username" to user,
            "password" to pass,
            "action" to "get_vod_info",
            "vod_id" to vodId
        ), corsProxy)

        val request = Request.Builder().url(url).header("User-Agent", "DRIDIFLIX/2.5.0").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val root = JSONObject(body)
            val info = root.optJSONObject("info") ?: return@withContext null
            val movieData = root.optJSONObject("movie_data")
            val cleanServer = cleanUrl(serverUrl)
            val ext = movieData?.optString("container_extension", "mp4") ?: "mp4"

            MediaItem(
                id = "vod_$vodId",
                streamId = vodId,
                name = info.optString("name", "Movie"),
                streamType = StreamType.VOD,
                posterUrl = info.optString("movie_image", ""),
                rating = info.optString("rating", ""),
                year = info.optString("releasedate", "").take(4),
                plot = info.optString("plot", info.optString("description", "")),
                cast = info.optString("cast", ""),
                director = info.optString("director", ""),
                genre = info.optString("genre", ""),
                containerExtension = ext,
                streamUrl = "$cleanServer/movie/$user/$pass/$vodId.$ext"
            )
        }
    }

    suspend fun getSeries(
        serverUrl: String,
        user: String,
        pass: String,
        categoryId: String = "",
        corsProxy: String = ""
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val params = mutableMapOf(
            "username" to user,
            "password" to pass,
            "action" to "get_series"
        )
        if (categoryId.isNotEmpty()) params["category_id"] = categoryId

        val url = buildRequestUrl(serverUrl, params, corsProxy)
        val request = Request.Builder().url(url).header("User-Agent", "DRIDIFLIX/2.5.0").build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val list = mutableListOf<MediaItem>()
            val arr = JSONArray(body)

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val seriesId = obj.optString("series_id", "")
                val name = obj.optString("name", "Series")
                val cover = obj.optString("cover", "")
                val catId = obj.optString("category_id", "")
                val rating = obj.optString("rating", "")
                val year = obj.optString("releaseDate", "").take(4)
                val plot = obj.optString("plot", "")
                val cast = obj.optString("cast", "")
                val genre = obj.optString("genre", "")

                list.add(
                    MediaItem(
                        id = "series_$seriesId",
                        streamId = seriesId,
                        name = name,
                        streamType = StreamType.SERIES,
                        categoryId = catId,
                        posterUrl = cover,
                        rating = rating,
                        year = year,
                        plot = plot,
                        cast = cast,
                        genre = genre
                    )
                )
            }
            list
        }
    }

    suspend fun getSeriesInfo(
        serverUrl: String,
        user: String,
        pass: String,
        seriesId: String,
        corsProxy: String = ""
    ): SeriesDetail? = withContext(Dispatchers.IO) {
        val url = buildRequestUrl(serverUrl, mapOf(
            "username" to user,
            "password" to pass,
            "action" to "get_series_info",
            "series_id" to seriesId
        ), corsProxy)

        val request = Request.Builder().url(url).header("User-Agent", "DRIDIFLIX/2.5.0").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val root = JSONObject(body)
            val info = root.optJSONObject("info") ?: JSONObject()
            val episodesObj = root.optJSONObject("episodes") ?: JSONObject()
            val cleanServer = cleanUrl(serverUrl)

            val seasonsList = mutableListOf<Int>()
            val episodesMap = mutableMapOf<Int, List<EpisodeItem>>()

            val seasonKeys = episodesObj.keys()
            while (seasonKeys.hasNext()) {
                val seasonKey = seasonKeys.next()
                val seasonNum = seasonKey.toIntOrNull() ?: 1
                seasonsList.add(seasonNum)

                val epArray = episodesObj.optJSONArray(seasonKey) ?: JSONArray()
                val epList = mutableListOf<EpisodeItem>()

                for (j in 0 until epArray.length()) {
                    val epObj = epArray.getJSONObject(j)
                    val epId = epObj.optString("id", "")
                    val epNum = epObj.optInt("episode_num", j + 1)
                    val title = epObj.optString("title", "Episode $epNum")
                    val ext = epObj.optString("container_extension", "mp4").ifEmpty { "mp4" }
                    val epInfo = epObj.optJSONObject("info")
                    val epPlot = epInfo?.optString("plot", "") ?: ""
                    val duration = epInfo?.optString("duration", "") ?: ""
                    val streamUrl = "$cleanServer/series/$user/$pass/$epId.$ext"

                    epList.add(
                        EpisodeItem(
                            id = epId,
                            episodeNum = epNum,
                            seasonNum = seasonNum,
                            title = title,
                            containerExtension = ext,
                            duration = duration,
                            plot = epPlot,
                            streamUrl = streamUrl
                        )
                    )
                }
                episodesMap[seasonNum] = epList.sortedBy { it.episodeNum }
            }

            SeriesDetail(
                id = seriesId,
                title = info.optString("name", "Series"),
                posterUrl = info.optString("cover", ""),
                backdropUrl = info.optJSONArray("backdrop_path")?.optString(0) ?: "",
                plot = info.optString("plot", ""),
                cast = info.optString("cast", ""),
                director = info.optString("director", ""),
                genre = info.optString("genre", ""),
                releaseDate = info.optString("releaseDate", ""),
                rating = info.optString("rating", ""),
                seasons = seasonsList.sorted(),
                episodes = episodesMap
            )
        }
    }
}
