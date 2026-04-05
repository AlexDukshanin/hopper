package com.alex.xdw.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alex.xdw.settings.AppSettings
import com.alex.xdw.ui.screens.CameraScreen
import com.alex.xdw.ui.screens.EntryDetailScreen
import com.alex.xdw.ui.screens.JournalScreen
import com.alex.xdw.ui.screens.PhotoScreen
import com.alex.xdw.ui.screens.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun XdwApp(
    viewModel: MainViewModel,
    settings: AppSettings,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute == AppRoute.Journal.route ||
        currentRoute == AppRoute.Camera.route ||
        currentRoute == AppRoute.Settings.route

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AppEvent.OpenEntry -> {
                    navController.navigate(AppRoute.Detail.createRoute(event.entryId))
                }

                is AppEvent.PhotoUpdated -> {
                    navController.popBackStack()
                    snackbarHostState.showSnackbar(event.message)
                }

                is AppEvent.Snackbar -> {
                    launch {
                        val dismissJob = launch {
                            delay(event.durationMillis)
                            snackbarHostState.currentSnackbarData?.dismiss()
                        }
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Indefinite,
                            withDismissAction = false,
                        )
                        dismissJob.cancel()
                    }
                }

                is AppEvent.PendingDelete -> {
                    if (event.popBack) {
                        navController.popBackStack()
                    }
                    launch {
                        val dismissJob = launch {
                            delay(5_000)
                            snackbarHostState.currentSnackbarData?.dismiss()
                        }
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = "Вернуть",
                            duration = SnackbarDuration.Indefinite,
                            withDismissAction = false,
                        )
                        dismissJob.cancel()
                        when (result) {
                            SnackbarResult.ActionPerformed -> {
                                viewModel.restorePendingDelete(event.entryId)
                            }

                            SnackbarResult.Dismissed -> {
                                viewModel.confirmPendingDelete(event.entryId)
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                MainBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Journal.route,
        ) {
            composable(AppRoute.Journal.route) {
                JournalScreen(
                    entries = entries,
                    settings = settings,
                    contentPadding = contentPadding,
                    onOpenCamera = { navController.navigate(AppRoute.Camera.route) },
                    onOpenEntry = { navController.navigate(AppRoute.Detail.createRoute(it)) },
                    onOpenPhoto = { navController.navigate(AppRoute.Photo.createRoute(it)) },
                    onUpdateNumber = viewModel::updatePrimaryNumber,
                    onDeleteEntry = { viewModel.requestDelete(it, popBack = false) },
                    onDeletePhoto = viewModel::deletePhoto,
                    onReplacePhoto = {
                        navController.navigate(AppRoute.ReplacePhoto.createRoute(it))
                    },
                    onToggleDirection = viewModel::toggleDirection,
                    onUpdateDirections = viewModel::updateDirectionLabels,
                    onSaveNote = viewModel::saveNote,
                    onSaveJournalDescription = viewModel::updateJournalDescription,
                    onTogglePhotoVisibility = viewModel::setShowPhotosInJournal,
                    onMoveEntry = viewModel::moveEntry,
                    onCopied = { viewModel.showSnackbar("Скопировано", durationMillis = 1_000) },
                )
            }

            composable(AppRoute.Camera.route) {
                CameraScreen(
                    viewModel = viewModel,
                    contentPadding = contentPadding,
                    replaceEntryId = null,
                )
            }

            composable(
                route = AppRoute.ReplacePhoto.route,
                arguments = listOf(
                    navArgument(AppRoute.ReplacePhoto.entryIdArg) {
                        type = NavType.LongType
                    },
                ),
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getLong(AppRoute.ReplacePhoto.entryIdArg)
                    ?: return@composable
                CameraScreen(
                    viewModel = viewModel,
                    contentPadding = contentPadding,
                    replaceEntryId = entryId,
                )
            }

            composable(AppRoute.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    contentPadding = contentPadding,
                    onSelectTheme = viewModel::setThemeMode,
                    onSelectAppIcon = viewModel::setAppIconMode,
                    onNumberSizeChange = viewModel::setNumberFontSize,
                    onNewEntryPositionChange = viewModel::setNewEntryPosition,
                    onIncludeDirectionInCopyChange = viewModel::setIncludeDirectionInCopy,
                )
            }

            composable(
                route = AppRoute.Detail.route,
                arguments = listOf(
                    navArgument(AppRoute.Detail.entryIdArg) {
                        type = NavType.LongType
                    },
                ),
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getLong(AppRoute.Detail.entryIdArg) ?: return@composable
                EntryDetailScreen(
                    entryId = entryId,
                    viewModel = viewModel,
                    contentPadding = contentPadding,
                    onBack = { navController.popBackStack() },
                    onOpenPhoto = { navController.navigate(AppRoute.Photo.createRoute(it)) },
                )
            }

            composable(
                route = AppRoute.Photo.route,
                arguments = listOf(
                    navArgument(AppRoute.Photo.entryIdArg) {
                        type = NavType.LongType
                    },
                ),
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getLong(AppRoute.Photo.entryIdArg) ?: return@composable
                PhotoScreen(
                    entryId = entryId,
                    viewModel = viewModel,
                    contentPadding = contentPadding,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun MainBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onNavigate(AppRoute.Journal.route) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ViewList,
                    contentDescription = "Журнал",
                    tint = if (currentRoute == AppRoute.Journal.route) {
                        androidx.compose.material3.MaterialTheme.colorScheme.primary
                    } else {
                        androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            FilledTonalIconButton(
                onClick = { onNavigate(AppRoute.Camera.route) },
                modifier = Modifier.size(58.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = "Съемка",
                )
            }

            IconButton(onClick = { onNavigate(AppRoute.Settings.route) }) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Настройки",
                    tint = if (currentRoute == AppRoute.Settings.route) {
                        androidx.compose.material3.MaterialTheme.colorScheme.primary
                    } else {
                        androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
