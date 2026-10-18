package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.MediaItem
import com.example.data.model.StreamType
import com.example.data.model.WatchHistoryItem
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.ExternalPlayerMenuSheet
import com.example.ui.components.MediaPosterCard
import com.example.ui.theme.*
import com.example.util.Strings

@Composable
fun HomeScreen(
    uiState: UiState,
    viewModel: MainViewModel,
    onSelectMedia: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = uiState.settings.language
    var externalPlayerItem by remember { mutableStateOf<MediaItem?>(null) }

    // Featured Hero Movie (pick from movies or fallback to StrikePoint)
    val featuredMovie = uiState.movies.firstOrNull() ?: MediaItem(
        id = "hero_featured",
        streamId = "101",
        name = "نقطة الهجوم: صراع الشوارع (StrikePoint)",
        streamType = StreamType.VOD,
        rating = "8.9",
        year = "2026",
        genre = "أكشن / إثارة / تشويق",
        plot = "في شوارع المدينة المشتعلة، يخوض قائد نخبة العمليات الخاصة صراعاً مصيرياً لحماية الأبرياء.",
        streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                // Background image - local hero banner asset
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(R.drawable.img_hero_banner)
                        .crossfade(true)
                        .build(),
                    contentDescription = featuredMovie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top & Bottom Noir Gradients
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    NetflixBlack.copy(alpha = 0.5f),
                                    Color.Transparent,
                                    NetflixBlack.copy(alpha = 0.85f),
                                    NetflixBlack
                                )
                            )
                        )
                )

                // Featured Title & Action Buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tagline
                    Text(
                        text = featuredMovie.genre.ifBlank { "أكشن • خيال علمي • سينما عالمية" },
                        color = NetflixOffWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = featuredMovie.name,
                        color = NetflixWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons Row: Play, External Player, Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play Button
                        Button(
                            onClick = {
                                viewModel.playStream(
                                    streamUrl = featuredMovie.streamUrl,
                                    title = featuredMovie.name,
                                    streamType = featuredMovie.streamType,
                                    streamId = featuredMovie.streamId,
                                    posterUrl = featuredMovie.posterUrl
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NetflixWhite),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .weight(1f)
                                .testTag("hero_play_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = NetflixBlack,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = Strings.get("play", lang),
                                    color = NetflixBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Details Button
                        FilledTonalButton(
                            onClick = { onSelectMedia(featuredMovie) },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = NetflixCardBg),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Info",
                                tint = NetflixWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // External Player Button
                        FilledTonalButton(
                            onClick = { externalPlayerItem = featuredMovie },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = NetflixCardBg),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = Strings.get("open_external", lang),
                                tint = NetflixRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section: Continue Watching
        if (uiState.continueWatching.isNotEmpty()) {
            item {
                SectionHeader(title = Strings.get("continue_watching", lang))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.continueWatching) { history ->
                        ContinueWatchingCard(
                            item = history,
                            onClick = {
                                viewModel.playStream(
                                    streamUrl = history.streamUrl,
                                    title = history.name,
                                    streamType = history.streamType,
                                    streamId = history.streamId,
                                    posterUrl = history.posterUrl
                                )
                            }
                        )
                    }
                }
            }
        }

        // Section: Favorites
        if (uiState.favorites.isNotEmpty()) {
            item {
                SectionHeader(title = Strings.get("favorites", lang))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.favorites) { item ->
                        MediaPosterCard(
                            item = item,
                            onClick = { onSelectMedia(item) },
                            modifier = Modifier.width(115.dp)
                        )
                    }
                }
            }
        }

        // Section: Live Channels Highlights
        if (uiState.liveChannels.isNotEmpty()) {
            item {
                SectionHeader(title = Strings.get("live_news_sports", lang))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.liveChannels.take(8)) { channel ->
                        LiveChannelCard(
                            item = channel,
                            onClick = {
                                viewModel.playStream(
                                    streamUrl = channel.streamUrl,
                                    title = channel.name,
                                    streamType = StreamType.LIVE,
                                    streamId = channel.streamId,
                                    posterUrl = channel.posterUrl
                                )
                            }
                        )
                    }
                }
            }
        }

        // Section: Recently Added Movies
        if (uiState.movies.isNotEmpty()) {
            item {
                SectionHeader(title = Strings.get("recently_added", lang))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.movies.take(12)) { movie ->
                        MediaPosterCard(
                            item = movie,
                            onClick = { onSelectMedia(movie) },
                            modifier = Modifier.width(115.dp)
                        )
                    }
                }
            }
        }

        // Section: Trending Series
        if (uiState.seriesList.isNotEmpty()) {
            item {
                SectionHeader(title = Strings.get("trending_series", lang))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.seriesList.take(12)) { series ->
                        MediaPosterCard(
                            item = series,
                            onClick = { onSelectMedia(series) },
                            modifier = Modifier.width(115.dp)
                        )
                    }
                }
            }
        }

        // Section: Popular Movies
        if (uiState.movies.size > 12) {
            item {
                SectionHeader(title = Strings.get("popular_movies", lang))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.movies.drop(12).take(12)) { movie ->
                        MediaPosterCard(
                            item = movie,
                            onClick = { onSelectMedia(movie) },
                            modifier = Modifier.width(115.dp)
                        )
                    }
                }
            }
        }
    }

    // External Player Bottom Sheet
    externalPlayerItem?.let { item ->
        ExternalPlayerMenuSheet(
            streamUrl = item.streamUrl,
            title = item.name,
            lang = lang,
            onDismiss = { externalPlayerItem = null }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = NetflixWhite,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun ContinueWatchingCard(
    item: WatchHistoryItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = NetflixCardBg),
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                AsyncImage(
                    model = item.posterUrl.ifBlank { R.drawable.img_hero_banner },
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NetflixBlack.copy(alpha = 0.65f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = NetflixWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Progress Bar at bottom
                val progress = if (item.durationMs > 0) {
                    (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0.5f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = NetflixRed,
                    trackColor = NetflixDarkText
                )
            }

            Text(
                text = item.name,
                color = NetflixWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun LiveChannelCard(
    item: MediaItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = NetflixCardBg),
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(NetflixDarkGrey),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
            }

            Text(
                text = item.name,
                color = NetflixWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NetflixRed)
                )
                Text(
                    text = "LIVE",
                    color = NetflixRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
