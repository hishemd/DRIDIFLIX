package com.example.data.repository

import com.example.data.model.CategoryItem
import com.example.data.model.MediaItem
import com.example.data.model.StreamType
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID
import java.util.regex.Pattern

data class ParsedM3uResult(
    val items: List<MediaItem>,
    val categories: List<CategoryItem>
)

object M3uParser {

    private val LOGO_PATTERN = Pattern.compile("tvg-logo=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val GROUP_PATTERN = Pattern.compile("group-title=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val NAME_PATTERN = Pattern.compile("tvg-name=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val ID_PATTERN = Pattern.compile("tvg-id=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)

    fun parse(content: String): ParsedM3uResult {
        val items = mutableListOf<MediaItem>()
        val categoriesSet = mutableMapOf<String, StreamType>()

        val reader = BufferedReader(StringReader(content))
        var line: String?
        var currentTvgLogo = ""
        var currentGroupTitle = ""
        var currentTvgName = ""
        var currentTvgId = ""
        var currentDisplayName = ""
        var hasPendingItem = false

        while (reader.readLine().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                // Parse attributes
                currentTvgLogo = extractAttribute(LOGO_PATTERN, trimmed)
                currentGroupTitle = extractAttribute(GROUP_PATTERN, trimmed)
                currentTvgName = extractAttribute(NAME_PATTERN, trimmed)
                currentTvgId = extractAttribute(ID_PATTERN, trimmed)

                val commaIdx = trimmed.lastIndexOf(',')
                currentDisplayName = if (commaIdx != -1 && commaIdx < trimmed.length - 1) {
                    trimmed.substring(commaIdx + 1).trim()
                } else if (currentTvgName.isNotEmpty()) {
                    currentTvgName
                } else {
                    "Channel"
                }

                if (currentGroupTitle.isEmpty()) {
                    currentGroupTitle = "General"
                }

                hasPendingItem = true
            } else if (!trimmed.startsWith("#") && hasPendingItem) {
                // This line is the stream URL
                val streamUrl = trimmed
                val streamType = detectStreamType(currentGroupTitle, streamUrl)
                val id = if (currentTvgId.isNotEmpty()) currentTvgId else UUID.randomUUID().toString()

                val item = MediaItem(
                    id = id,
                    streamId = id,
                    name = currentDisplayName,
                    streamType = streamType,
                    categoryId = currentGroupTitle.lowercase().replace(" ", "_"),
                    categoryName = currentGroupTitle,
                    posterUrl = currentTvgLogo,
                    streamUrl = streamUrl
                )
                items.add(item)
                categoriesSet[currentGroupTitle] = streamType

                // Reset
                hasPendingItem = false
                currentTvgLogo = ""
                currentGroupTitle = ""
                currentTvgName = ""
                currentTvgId = ""
                currentDisplayName = ""
            }
        }

        val categories = categoriesSet.map { (catName, type) ->
            CategoryItem(
                id = catName.lowercase().replace(" ", "_"),
                name = catName,
                streamType = type
            )
        }

        return ParsedM3uResult(items, categories)
    }

    private fun extractAttribute(pattern: Pattern, text: String): String {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)?.trim() ?: ""
        } else {
            ""
        }
    }

    private fun detectStreamType(groupTitle: String, url: String): StreamType {
        val lowerGroup = groupTitle.lowercase()
        val lowerUrl = url.lowercase()

        val isMovie = lowerGroup.contains("movie") ||
                lowerGroup.contains("film") ||
                lowerGroup.contains("vod") ||
                lowerGroup.contains("cinema") ||
                lowerGroup.contains("أفلام") ||
                lowerGroup.contains("افلام") ||
                lowerUrl.contains("/movie/") ||
                lowerUrl.endsWith(".mp4") ||
                lowerUrl.endsWith(".mkv") ||
                lowerUrl.endsWith(".avi")

        if (isMovie) return StreamType.VOD

        val isSeries = lowerGroup.contains("series") ||
                lowerGroup.contains("série") ||
                lowerGroup.contains("serie") ||
                lowerGroup.contains("مسلسل") ||
                lowerGroup.contains("season") ||
                lowerGroup.contains("episode") ||
                lowerUrl.contains("/series/")

        if (isSeries) return StreamType.SERIES

        return StreamType.LIVE
    }
}
