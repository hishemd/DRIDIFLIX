package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SourceProfile
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.theme.*
import com.example.util.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    uiState: UiState,
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit
) {
    val lang = uiState.settings.language
    var selectedTab by remember { mutableStateOf(0) } // 0 = Xtream, 1 = M3U

    // Xtream fields
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var profileName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // M3U fields
    var m3uUrl by remember { mutableStateOf("") }
    var m3uContent by remember { mutableStateOf("") }
    var m3uProfileName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = NetflixBlack,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    TextButton(
                        onClick = {
                            val newLang = if (lang == "ar") "en" else "ar"
                            viewModel.updateSettings(uiState.settings.copy(language = newLang))
                        }
                    ) {
                        Text(
                            text = if (lang == "ar") "English" else "العربية",
                            color = NetflixWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Brand Logo & Welcome
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "DRIDI",
                        color = NetflixRed,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FLIX",
                        color = NetflixWhite,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Strings.get("welcome_subtitle", lang),
                    color = NetflixGrey,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Quick Demo Access Banner
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NetflixCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NetflixRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.loginDemo() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NetflixRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = NetflixWhite
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Strings.get("demo_mode", lang),
                                color = NetflixWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Al Jazeera, DW, NASA TV, 4K Movies & Series",
                                color = NetflixGrey,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = NetflixRed
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Tabs: Xtream vs M3U
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = NetflixCardBg,
                    contentColor = NetflixRed,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NetflixRed
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = Strings.get("login_xtream", lang),
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) NetflixWhite else NetflixGrey
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = Strings.get("login_m3u", lang),
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) NetflixWhite else NetflixGrey
                            )
                        }
                    )
                }
            }

            // Error display
            if (uiState.errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NetflixRedDark.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NetflixRed, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = NetflixWhite,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Xtream Form
            if (selectedTab == 0) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text(Strings.get("server_url", lang)) },
                            placeholder = { Text(Strings.get("server_url_hint", lang)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NetflixRed,
                                unfocusedBorderColor = NetflixCardBorder,
                                focusedLabelColor = NetflixRed,
                                unfocusedLabelColor = NetflixGrey,
                                focusedTextColor = NetflixWhite,
                                unfocusedTextColor = NetflixWhite,
                                focusedContainerColor = NetflixCardBg,
                                unfocusedContainerColor = NetflixCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("server_url_input")
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(Strings.get("username", lang)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NetflixRed,
                                unfocusedBorderColor = NetflixCardBorder,
                                focusedLabelColor = NetflixRed,
                                unfocusedLabelColor = NetflixGrey,
                                focusedTextColor = NetflixWhite,
                                unfocusedTextColor = NetflixWhite,
                                focusedContainerColor = NetflixCardBg,
                                unfocusedContainerColor = NetflixCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(Strings.get("password", lang)) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = NetflixGrey
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NetflixRed,
                                unfocusedBorderColor = NetflixCardBorder,
                                focusedLabelColor = NetflixRed,
                                unfocusedLabelColor = NetflixGrey,
                                focusedTextColor = NetflixWhite,
                                unfocusedTextColor = NetflixWhite,
                                focusedContainerColor = NetflixCardBg,
                                unfocusedContainerColor = NetflixCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )

                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            label = { Text("اسم السيرفر / الملف (اختياري)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NetflixRed,
                                unfocusedBorderColor = NetflixCardBorder,
                                focusedLabelColor = NetflixRed,
                                unfocusedLabelColor = NetflixGrey,
                                focusedTextColor = NetflixWhite,
                                unfocusedTextColor = NetflixWhite,
                                focusedContainerColor = NetflixCardBg,
                                unfocusedContainerColor = NetflixCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                if (serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                                    viewModel.loginXtream(serverUrl, username, password, profileName)
                                }
                            },
                            enabled = !uiState.isLoading && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NetflixRed,
                                disabledContainerColor = NetflixRedDark.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_button")
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(color = NetflixWhite, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = Strings.get("login_button", lang),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NetflixWhite
                                )
                            }
                        }
                    }
                }
            } else {
                // M3U Form
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = m3uUrl,
                            onValueChange = { m3uUrl = it },
                            label = { Text(Strings.get("m3u_url", lang)) },
                            placeholder = { Text("http://example.com/playlist.m3u") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NetflixRed,
                                unfocusedBorderColor = NetflixCardBorder,
                                focusedLabelColor = NetflixRed,
                                unfocusedLabelColor = NetflixGrey,
                                focusedTextColor = NetflixWhite,
                                unfocusedTextColor = NetflixWhite,
                                focusedContainerColor = NetflixCardBg,
                                unfocusedContainerColor = NetflixCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = m3uContent,
                            onValueChange = { m3uContent = it },
                            label = { Text(Strings.get("m3u_file_or_text", lang)) },
                            placeholder = { Text("#EXTM3U\n#EXTINF:-1,Channel Name\nhttp://...") },
                            minLines = 3,
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NetflixRed,
                                unfocusedBorderColor = NetflixCardBorder,
                                focusedLabelColor = NetflixRed,
                                unfocusedLabelColor = NetflixGrey,
                                focusedTextColor = NetflixWhite,
                                unfocusedTextColor = NetflixWhite,
                                focusedContainerColor = NetflixCardBg,
                                unfocusedContainerColor = NetflixCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = m3uProfileName,
                            onValueChange = { m3uProfileName = it },
                            label = { Text("اسم القائمة (اختياري)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NetflixRed,
                                unfocusedBorderColor = NetflixCardBorder,
                                focusedLabelColor = NetflixRed,
                                unfocusedLabelColor = NetflixGrey,
                                focusedTextColor = NetflixWhite,
                                unfocusedTextColor = NetflixWhite,
                                focusedContainerColor = NetflixCardBg,
                                unfocusedContainerColor = NetflixCardBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                if (m3uUrl.isNotBlank() || m3uContent.isNotBlank()) {
                                    viewModel.loginM3u(m3uUrl, m3uContent, m3uProfileName)
                                }
                            },
                            enabled = !uiState.isLoading && (m3uUrl.isNotBlank() || m3uContent.isNotBlank()),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NetflixRed,
                                disabledContainerColor = NetflixRedDark.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(color = NetflixWhite, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = Strings.get("login_button", lang),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NetflixWhite
                                )
                            }
                        }
                    }
                }
            }

            // Saved Profiles list
            if (uiState.profiles.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = Strings.get("profiles", lang),
                        color = NetflixWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(uiState.profiles) { profile ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = NetflixCardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.switchProfile(profile) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (profile.type == "XTREAM") Icons.Default.Dns else Icons.Default.FormatListBulleted,
                                contentDescription = null,
                                tint = if (profile.isDemo) NetflixGold else NetflixRed
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.name,
                                    color = NetflixWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (profile.type == "XTREAM") "${profile.username} @ ${profile.serverUrl}" else "M3U Playlist",
                                    color = NetflixDarkText,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                            if (!profile.isDemo) {
                                IconButton(onClick = { viewModel.deleteProfile(profile.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = Strings.get("delete_profile", lang),
                                        tint = NetflixGrey
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
