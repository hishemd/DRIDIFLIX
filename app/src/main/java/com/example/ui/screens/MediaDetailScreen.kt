package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.EpisodeItem
import com.example.data.model.MediaItem
import com.example.data.model.StreamType
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.ExternalPlayerMenuSheet
import com.example.ui.theme.*
import com.example.util.ExternalPlayerHelper
import com.example.util.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    uiState: UiState,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val lang = uiState.settings.language
    val item = uiState.selectedMedia ?: return
    val seriesDetail = uiState.selectedSeriesDetail
    val context = LocalContext.current

    var isExternalPlayerSheetVisible by remember { mutableStateOf(false) }
    var externalStreamUrl by remember { mutableStateOf(item.streamUrl) }
    var externalStreamTitle by remember { mutableStateOf(item.name) }

    val isFav = remember(uiState.favorites, item) {
        uiState.favorites.any { it.streamId == item.streamId }
    }

    Scaffold(
        containerColor = NetflixBlack,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NetflixBlack.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = NetflixWhite
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(item) },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NetflixBlack.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = Strings.get("favorites", lang),
                            tint = if (isFav) NetflixRed else NetflixWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp)
        ) {
            // Backdrop Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    val backdropImage = seriesDetail?.backdropUrl?.ifBlank { item.posterUrl } ?: item.posterUrl
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(backdropImage.ifBlank { R.drawable.img_hero_banner })
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        NetflixBlack.copy(alpha = 0.4f),
                                        Color.Transparent,
                                        NetflixBlack.copy(alpha = 0.85f),
                                        NetflixBlack
                                    )
                                )
                            )
                    )

                    // Floating Poster
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .width(90.dp)
                                .aspectRatio(0.68f)
                                .border(1.dp, NetflixCardBorder, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = item.posterUrl.ifBlank { R.drawable.img_hero_banner },
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.name,
                                color = NetflixWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (item.year.isNotBlank()) {
                                    Text(
                                        text = item.year,
                                        color = NetflixGrey,
                                        fontSize = 12.sp
                                    )
                                }
                                if (item.rating.isNotBlank() && item.rating != "0") {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = NetflixGold,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = item.rating.take(3),
                                            color = NetflixWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (item.genre.isNotBlank()) {
                                    Text(
                                        text = "• ${item.genre.split("/").firstOrNull()?.trim() ?: item.genre}",
                                        color = NetflixGrey,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Primary Action Buttons Row
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Main Play Button (for VOD and LIVE)
                    if (item.streamType != StreamType.SERIES) {
                        Button(
                            onClick = {
                                viewModel.playStream(
                                    streamUrl = item.streamUrl,
                                    title = item.name,
                                    streamType = item.streamType,
                                    streamId = item.streamId,
                                    posterUrl = item.posterUrl
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NetflixWhite),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("details_play_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    }

                    // Secondary Buttons: Open in External Player, Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                externalStreamUrl = item.streamUrl
                                externalStreamTitle = item.name
                                isExternalPlayerSheetVisible = true
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = NetflixCardBg),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("details_external_player_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = NetflixRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = Strings.get("open_external", lang),
                                    color = NetflixWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                ExternalPlayerHelper.shareStream(context, item.streamUrl, item.name)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = NetflixCardBg),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = Strings.get("share_stream", lang),
                                tint = NetflixWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Synopsis / Plot
            val plotText = seriesDetail?.plot?.ifBlank { item.plot } ?: item.plot
            if (plotText.isNotBlank()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = Strings.get("plot", lang),
                            color = NetflixWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = plotText,
                            color = NetflixOffWhite,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Cast & Director
            val castText = seriesDetail?.cast?.ifBlank { item.cast } ?: item.cast
            if (castText.isNotBlank()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = Strings.get("cast", lang),
                            color = NetflixWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = castText,
                            color = NetflixGrey,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Series Seasons and Episodes
            if (item.streamType == StreamType.SERIES && seriesDetail != null) {
                // Season Selector Tabs
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = Strings.get("episodes", lang),
                        color = NetflixWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    if (seriesDetail.seasons.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(seriesDetail.seasons) { seasonNum ->
                                val isSelected = uiState.selectedSeason == seasonNum
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectSeason(seasonNum) },
                                    label = { Text("${Strings.get("season", lang)} $seasonNum") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NetflixRed,
                                        selectedLabelColor = NetflixWhite,
                                        containerColor = NetflixCardBg,
                                        labelColor = NetflixGrey
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }

                // Episodes list
                val episodesForSeason = seriesDetail.episodes[uiState.selectedSeason] ?: emptyList()
                items(episodesForSeason, key = { it.id }) { ep ->
                    EpisodeRowItem(
                        episode = ep,
                        lang = lang,
                        onPlay = {
                            viewModel.playStream(
                                streamUrl = ep.streamUrl,
                                title = "${item.name} - S${ep.seasonNum}E${ep.episodeNum}",
                                streamType = StreamType.SERIES,
                                streamId = ep.id,
                                posterUrl = ep.posterUrl.ifBlank { item.posterUrl }
                            )
                        },
                        onOpenExternal = {
                            externalStreamUrl = ep.streamUrl
                            externalStreamTitle = "${item.name} - S${ep.seasonNum}E${ep.episodeNum}"
                            isExternalPlayerSheetVisible = true
                        }
                    )
                }
            }
        }
    }

    if (isExternalPlayerSheetVisible) {
        ExternalPlayerMenuSheet(
            streamUrl = externalStreamUrl,
            title = externalStreamTitle,
            lang = lang,
            onDismiss = { isExternalPlayerSheetVisible = false }
        )
    }
}

@Composable
fun EpisodeRowItem(
    episode: EpisodeItem,
    lang: String,
    onPlay: () -> Unit,
    onOpenExternal: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = NetflixCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NetflixDarkGrey),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = NetflixWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    color = NetflixWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (episode.duration.isNotBlank()) {
                    Text(
                        text = episode.duration,
                        color = NetflixDarkText,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onOpenExternal,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = Strings.get("open_external", lang),
                    tint = NetflixGrey,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
