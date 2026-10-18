package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExternalPlayerType
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.theme.*
import com.example.util.Strings

@Composable
fun SettingsScreen(
    uiState: UiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val lang = uiState.settings.language
    val user = uiState.userInfo
    val server = uiState.serverInfo
    val profile = uiState.activeProfile

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var proxyInput by remember { mutableStateOf(uiState.settings.corsProxyUrl) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Account & Server Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NetflixCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Strings.get("account_info", lang),
                            color = NetflixWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (user.status.contains("Active", ignoreCase = true)) NetflixGreen.copy(alpha = 0.2f) else NetflixRed.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = user.status,
                                color = if (user.status.contains("Active", ignoreCase = true)) NetflixGreen else NetflixRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = NetflixCardBorder, thickness = 1.dp)

                    InfoRow(label = Strings.get("username", lang), value = user.username.ifBlank { profile?.name ?: "Guest" })
                    InfoRow(label = Strings.get("expiry_date", lang), value = user.expDate.ifBlank { "2030-12-31" })
                    InfoRow(label = Strings.get("active_connections", lang), value = "${user.activeCons} / ${user.maxConnections}")
                    if (server.url.isNotBlank()) {
                        InfoRow(label = Strings.get("server_url", lang), value = server.url)
                    }
                    if (server.timezone.isNotBlank()) {
                        InfoRow(label = "Timezone", value = server.timezone)
                    }
                }
            }
        }

        // Language Switcher
        item {
            SettingsSectionTitle(Strings.get("language_title", lang))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NetflixCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateSettings(uiState.settings.copy(language = "ar"))
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = Strings.get("arabic", lang),
                            color = NetflixWhite,
                            fontSize = 14.sp
                        )
                        RadioButton(
                            selected = lang == "ar",
                            onClick = { viewModel.updateSettings(uiState.settings.copy(language = "ar")) },
                            colors = RadioButtonDefaults.colors(selectedColor = NetflixRed)
                        )
                    }

                    HorizontalDivider(color = NetflixCardBorder, thickness = 0.8.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateSettings(uiState.settings.copy(language = "en"))
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = Strings.get("english", lang),
                            color = NetflixWhite,
                            fontSize = 14.sp
                        )
                        RadioButton(
                            selected = lang == "en",
                            onClick = { viewModel.updateSettings(uiState.settings.copy(language = "en")) },
                            colors = RadioButtonDefaults.colors(selectedColor = NetflixRed)
                        )
                    }
                }
            }
        }

        // Default Player Selection
        item {
            SettingsSectionTitle(Strings.get("default_player_title", lang))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NetflixCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    val players = listOf(
                        ExternalPlayerType.BUILT_IN to Strings.get("player_builtin", lang),
                        ExternalPlayerType.VLC to Strings.get("vlc_player", lang),
                        ExternalPlayerType.MX_PLAYER to Strings.get("mx_player", lang),
                        ExternalPlayerType.JUST_PLAYER to Strings.get("just_player", lang),
                        ExternalPlayerType.SYSTEM to Strings.get("other_player", lang)
                    )

                    players.forEachIndexed { index, (type, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(uiState.settings.copy(defaultPlayer = type))
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                color = NetflixWhite,
                                fontSize = 13.sp
                            )
                            RadioButton(
                                selected = uiState.settings.defaultPlayer == type,
                                onClick = { viewModel.updateSettings(uiState.settings.copy(defaultPlayer = type)) },
                                colors = RadioButtonDefaults.colors(selectedColor = NetflixRed)
                            )
                        }
                        if (index < players.size - 1) {
                            HorizontalDivider(color = NetflixCardBorder, thickness = 0.8.dp)
                        }
                    }
                }
            }
        }

        // Parental Control (PIN)
        item {
            SettingsSectionTitle(Strings.get("parental_control", lang))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NetflixCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Strings.get("parental_pin_set", lang),
                            color = NetflixWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (uiState.settings.parentalLockEnabled) "مفعل (PIN: ****)" else "معطل",
                            color = NetflixGrey,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = uiState.settings.parentalLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showPinDialog = true
                            } else {
                                viewModel.setParentalPin("", false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NetflixWhite,
                            checkedTrackColor = NetflixRed
                        )
                    )
                }
            }
        }

        // CORS Proxy URL
        item {
            SettingsSectionTitle(Strings.get("cors_proxy_title", lang))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NetflixCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proxyInput,
                        onValueChange = { proxyInput = it },
                        placeholder = { Text(Strings.get("cors_proxy_hint", lang), color = NetflixDarkText) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NetflixRed,
                            unfocusedBorderColor = NetflixCardBorder,
                            focusedTextColor = NetflixWhite,
                            unfocusedTextColor = NetflixWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            viewModel.updateSettings(uiState.settings.copy(corsProxyUrl = proxyInput))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(Strings.get("confirm", lang), color = NetflixWhite)
                    }
                }
            }
        }

        // Actions: Clear Cache, Switch Profile, Logout
        item {
            SettingsSectionTitle("إدارة التطبيق والحساب")
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NetflixCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Clear Cache
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.clearCache() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = NetflixGrey)
                        Text(Strings.get("clear_cache", lang), color = NetflixWhite, fontSize = 14.sp)
                    }

                    HorizontalDivider(color = NetflixCardBorder, thickness = 0.8.dp)

                    // Switch / Add Profile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setScreen(AppScreen.LOGIN) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.SwitchAccount, contentDescription = null, tint = NetflixGrey)
                        Text(Strings.get("switch_profile", lang), color = NetflixWhite, fontSize = 14.sp)
                    }

                    HorizontalDivider(color = NetflixCardBorder, thickness = 0.8.dp)

                    // Logout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.logout() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = NetflixRed)
                        Text(Strings.get("logout", lang), color = NetflixRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Version & Credits
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DRIDI FLIX",
                    color = NetflixRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = Strings.get("version", lang),
                    color = NetflixDarkText,
                    fontSize = 11.sp
                )
            }
        }
    }

    // Set PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(Strings.get("parental_control", lang), color = NetflixWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(Strings.get("parental_pin_prompt", lang), color = NetflixGrey, fontSize = 13.sp)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NetflixRed,
                            unfocusedBorderColor = NetflixCardBorder,
                            focusedTextColor = NetflixWhite,
                            unfocusedTextColor = NetflixWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            viewModel.setParentalPin(pinInput, true)
                            showPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)
                ) {
                    Text(Strings.get("confirm", lang), color = NetflixWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(Strings.get("cancel", lang), color = NetflixGrey)
                }
            },
            containerColor = NetflixDarkGrey
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = NetflixGrey,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = NetflixGrey, fontSize = 13.sp)
        Text(text = value, color = NetflixWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
