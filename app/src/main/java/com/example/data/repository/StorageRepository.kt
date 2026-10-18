package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class StorageRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("dridi_flix_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROFILES = "profiles_json"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_FAVORITES = "favorites_json"
        private const val KEY_HISTORY = "history_json"
        private const val KEY_SETTINGS_LANG = "settings_lang"
        private const val KEY_SETTINGS_PLAYER = "settings_player"
        private const val KEY_SETTINGS_PROXY = "settings_proxy"
        private const val KEY_SETTINGS_PIN = "settings_pin"
        private const val KEY_SETTINGS_PIN_ENABLED = "settings_pin_enabled"
        private const val KEY_SETTINGS_LOCKED_CATS = "settings_locked_cats"
    }

    init {
        // Initialize default Demo profile if no profiles exist
        if (getProfiles().isEmpty()) {
            val demoProfile = SourceProfile(
                id = "demo_profile_default",
                name = "DRIDI FLIX Demo (سيرفر تجريبي مجاني)",
                type = "XTREAM",
                serverUrl = "http://demo.dridiflix.com:8080",
                username = "demo_user",
                password = "demo_password",
                isDemo = true
            )
            saveProfile(demoProfile)
            setActiveProfileId(demoProfile.id)
        }
    }

    // --- PROFILES ---
    fun getProfiles(): List<SourceProfile> {
        val json = prefs.getString(KEY_PROFILES, "[]") ?: "[]"
        val list = mutableListOf<SourceProfile>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SourceProfile(
                        id = obj.getString("id"),
                        name = obj.optString("name", "Profile"),
                        type = obj.optString("type", "XTREAM"),
                        serverUrl = obj.optString("serverUrl", ""),
                        username = obj.optString("username", ""),
                        password = obj.optString("password", ""),
                        m3uUrl = obj.optString("m3uUrl", ""),
                        m3uContent = obj.optString("m3uContent", ""),
                        isDemo = obj.optBoolean("isDemo", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveProfile(profile: SourceProfile) {
        val current = getProfiles().toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index != -1) {
            current[index] = profile
        } else {
            current.add(profile)
        }
        val arr = JSONArray()
        current.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("type", p.type)
                put("serverUrl", p.serverUrl)
                put("username", p.username)
                put("password", p.password)
                put("m3uUrl", p.m3uUrl)
                put("m3uContent", p.m3uContent)
                put("isDemo", p.isDemo)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_PROFILES, arr.toString()).apply()
    }

    fun deleteProfile(id: String) {
        val current = getProfiles().filter { it.id != id }
        val arr = JSONArray()
        current.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("type", p.type)
                put("serverUrl", p.serverUrl)
                put("username", p.username)
                put("password", p.password)
                put("m3uUrl", p.m3uUrl)
                put("m3uContent", p.m3uContent)
                put("isDemo", p.isDemo)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_PROFILES, arr.toString()).apply()
        if (getActiveProfileId() == id) {
            val next = current.firstOrNull()?.id ?: ""
            setActiveProfileId(next)
        }
    }

    fun getActiveProfile(): SourceProfile? {
        val activeId = getActiveProfileId()
        val profiles = getProfiles()
        return profiles.find { it.id == activeId } ?: profiles.firstOrNull()
    }

    fun getActiveProfileId(): String {
        return prefs.getString(KEY_ACTIVE_PROFILE_ID, "") ?: ""
    }

    fun setActiveProfileId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, id).apply()
    }

    fun logout() {
        prefs.edit().remove(KEY_ACTIVE_PROFILE_ID).apply()
    }

    // --- FAVORITES ---
    fun getFavorites(): List<MediaItem> {
        val json = prefs.getString(KEY_FAVORITES, "[]") ?: "[]"
        val list = mutableListOf<MediaItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    MediaItem(
                        id = obj.getString("id"),
                        streamId = obj.optString("streamId", ""),
                        name = obj.optString("name", ""),
                        streamType = StreamType.valueOf(obj.optString("streamType", "VOD")),
                        categoryId = obj.optString("categoryId", ""),
                        categoryName = obj.optString("categoryName", ""),
                        posterUrl = obj.optString("posterUrl", ""),
                        rating = obj.optString("rating", ""),
                        year = obj.optString("year", ""),
                        plot = obj.optString("plot", ""),
                        streamUrl = obj.optString("streamUrl", ""),
                        isFavorite = true
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun toggleFavorite(item: MediaItem): Boolean {
        val current = getFavorites().toMutableList()
        val index = current.indexOfFirst { it.id == item.id || (it.streamId == item.streamId && it.streamType == item.streamType) }
        val isNowFavorite: Boolean
        if (index != -1) {
            current.removeAt(index)
            isNowFavorite = false
        } else {
            current.add(0, item.copy(isFavorite = true))
            isNowFavorite = true
        }

        val arr = JSONArray()
        current.forEach { fav ->
            val obj = JSONObject().apply {
                put("id", fav.id)
                put("streamId", fav.streamId)
                put("name", fav.name)
                put("streamType", fav.streamType.name)
                put("categoryId", fav.categoryId)
                put("categoryName", fav.categoryName)
                put("posterUrl", fav.posterUrl)
                put("rating", fav.rating)
                put("year", fav.year)
                put("plot", fav.plot)
                put("streamUrl", fav.streamUrl)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_FAVORITES, arr.toString()).apply()
        return isNowFavorite
    }

    fun isFavorite(id: String, streamId: String): Boolean {
        return getFavorites().any { it.id == id || it.streamId == streamId }
    }

    // --- WATCH HISTORY / CONTINUE WATCHING ---
    fun getWatchHistory(): List<WatchHistoryItem> {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<WatchHistoryItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WatchHistoryItem(
                        streamId = obj.getString("streamId"),
                        name = obj.optString("name", ""),
                        posterUrl = obj.optString("posterUrl", ""),
                        streamType = StreamType.valueOf(obj.optString("streamType", "VOD")),
                        positionMs = obj.optLong("positionMs", 0),
                        durationMs = obj.optLong("durationMs", 0),
                        streamUrl = obj.optString("streamUrl", ""),
                        updatedAt = obj.optLong("updatedAt", 0)
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.updatedAt }
    }

    fun saveWatchProgress(
        streamId: String,
        name: String,
        posterUrl: String,
        streamType: StreamType,
        positionMs: Long,
        durationMs: Long,
        streamUrl: String
    ) {
        val current = getWatchHistory().toMutableList()
        current.removeAll { it.streamId == streamId }
        // Keep progress if within 95% of duration (if finished, drop from continue watching)
        if (durationMs <= 0 || positionMs < durationMs * 0.95) {
            current.add(
                0,
                WatchHistoryItem(
                    streamId = streamId,
                    name = name,
                    posterUrl = posterUrl,
                    streamType = streamType,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    streamUrl = streamUrl,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        val arr = JSONArray()
        current.take(20).forEach { item ->
            val obj = JSONObject().apply {
                put("streamId", item.streamId)
                put("name", item.name)
                put("posterUrl", item.posterUrl)
                put("streamType", item.streamType.name)
                put("positionMs", item.positionMs)
                put("durationMs", item.durationMs)
                put("streamUrl", item.streamUrl)
                put("updatedAt", item.updatedAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    // --- APP SETTINGS ---
    fun getSettings(): AppSettings {
        val lang = prefs.getString(KEY_SETTINGS_LANG, "ar") ?: "ar"
        val playerStr = prefs.getString(KEY_SETTINGS_PLAYER, ExternalPlayerType.BUILT_IN.name)
        val player = try {
            ExternalPlayerType.valueOf(playerStr ?: ExternalPlayerType.BUILT_IN.name)
        } catch (_: Exception) {
            ExternalPlayerType.BUILT_IN
        }
        val proxy = prefs.getString(KEY_SETTINGS_PROXY, "") ?: ""
        val pin = prefs.getString(KEY_SETTINGS_PIN, "") ?: ""
        val pinEnabled = prefs.getBoolean(KEY_SETTINGS_PIN_ENABLED, false)
        val lockedCats = prefs.getStringSet(KEY_SETTINGS_LOCKED_CATS, emptySet()) ?: emptySet()

        return AppSettings(
            language = lang,
            defaultPlayer = player,
            corsProxyUrl = proxy,
            parentalPin = pin,
            parentalLockEnabled = pinEnabled,
            lockedCategories = lockedCats
        )
    }

    fun updateSettings(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_SETTINGS_LANG, settings.language)
            .putString(KEY_SETTINGS_PLAYER, settings.defaultPlayer.name)
            .putString(KEY_SETTINGS_PROXY, settings.corsProxyUrl)
            .putString(KEY_SETTINGS_PIN, settings.parentalPin)
            .putBoolean(KEY_SETTINGS_PIN_ENABLED, settings.parentalLockEnabled)
            .putStringSet(KEY_SETTINGS_LOCKED_CATS, settings.lockedCategories)
            .apply()
    }

    fun clearCache() {
        prefs.edit()
            .remove(KEY_HISTORY)
            .apply()
    }

    // --- SAMPLE / DEMO DATA PROVIDER ---
    fun getSampleLiveChannels(): List<MediaItem> {
        return listOf(
            MediaItem(
                id = "live_aljazeera",
                streamId = "aljazeera_ar",
                name = "الجزيرة الإخبارية HD (Al Jazeera)",
                streamType = StreamType.LIVE,
                categoryId = "news",
                categoryName = "أخبار (News)",
                posterUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/f/f2/Aljazeera_eng.svg/512px-Aljazeera_eng.svg.png",
                streamUrl = "https://live-hls-cf-aljazeera-arabic.akamaized.net/apple/live-hls-cf-aljazeera-arabic/arabic/index.m3u8"
            ),
            MediaItem(
                id = "live_france24",
                streamId = "france24_ar",
                name = "فرانس 24 العربية HD (France 24)",
                streamType = StreamType.LIVE,
                categoryId = "news",
                categoryName = "أخبار (News)",
                posterUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/France_24_logo.svg/512px-France_24_logo.svg.png",
                streamUrl = "https://f24hls-i.akamaihd.net/hls/live/221193/F24_AR_HLS/master_2000.m3u8"
            ),
            MediaItem(
                id = "live_dw_arabic",
                streamId = "dw_ar",
                name = "دويتشه فيله DW العربية HD",
                streamType = StreamType.LIVE,
                categoryId = "news",
                categoryName = "أخبار (News)",
                posterUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Deutsche_Welle_logo.svg/512px-Deutsche_Welle_logo.svg.png",
                streamUrl = "https://dwstream4-lh.akamaihd.net/i/dwarabic_0@470984/master.m3u8"
            ),
            MediaItem(
                id = "live_nasa",
                streamId = "nasa_tv",
                name = "NASA TV Ultra HD (الفضاء والعلوم)",
                streamType = StreamType.LIVE,
                categoryId = "docs",
                categoryName = "وثائقي وعلوم (Documentary)",
                posterUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/NASA_logo.svg/512px-NASA_logo.svg.png",
                streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8"
            ),
            MediaItem(
                id = "live_redbull",
                streamId = "redbull_tv",
                name = "Red Bull TV Live (رياضة وحماس)",
                streamType = StreamType.LIVE,
                categoryId = "sports",
                categoryName = "رياضة (Sports)",
                posterUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/f/f5/Red_Bull_TV_logo.svg/512px-Red_Bull_TV_logo.svg.png",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8"
            )
        )
    }

    fun getSampleMovies(): List<MediaItem> {
        return listOf(
            MediaItem(
                id = "vod_strikepoint",
                streamId = "101",
                name = "نقطة الهجوم: صراع الشوارع (StrikePoint)",
                streamType = StreamType.VOD,
                categoryId = "action",
                categoryName = "أكشن وإثارة (Action)",
                posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80",
                rating = "8.9",
                year = "2026",
                plot = "في شوارع المدينة المشتعلة، يخوض قائد نخبة العمليات الخاصة صراعاً مصيرياً لحماية الأبرياء وكشف خيانة كبرى في قلب النظام.",
                cast = "ريان جوسلينج، إلياس ثورن، سارة فالنسيا",
                director = "ماركوس فان",
                genre = "أكشن / إثارة / تشويق",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            ),
            MediaItem(
                id = "vod_big_buck_bunny",
                streamId = "102",
                name = "مغامرة الأرنب الضخم (Big Buck Bunny)",
                streamType = StreamType.VOD,
                categoryId = "animation",
                categoryName = "رسوم متحركة (Animation)",
                posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80",
                rating = "8.4",
                year = "2024",
                plot = "مغامرة كرتونية مبهجة في الغابة الخضراء حين يقرر أرنب عملاق الدفاع عن أصدقائه الضعفاء والتصدي للمتنمرين بحيل ذكية ومضحكة.",
                cast = "ساسشا غود، أليخاندرو مارتين",
                director = "ساتشا ويلكينز",
                genre = "عائلي / كوميديا / مغامرة",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            ),
            MediaItem(
                id = "vod_tears_of_steel",
                streamId = "103",
                name = "دموع الفولاذ (Tears of Steel)",
                streamType = StreamType.VOD,
                categoryId = "scifi",
                categoryName = "خيال علمي (Sci-Fi)",
                posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
                rating = "7.8",
                year = "2025",
                plot = "في مستقبل ديستوبي تسيطر فيه الآلات العملاقة، يجتمع فريق من العلماء في محاولة أخيرة لإعادة كتابة التاريخ وإنقاذ الأرض.",
                cast = "ديريك دي لينت، دينيس ريفرز",
                director = "إيان هوبرتس",
                genre = "خيال علمي / مستقبل / سينما",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            ),
            MediaItem(
                id = "vod_sintel",
                streamId = "104",
                name = "أسطورة سينتل والتنين (Sintel)",
                streamType = StreamType.VOD,
                categoryId = "fantasy",
                categoryName = "فانتازيا وخيال (Fantasy)",
                posterUrl = "https://images.unsplash.com/photo-1514533450685-4493e01d1fdc?w=600&auto=format&fit=crop&q=80",
                rating = "8.2",
                year = "2023",
                plot = "رحلة ملحمية تخوضها الفتاة الشجاعة سينتل عبر جبال الثلج والصحاري للبحث عن تنين صغير أنقذته في طفولتها.",
                cast = "هالينا رين، توم هوفمان",
                director = "كولين ليفي",
                genre = "فانتازيا / دراما / مغامرة",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
            ),
            MediaItem(
                id = "vod_elephants_dream",
                streamId = "105",
                name = "حلم المتاهة الميكانيكية (Elephant's Dream)",
                streamType = StreamType.VOD,
                categoryId = "scifi",
                categoryName = "خيال علمي (Sci-Fi)",
                posterUrl = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=600&auto=format&fit=crop&q=80",
                rating = "7.5",
                year = "2024",
                plot = "اثنان من المستكشفين داخل آلة حية عملاقة وغامضة، يتعلمان كيفية التحكم في مسارات الواقع المعقدة.",
                cast = "تيش أوفلاهرتي، كاسبير كورتز",
                director = "باسم الخوري",
                genre = "خيال علمي / تشويق",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            )
        )
    }

    fun getSampleSeries(): List<MediaItem> {
        return listOf(
            MediaItem(
                id = "series_berlin_protocol",
                streamId = "201",
                name = "بروتوكول برلين: الظلال السوداء (Berlin Protocol)",
                streamType = StreamType.SERIES,
                categoryId = "drama",
                categoryName = "دراما وتشويق (Drama)",
                posterUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=600&auto=format&fit=crop&q=80",
                rating = "9.1",
                year = "2026",
                plot = "مسلسل مخابراتي مثير يتتبع ضابطاً سرياً في برلين يكشف شبكة تجسس دولية تتحكم في مصادر الطاقة العالمية.",
                cast = "كارل كراوس، لورا هانسن",
                director = "ستيفان فاغنر",
                genre = "تشويق / غموض / دراما"
            ),
            MediaItem(
                id = "series_cyber_realm",
                streamId = "202",
                name = "عالم السيبر: ما بعد 2070 (Cyber Realm)",
                streamType = StreamType.SERIES,
                categoryId = "scifi",
                categoryName = "خيال علمي (Sci-Fi)",
                posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
                rating = "8.7",
                year = "2025",
                plot = "في مدينة يابانية تقودها الروبوتات والذكاء الاصطناعي، تسعى مجموعة من القراصنة لاستعادة السيطرة البشرية على الشبكة.",
                cast = "كينجي تاناكا، مايا لين",
                director = "هيروشي تاكاهاشي",
                genre = "سايبربانك / إثارة"
            )
        )
    }

    fun getSampleSeriesDetail(seriesId: String): SeriesDetail {
        val epList1 = listOf(
            EpisodeItem(
                id = "ep_1",
                episodeNum = 1,
                seasonNum = 1,
                title = "الحلقة 1: البداية الغامضة (The Breach)",
                duration = "45m",
                plot = "اختراق أمني مفاجئ يكشف ملفات سرية تهدد استقرار المدينة ويبدأ التحقيق المعقد.",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            ),
            EpisodeItem(
                id = "ep_2",
                episodeNum = 2,
                seasonNum = 1,
                title = "الحلقة 2: في قلب الخطر (In the Crosshairs)",
                duration = "48m",
                plot = "مطاردة مثيرة في شوارع العاصمة تفضي إلى اكتشاف حليف غير متوقع.",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            ),
            EpisodeItem(
                id = "ep_3",
                episodeNum = 3,
                seasonNum = 1,
                title = "الحلقة 3: نقطة اللاعودة (Point of No Return)",
                duration = "52m",
                plot = "مواجهة مباشرة مع الرأس المدبر تكشف حقائق صادمة لم تكن في الحسبان.",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
            )
        )
        val epList2 = listOf(
            EpisodeItem(
                id = "ep_4",
                episodeNum = 1,
                seasonNum = 2,
                title = "الحلقة 1: النهوض من جديد (Resurgence)",
                duration = "50m",
                plot = "بعد عام من أحداث الموسم الأول، تظهر إشارات لاسلكية مجهولة من المنطقة المحظورة.",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            ),
            EpisodeItem(
                id = "ep_5",
                episodeNum = 2,
                seasonNum = 2,
                title = "الحلقة 2: المعركة الأخيرة (The Final Gambit)",
                duration = "55m",
                plot = "حشد القوات لمواجهة مصيرية تحدد مستقبل العالم الجديد.",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            )
        )

        return SeriesDetail(
            id = seriesId,
            title = if (seriesId == "series_berlin_protocol") "بروتوكول برلين: الظلال السوداء" else "عالم السيبر: ما بعد 2070",
            posterUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=600&auto=format&fit=crop&q=80",
            plot = "مسلسل درامي ملحمي مشوق عالي التقييم مع حبكة لا تتوقف عن المفاجآت.",
            cast = "نخبة من ألمع نجوم الدراما والسينما",
            director = "المخرج العالمي ستيفان فاغنر",
            genre = "دراما / تشويق / خيال علمي",
            releaseDate = "2026",
            rating = "9.1",
            seasons = listOf(1, 2),
            episodes = mapOf(1 to epList1, 2 to epList2)
        )
    }
}
