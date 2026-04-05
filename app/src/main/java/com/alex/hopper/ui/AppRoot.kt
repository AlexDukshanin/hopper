package com.alex.hopper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import com.alex.hopper.settings.AppSettings
import com.alex.hopper.ui.screens.CameraScreen
import com.alex.hopper.ui.screens.CollectionsScreen
import com.alex.hopper.ui.screens.EntryDetailScreen
import com.alex.hopper.ui.screens.JournalScreen
import com.alex.hopper.ui.screens.PhotoScreen
import com.alex.hopper.ui.screens.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun XdwApp(
    viewModel: MainViewModel,
    settings: AppSettings,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val currentCollection by viewModel.currentCollection.collectAsStateWithLifecycle()
    val selectedCollectionId by viewModel.selectedCollectionId.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute == AppRoute.Collections.route ||
        currentRoute == AppRoute.Journal.route ||
        currentRoute == AppRoute.Camera.route ||
        currentRoute == AppRoute.Settings.route

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AppEvent.OpenCollection -> {
                    navController.navigate(AppRoute.Journal.createRoute(event.collectionId))
                }

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
                            SnackbarResult.ActionPerformed -> viewModel.restorePendingDelete(event.entryId)
                            SnackbarResult.Dismissed -> viewModel.confirmPendingDelete(event.entryId)
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
                    canOpenCamera = selectedCollectionId != null,
                    onNavigateHome = {
                        navController.navigate(AppRoute.Collections.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateCamera = {
                        selectedCollectionId?.let { collectionId ->
                            navController.navigate(AppRoute.Camera.createRoute(collectionId)) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onNavigateSettings = {
                        navController.navigate(AppRoute.Settings.route) {
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
            startDestination = AppRoute.Collections.route,
        ) {
            composable(AppRoute.Collections.route) {
                CollectionsScreen(
                    collections = collections,
                    selectedCollectionId = selectedCollectionId,
                    layoutMode = settings.collectionLayoutMode,
                    contentPadding = contentPadding,
                    onOpenCollection = { collectionId ->
                        viewModel.selectCollection(collectionId)
                        navController.navigate(AppRoute.Journal.createRoute(collectionId))
                    },
                    onCreateCollection = viewModel::createCollection,
                    onRenameCollection = viewModel::renameCollection,
                    onDeleteCollection = viewModel::deleteCollection,
                    onLayoutModeChange = viewModel::setCollectionLayoutMode,
                )
            }

            composable(
                route = AppRoute.Journal.route,
                arguments = listOf(
                    navArgument(AppRoute.Journal.collectionIdArg) {
                        type = NavType.LongType
                    },
                ),
            ) { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getLong(AppRoute.Journal.collectionIdArg)
                    ?: return@composable

                LaunchedEffect(collectionId) {
                    viewModel.selectCollection(collectionId)
                }

                val activeCollection = currentCollection?.takeIf { it.id == collectionId }
                JournalScreen(
                    collection = activeCollection,
                    entries = if (activeCollection != null) entries else emptyList(),
                    settings = settings,
                    contentPadding = contentPadding,
                    onGoHome = {
                        navController.navigate(AppRoute.Collections.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenCamera = { navController.navigate(AppRoute.Camera.createRoute(collectionId)) },
                    onOpenEntry = { navController.navigate(AppRoute.Detail.createRoute(it)) },
                    onOpenPhoto = { navController.navigate(AppRoute.Photo.createRoute(it)) },
                    onUpdateNumber = viewModel::updatePrimaryNumber,
                    onDeleteEntry = { viewModel.requestDelete(it, popBack = false) },
                    onDeletePhoto = viewModel::deletePhoto,
                    onReplacePhoto = { navController.navigate(AppRoute.ReplacePhoto.createRoute(it)) },
                    onToggleDirection = viewModel::toggleDirection,
                    onUpdateDirections = viewModel::updateDirectionLabels,
                    onSaveNote = viewModel::saveNote,
                    onSaveJournalDescription = viewModel::updateJournalDescription,
                    onTogglePhotoVisibility = viewModel::setShowPhotosInJournal,
                    onMoveEntry = viewModel::moveEntry,
                    onCopied = { viewModel.showSnackbar("Скопировано", durationMillis = 1_000) },
                )
            }

            composable(
                route = AppRoute.Camera.route,
                arguments = listOf(
                    navArgument(AppRoute.Camera.collectionIdArg) {
                        type = NavType.LongType
                    },
                ),
            ) { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getLong(AppRoute.Camera.collectionIdArg)
                    ?: return@composable

                LaunchedEffect(collectionId) {
                    viewModel.selectCollection(collectionId)
                }

                CameraScreen(
                    viewModel = viewModel,
                    contentPadding = contentPadding,
                    replaceEntryId = null,
                    collectionId = collectionId,
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
                    collectionId = null,
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
    canOpenCamera: Boolean,
    onNavigateHome: () -> Unit,
    onNavigateCamera: () -> Unit,
    onNavigateSettings: () -> Unit,
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
            IconButton(onClick = onNavigateHome) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = "Подборки",
                    tint = if (currentRoute == AppRoute.Collections.route) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            FilledTonalIconButton(
                onClick = onNavigateCamera,
                enabled = canOpenCamera,
                modifier = Modifier.size(58.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = "Съемка",
                )
            }

            IconButton(onClick = onNavigateSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Настройки",
                    tint = if (currentRoute == AppRoute.Settings.route) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
