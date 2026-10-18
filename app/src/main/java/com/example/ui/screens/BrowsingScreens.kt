package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.data.model.StreamType
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.components.CategoryFilterChips
import com.example.ui.components.ExternalPlayerMenuSheet
import com.example.ui.components.MediaPosterCard
import com.example.ui.theme.*
import com.example.util.Strings

// ================= LIVE TV SCREEN =================
@Composable
fun LiveTvScreen(
    uiState: UiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val lang = uiState.settings.language
    var searchQuery by remember { mutableStateOf("") }
    var externalPlayerItem by remember { mutableStateOf<MediaItem?>(null) }

    val filteredChannels = remember(uiState.liveChannels, uiState.selectedLiveCategoryId, searchQuery) {
        uiState.liveChannels.filter { channel ->
            val matchCat = uiState.selectedLiveCategoryId.isEmpty() ||
                    uiState.selectedLiveCategoryId == "all" ||
                    channel.categoryId == uiState.selectedLiveCategoryId ||
                    channel.categoryName.equals(uiState.selectedLiveCategoryId, ignoreCase = true)

            val matchQuery = searchQuery.isBlank() || channel.name.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(Strings.get("search_hint", lang), color = NetflixGrey) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NetflixGrey) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = NetflixGrey)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NetflixRed,
                unfocusedBorderColor = NetflixCardBorder,
                focusedTextColor = NetflixWhite,
                unfocusedTextColor = NetflixWhite,
                focusedContainerColor = NetflixCardBg,
                unfocusedContainerColor = NetflixCardBg
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("live_search_bar")
        )

        // Categories Filter
        CategoryFilterChips(
            categories = uiState.liveCategories,
            selectedCategoryId = uiState.selectedLiveCategoryId,
            onSelectCategory = { viewModel.selectLiveCategory(it) },
            lang = lang
        )

        // Channel Count Summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredChannels.size} ${Strings.get("channels_count", lang)}",
                color = NetflixGrey,
                fontSize = 12.sp
            )
        }

        // Channels Grid
        if (filteredChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Strings.get("no_results", lang),
                    color = NetflixGrey,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredChannels, key = { it.id }) { channel ->
                    LiveChannelGridItem(
                        item = channel,
                        lang = lang,
                        onPlay = {
                            viewModel.playStream(
                                streamUrl = channel.streamUrl,
                                title = channel.name,
                                streamType = StreamType.LIVE,
                                streamId = channel.streamId,
                                posterUrl = channel.posterUrl
                            )
                        },
                        onOpenExternal = { externalPlayerItem = channel },
                        onToggleFavorite = { viewModel.toggleFavorite(channel) },
                        isFavorite = uiState.favorites.any { it.streamId == channel.streamId }
                    )
                }
            }
        }
    }

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
fun LiveChannelGridItem(
    item: MediaItem,
    lang: String,
    onPlay: () -> Unit,
    onOpenExternal: () -> Unit,
    onToggleFavorite: () -> Unit,
    isFavorite: Boolean
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = NetflixCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NetflixDarkGrey),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                }

                Row {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) NetflixRed else NetflixGrey,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenExternal,
                        modifier = Modifier.size(32.dp)
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

            Text(
                text = item.name,
                color = NetflixWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
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
                if (item.categoryName.isNotBlank()) {
                    Text(
                        text = "• ${item.categoryName}",
                        color = NetflixDarkText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ================= MOVIES SCREEN =================
@Composable
fun MoviesScreen(
    uiState: UiState,
    viewModel: MainViewModel,
    onSelectMedia: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = uiState.settings.language
    var searchQuery by remember { mutableStateOf("") }

    val filteredMovies = remember(uiState.movies, uiState.selectedMovieCategoryId, searchQuery) {
        uiState.movies.filter { movie ->
            val matchCat = uiState.selectedMovieCategoryId.isEmpty() ||
                    uiState.selectedMovieCategoryId == "all" ||
                    movie.categoryId == uiState.selectedMovieCategoryId ||
                    movie.categoryName.equals(uiState.selectedMovieCategoryId, ignoreCase = true)

            val matchQuery = searchQuery.isBlank() || movie.name.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(Strings.get("search_hint", lang), color = NetflixGrey) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NetflixGrey) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = NetflixGrey)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NetflixRed,
                unfocusedBorderColor = NetflixCardBorder,
                focusedTextColor = NetflixWhite,
                unfocusedTextColor = NetflixWhite,
                focusedContainerColor = NetflixCardBg,
                unfocusedContainerColor = NetflixCardBg
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("movies_search_bar")
        )

        // Categories Filter
        CategoryFilterChips(
            categories = uiState.movieCategories,
            selectedCategoryId = uiState.selectedMovieCategoryId,
            onSelectCategory = { viewModel.selectMovieCategory(it) },
            lang = lang
        )

        // Summary
        Text(
            text = "${filteredMovies.size} ${Strings.get("movies_count", lang)}",
            color = NetflixGrey,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (filteredMovies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Strings.get("no_results", lang),
                    color = NetflixGrey,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredMovies, key = { it.id }) { movie ->
                    MediaPosterCard(
                        item = movie,
                        onClick = { onSelectMedia(movie) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ================= SERIES SCREEN =================
@Composable
fun SeriesScreen(
    uiState: UiState,
    viewModel: MainViewModel,
    onSelectMedia: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = uiState.settings.language
    var searchQuery by remember { mutableStateOf("") }

    val filteredSeries = remember(uiState.seriesList, uiState.selectedSeriesCategoryId, searchQuery) {
        uiState.seriesList.filter { series ->
            val matchCat = uiState.selectedSeriesCategoryId.isEmpty() ||
                    uiState.selectedSeriesCategoryId == "all" ||
                    series.categoryId == uiState.selectedSeriesCategoryId ||
                    series.categoryName.equals(uiState.selectedSeriesCategoryId, ignoreCase = true)

            val matchQuery = searchQuery.isBlank() || series.name.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(Strings.get("search_hint", lang), color = NetflixGrey) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NetflixGrey) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = NetflixGrey)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NetflixRed,
                unfocusedBorderColor = NetflixCardBorder,
                focusedTextColor = NetflixWhite,
                unfocusedTextColor = NetflixWhite,
                focusedContainerColor = NetflixCardBg,
                unfocusedContainerColor = NetflixCardBg
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("series_search_bar")
        )

        // Categories Filter
        CategoryFilterChips(
            categories = uiState.seriesCategories,
            selectedCategoryId = uiState.selectedSeriesCategoryId,
            onSelectCategory = { viewModel.selectSeriesCategory(it) },
            lang = lang
        )

        // Summary
        Text(
            text = "${filteredSeries.size} ${Strings.get("series_count", lang)}",
            color = NetflixGrey,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (filteredSeries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Strings.get("no_results", lang),
                    color = NetflixGrey,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSeries, key = { it.id }) { series ->
                    MediaPosterCard(
                        item = series,
                        onClick = { onSelectMedia(series) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
