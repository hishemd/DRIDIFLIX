package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExternalPlayerType
import com.example.ui.AppScreen
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.components.FlixBottomNav
import com.example.ui.components.FlixTopBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NetflixBlack
import com.example.util.ExternalPlayerHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isRtl = uiState.settings.language == "ar"
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            val context = LocalContext.current
            val snackbarHostState = remember { SnackbarHostState() }

            // Handle status toast messages
            LaunchedEffect(uiState.statusMessage) {
                uiState.statusMessage?.let { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearStatusMessage()
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MyApplicationTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NetflixBlack)
                    ) {
                        Crossfade(
                            targetState = uiState.screen,
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                AppScreen.LOGIN -> {
                                    LoginScreen(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        onLoginSuccess = { viewModel.setScreen(AppScreen.MAIN) }
                                    )
                                }

                                AppScreen.PLAYER -> {
                                    BuiltInPlayerScreen(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        onBack = {
                                            viewModel.setScreen(
                                                if (uiState.selectedMedia != null) AppScreen.MEDIA_DETAIL else AppScreen.MAIN
                                            )
                                        }
                                    )
                                }

                                AppScreen.MEDIA_DETAIL -> {
                                    MediaDetailScreen(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        onBack = { viewModel.setScreen(AppScreen.MAIN) }
                                    )
                                }

                                AppScreen.MAIN -> {
                                    MainScaffold(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        snackbarHostState = snackbarHostState
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScaffold(
    uiState: com.example.ui.UiState,
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState
) {
    val lang = uiState.settings.language
    val context = LocalContext.current

    // Back button behavior on main screen
    BackHandler(enabled = uiState.currentTab != MainTab.HOME) {
        viewModel.setTab(MainTab.HOME)
    }

    Scaffold(
        containerColor = NetflixBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FlixTopBar(
                title = "DRIDI FLIX",
                profileName = uiState.activeProfile?.name ?: "",
                lang = lang,
                onProfileClick = { viewModel.setScreen(AppScreen.LOGIN) }
            )
        },
        bottomBar = {
            FlixBottomNav(
                currentTab = uiState.currentTab,
                lang = lang,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState.currentTab) {
                MainTab.HOME -> {
                    HomeScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onSelectMedia = { item ->
                            if (uiState.settings.defaultPlayer != ExternalPlayerType.BUILT_IN && item.streamUrl.isNotBlank()) {
                                ExternalPlayerHelper.openInPlayer(
                                    context = context,
                                    playerType = uiState.settings.defaultPlayer,
                                    streamUrl = item.streamUrl,
                                    title = item.name
                                )
                            } else {
                                viewModel.selectMedia(item)
                            }
                        }
                    )
                }

                MainTab.LIVE -> {
                    LiveTvScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }

                MainTab.MOVIES -> {
                    MoviesScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onSelectMedia = { item ->
                            if (uiState.settings.defaultPlayer != ExternalPlayerType.BUILT_IN && item.streamUrl.isNotBlank()) {
                                ExternalPlayerHelper.openInPlayer(
                                    context = context,
                                    playerType = uiState.settings.defaultPlayer,
                                    streamUrl = item.streamUrl,
                                    title = item.name
                                )
                            } else {
                                viewModel.selectMedia(item)
                            }
                        }
                    )
                }

                MainTab.SERIES -> {
                    SeriesScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onSelectMedia = { item -> viewModel.selectMedia(item) }
                    )
                }

                MainTab.SETTINGS -> {
                    SettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
