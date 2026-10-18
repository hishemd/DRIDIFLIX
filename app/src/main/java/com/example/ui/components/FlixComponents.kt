package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.CategoryItem
import com.example.data.model.ExternalPlayerType
import com.example.data.model.MediaItem
import com.example.data.model.StreamType
import com.example.ui.MainTab
import com.example.ui.theme.*
import com.example.util.ExternalPlayerHelper
import com.example.util.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlixTopBar(
    title: String,
    profileName: String,
    lang: String,
    onSearchClick: (() -> Unit)? = null,
    onProfileClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Red glowing logo text
                Text(
                    text = "DRIDI",
                    color = NetflixRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "FLIX",
                    color = NetflixWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp
                )

                if (profileName.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NetflixCardBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = profileName,
                            color = NetflixGrey,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        actions = {
            if (onSearchClick != null) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = Strings.get("search_hint", lang),
                        tint = NetflixWhite
                    )
                }
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NetflixRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = Strings.get("profiles", lang),
                        tint = NetflixWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = NetflixBlack.copy(alpha = 0.95f),
            titleContentColor = NetflixWhite
        )
    )
}

@Composable
fun FlixBottomNav(
    currentTab: MainTab,
    lang: String,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = NetflixBlack,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier.fillMaxWidth()
    ) {
        val tabs = listOf(
            MainTab.HOME to (Icons.Filled.Home to Icons.Outlined.Home),
            MainTab.LIVE to (Icons.Filled.Tv to Icons.Outlined.Tv),
            MainTab.MOVIES to (Icons.Filled.Movie to Icons.Outlined.Movie),
            MainTab.SERIES to (Icons.Filled.VideoLibrary to Icons.Outlined.VideoLibrary),
            MainTab.SETTINGS to (Icons.Filled.Settings to Icons.Outlined.Settings)
        )

        tabs.forEach { (tab, icons) ->
            val isSelected = currentTab == tab
            val label = when (tab) {
                MainTab.HOME -> Strings.get("home", lang)
                MainTab.LIVE -> Strings.get("live_tv", lang)
                MainTab.MOVIES -> Strings.get("movies", lang)
                MainTab.SERIES -> Strings.get("series", lang)
                MainTab.SETTINGS -> Strings.get("settings", lang)
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) icons.first else icons.second,
                        contentDescription = label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NetflixRed,
                    selectedTextColor = NetflixRed,
                    unselectedIconColor = NetflixGrey,
                    unselectedTextColor = NetflixGrey,
                    indicatorColor = NetflixRed.copy(alpha = 0.15f)
                )
            )
        }
    }
}

@Composable
fun MediaPosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 0.68f
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = NetflixCardBg),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
        ) {
            // Poster Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.posterUrl.ifBlank { "android.resource://com.example/drawable/img_hero_banner" })
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient at bottom for title readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, NetflixBlack.copy(alpha = 0.9f)),
                            startY = 100f
                        )
                    )
            )

            // Live badge or Rating badge
            if (item.streamType == StreamType.LIVE) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NetflixRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE",
                        color = NetflixWhite,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (item.rating.isNotBlank() && item.rating != "0") {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NetflixBlack.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = NetflixGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = item.rating.take(3),
                            color = NetflixWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Title at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = item.name,
                    color = NetflixWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CategoryFilterChips(
    categories: List<CategoryItem>,
    selectedCategoryId: String,
    onSelectCategory: (String) -> Unit,
    lang: String
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            val isAll = selectedCategoryId.isEmpty() || selectedCategoryId == "all"
            FilterChip(
                selected = isAll,
                onClick = { onSelectCategory("all") },
                label = { Text(Strings.get("all_categories", lang)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NetflixRed,
                    selectedLabelColor = NetflixWhite,
                    containerColor = NetflixCardBg,
                    labelColor = NetflixGrey
                ),
                border = null,
                shape = RoundedCornerShape(20.dp)
            )
        }

        items(categories) { category ->
            if (category.id != "all") {
                val isSelected = selectedCategoryId == category.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectCategory(category.id) },
                    label = { Text(category.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NetflixRed,
                        selectedLabelColor = NetflixWhite,
                        containerColor = NetflixCardBg,
                        labelColor = NetflixGrey
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalPlayerMenuSheet(
    streamUrl: String,
    title: String,
    lang: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NetflixDarkGrey,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NetflixGrey) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = Strings.get("select_player", lang),
                color = NetflixWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                color = NetflixGrey,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(color = NetflixCardBorder, thickness = 1.dp)

            // VLC Option
            PlayerOptionRow(
                icon = Icons.Default.PlayCircle,
                iconTint = Color(0xFFFF9800),
                title = Strings.get("vlc_player", lang),
                subtitle = "org.videolan.vlc"
            ) {
                ExternalPlayerHelper.openVlc(context, streamUrl, title)
                onDismiss()
            }

            // MX Player Option
            PlayerOptionRow(
                icon = Icons.Default.SmartDisplay,
                iconTint = Color(0xFF2196F3),
                title = Strings.get("mx_player", lang),
                subtitle = "com.mxtech.videoplayer"
            ) {
                ExternalPlayerHelper.openMxPlayer(context, streamUrl, title)
                onDismiss()
            }

            // Just Player Option
            PlayerOptionRow(
                icon = Icons.Default.PlayArrow,
                iconTint = NetflixGreen,
                title = Strings.get("just_player", lang),
                subtitle = "com.brouken.player"
            ) {
                ExternalPlayerHelper.openJustPlayer(context, streamUrl, title)
                onDismiss()
            }

            // System Chooser Option
            PlayerOptionRow(
                icon = Icons.Default.OpenInNew,
                iconTint = NetflixWhite,
                title = Strings.get("other_player", lang),
                subtitle = "video/*"
            ) {
                ExternalPlayerHelper.openSystemChooser(context, streamUrl, title)
                onDismiss()
            }

            // Copy Link
            PlayerOptionRow(
                icon = Icons.Default.ContentCopy,
                iconTint = NetflixGrey,
                title = Strings.get("copy_link", lang),
                subtitle = streamUrl.take(45) + "..."
            ) {
                ExternalPlayerHelper.copyToClipboard(context, streamUrl, Strings.get("link_copied", lang))
                onDismiss()
            }

            // Share Stream
            PlayerOptionRow(
                icon = Icons.Default.Share,
                iconTint = NetflixGrey,
                title = Strings.get("share_stream", lang),
                subtitle = ""
            ) {
                ExternalPlayerHelper.shareStream(context, streamUrl, title)
                onDismiss()
            }
        }
    }
}

@Composable
private fun PlayerOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(NetflixCardBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = NetflixWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    color = NetflixDarkText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = NetflixGrey,
            modifier = Modifier.size(20.dp)
        )
    }
}
