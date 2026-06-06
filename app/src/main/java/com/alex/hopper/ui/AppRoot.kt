package com.alex.hopper.ui

import android.content.ClipData
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alex.hopper.exchange.CollectionExchangeManager
import com.alex.hopper.settings.AppSettings
import com.alex.hopper.ui.screens.CameraScreen
import com.alex.hopper.ui.screens.CollectionsScreen
import com.alex.hopper.ui.screens.EntryDetailScreen
import com.alex.hopper.ui.screens.JournalScreen
import com.alex.hopper.ui.screens.PhotoScreen
import com.alex.hopper.ui.screens.QrImportScreen
import com.alex.hopper.ui.screens.QrShareScreen
import com.alex.hopper.ui.screens.ScanFrameEditorScreen
import com.alex.hopper.ui.screens.SearchScreen
import com.alex.hopper.ui.screens.SettingsMode
import com.alex.hopper.ui.screens.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SettingsSource {
    Collections,
    Journal,
}

private const val CameraLogTag = "HopperCamera"

@Composable
fun XdwApp(
    viewModel: MainViewModel,
    settings: AppSettings,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val importLauncher = rememberLauncherForActivityResult(
        contract = OpenHopperDocumentContract(),
    ) { uri ->
        if (uri != null) {
            viewModel.importCollectionFromUri(uri)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val selectedCollectionId by viewModel.selectedCollectionId.collectAsStateWithLifecycle()
    val selectedCollection by viewModel.currentCollection.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val qrShareSession by viewModel.qrShareSession.collectAsStateWithLifecycle()
    val pendingImportConflict by viewModel.pendingImportConflict.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentRouteCollectionId = when (currentRoute) {
        AppRoute.Journal.route -> navBackStackEntry?.arguments?.getLong(AppRoute.Journal.collectionIdArg)
        AppRoute.Camera.route -> navBackStackEntry?.arguments?.getLong(AppRoute.Camera.collectionIdArg)
        AppRoute.SearchCollection.route -> navBackStackEntry?.arguments?.getLong(AppRoute.SearchCollection.collectionIdArg)
        else -> null
    }
    var settingsSource by rememberSaveable { mutableStateOf(SettingsSource.Collections) }
    val showJournalActions = currentRoute == AppRoute.Journal.route ||
        currentRoute == AppRoute.Camera.route ||
        currentRoute == AppRoute.SearchCollection.route ||
        (currentRoute == AppRoute.Settings.route && settingsSource == SettingsSource.Journal)
    val bottomBarCollectionId = currentRouteCollectionId
        ?: selectedCollectionId?.takeIf { settingsSource == SettingsSource.Journal }
    val showSearchAction = currentRoute == AppRoute.Collections.route ||
        currentRoute == AppRoute.SearchCollection.route ||
        showJournalActions
    val showImportAction = currentRoute == AppRoute.Collections.route
    val showBottomBar = currentRoute == AppRoute.Collections.route ||
        currentRoute == AppRoute.Journal.route ||
        currentRoute == AppRoute.Camera.route ||
        currentRoute == AppRoute.SearchGlobal.route ||
        currentRoute == AppRoute.SearchCollection.route ||
        currentRoute == AppRoute.Settings.route

    fun navigateToCollectionSection(targetRoute: String) {
        navController.navigate(targetRoute) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AppEvent.OpenEntry -> {
                    navController.navigate(AppRoute.Detail.createRoute(event.entryId))
                }

                is AppEvent.OpenCollection -> {
                    settingsSource = SettingsSource.Journal
                    navController.navigate(AppRoute.Journal.createRoute(event.collectionId)) {
                        popUpTo(AppRoute.Collections.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                    snackbarHostState.showSnackbar(event.message)
                }

                AppEvent.OpenQrShare -> {
                    navController.navigate(AppRoute.ShareQr.route) {
                        launchSingleTop = true
                    }
                }

                is AppEvent.ShareFile -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        clipData = ClipData.newUri(
                            context.contentResolver,
                            event.chooserTitle,
                            event.uri,
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(sendIntent, event.chooserTitle).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
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
                            SnackbarResult.ActionPerformed -> viewModel.restorePendingDelete(event.entryIds)
                            SnackbarResult.Dismissed -> viewModel.confirmPendingDelete(event.entryIds)
                        }
                    }
                }
            }
        }
    }

    pendingImportConflict?.let { conflict ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPendingImportConflict,
            title = {
                Text("Подборка уже существует")
            },
            text = {
                Text(
                    "Подборка \"${conflict.collectionName}\" уже есть в Hopper. " +
                        "Импортировать вместо нее или создать отдельную копию?\n\n" +
                        "В файле: ${conflict.entryCount} ${if (conflict.entryCount == 1) "карточка" else "карточек"}.",
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = viewModel::importCollectionAsCopy) {
                        Text("Копия")
                    }
                    TextButton(onClick = viewModel::dismissPendingImportConflict) {
                        Text("Отмена")
                    }
                    TextButton(onClick = viewModel::replaceImportedCollection) {
                        Text("Заменить")
                    }
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                MainBottomBar(
                    currentRoute = currentRoute,
                    canOpenCamera = bottomBarCollectionId != null,
                    showJournalActions = showJournalActions,
                    showSearchAction = showSearchAction,
                    showImportAction = showImportAction,
                    onNavigateJournal = {
                        bottomBarCollectionId?.let { collectionId ->
                            val previousBackStackEntry = navController.previousBackStackEntry
                            val previousJournalCollectionId = previousBackStackEntry
                                ?.arguments
                                ?.getLong(AppRoute.Journal.collectionIdArg)
                            val cameFromSameJournal = currentRoute == AppRoute.Camera.route &&
                                previousBackStackEntry?.destination?.route == AppRoute.Journal.route &&
                                previousJournalCollectionId == collectionId
                            when {
                                currentRoute == AppRoute.Journal.route &&
                                    currentRouteCollectionId == collectionId -> Unit

                                cameFromSameJournal -> navController.popBackStack()
                                else -> navigateToCollectionSection(
                                    AppRoute.Journal.createRoute(collectionId),
                                )
                            }
                        }
                    },
                    onNavigateCamera = {
                        bottomBarCollectionId?.let { collectionId ->
                            if (
                                currentRoute == AppRoute.Camera.route &&
                                currentRouteCollectionId == collectionId
                            ) {
                                return@let
                            }
                            navigateToCollectionSection(AppRoute.Camera.createRoute(collectionId))
                        }
                    },
                    onNavigateSettings = {
                        settingsSource = when (currentRoute) {
                            AppRoute.Settings.route -> settingsSource
                            else -> if (showJournalActions) {
                                SettingsSource.Journal
                            } else {
                                SettingsSource.Collections
                            }
                        }
                        if (currentRoute == AppRoute.Settings.route) {
                            return@MainBottomBar
                        }
                        navController.navigate(AppRoute.Settings.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateSearch = {
                        if (showJournalActions) {
                            val collectionId = bottomBarCollectionId ?: return@MainBottomBar
                            if (
                                currentRoute == AppRoute.SearchCollection.route &&
                                currentRouteCollectionId == collectionId
                            ) {
                                return@MainBottomBar
                            }
                            navigateToCollectionSection(
                                AppRoute.SearchCollection.createRoute(collectionId),
                            )
                        } else {
                            if (currentRoute == AppRoute.SearchGlobal.route) {
                                return@MainBottomBar
                            }
                            navController.navigate(AppRoute.SearchGlobal.route) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onImportCollection = {
                        importLauncher.launch(CollectionExchangeManager.SUPPORTED_IMPORT_MIME_TYPES)
                    },
                    onImportQr = {
                        navController.navigate(AppRoute.ImportQr.route) {
                            launchSingleTop = true
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

                val collectionFlow = remember(collectionId) {
                    viewModel.observeCollection(collectionId)
                }
                val entriesFlow = remember(collectionId) {
                    viewModel.observeEntries(collectionId)
                }
                val routeCollection by collectionFlow.collectAsStateWithLifecycle(initialValue = null)
                val routeEntries by entriesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                LaunchedEffect(collectionId) {
                    viewModel.selectCollection(collectionId)
                }

                JournalScreen(
                    collection = routeCollection,
                    entries = routeEntries,
                    settings = settings,
                    contentPadding = contentPadding,
                    onGoHome = {
                        val returned = navController.popBackStack(
                            route = AppRoute.Collections.route,
                            inclusive = false,
                        )
                        if (!returned) {
                            navController.navigate(AppRoute.Collections.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onOpenCamera = { navController.navigate(AppRoute.Camera.createRoute(collectionId)) },
                    onOpenEntry = { navController.navigate(AppRoute.Detail.createRoute(it)) },
                    onOpenPhoto = { navController.navigate(AppRoute.Photo.createRoute(it)) },
                    onUpdateNumber = viewModel::updatePrimaryNumber,
                    onUpdateCondition = viewModel::updateCondition,
                    onDeleteEntry = { viewModel.requestDelete(it, popBack = false) },
                    onDeletePhoto = viewModel::deletePhoto,
                    onReplacePhoto = { navController.navigate(AppRoute.ReplacePhoto.createRoute(it)) },
                    onToggleDirection = { viewModel.toggleDirection(collectionId) },
                    onUpdateDirections = { primary, secondary ->
                        viewModel.updateDirectionLabels(collectionId, primary, secondary)
                    },
                    onSaveNote = viewModel::saveNote,
                    onSaveJournalDescription = { description ->
                        viewModel.updateJournalDescription(collectionId, description)
                    },
                    onAddEmptyEntry = { viewModel.createEmptyEntry(collectionId) },
                    onTogglePhotoVisibility = viewModel::setShowPhotosInJournal,
                    onMoveEntry = viewModel::moveEntry,
                    onCopied = { viewModel.showSnackbar("Скопировано", durationMillis = 1_000) },
                    onDeleteEntries = viewModel::requestDeleteEntries,
                    onShareCollectionFile = { includePhotos ->
                        viewModel.shareCollectionFile(collectionId, includePhotos)
                    },
                    onShareCollectionQr = {
                        viewModel.prepareCollectionQrShare(collectionId)
                    },
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
                    jpegQuality = settings.photoQualityJpeg,
                    scanFrameSettings = settings.scanFrameSettings,
                    onOpenScanFrameEditor = {
                        Log.d(CameraLogTag, "Navigate to scan frame editor from capture camera. collectionId=$collectionId")
                        navController.navigate(AppRoute.ScanFrameEditor.route)
                    },
                )
            }

            composable(AppRoute.SearchGlobal.route) {
                SearchScreen(
                    isGlobalSearch = true,
                    collections = collections,
                    entries = allEntries,
                    currentCollection = null,
                    contentPadding = contentPadding,
                    onBack = { navController.popBackStack() },
                    onOpenCollection = { collectionId ->
                        viewModel.selectCollection(collectionId)
                        navController.navigate(AppRoute.Journal.createRoute(collectionId))
                    },
                    onOpenEntry = { entry ->
                        viewModel.selectCollection(entry.collectionId)
                        navController.navigate(AppRoute.Detail.createRoute(entry.id))
                    },
                )
            }

            composable(
                route = AppRoute.SearchCollection.route,
                arguments = listOf(
                    navArgument(AppRoute.SearchCollection.collectionIdArg) {
                        type = NavType.LongType
                    },
                ),
            ) { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getLong(AppRoute.SearchCollection.collectionIdArg)
                    ?: return@composable

                val collectionFlow = remember(collectionId) {
                    viewModel.observeCollection(collectionId)
                }
                val entriesFlow = remember(collectionId) {
                    viewModel.observeEntries(collectionId)
                }
                val routeCollection by collectionFlow.collectAsStateWithLifecycle(initialValue = null)
                val routeEntries by entriesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                LaunchedEffect(collectionId) {
                    viewModel.selectCollection(collectionId)
                }

                SearchScreen(
                    isGlobalSearch = false,
                    collections = collections,
                    entries = routeEntries,
                    currentCollection = routeCollection,
                    contentPadding = contentPadding,
                    onBack = { navController.popBackStack() },
                    onOpenCollection = { targetCollectionId ->
                        viewModel.selectCollection(targetCollectionId)
                        navController.navigate(AppRoute.Journal.createRoute(targetCollectionId))
                    },
                    onOpenEntry = { entry ->
                        navController.navigate(AppRoute.Detail.createRoute(entry.id))
                    },
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
                    jpegQuality = settings.photoQualityJpeg,
                    scanFrameSettings = settings.scanFrameSettings,
                    onOpenScanFrameEditor = {
                        Log.d(CameraLogTag, "Navigate to scan frame editor from replace photo camera. entryId=$entryId")
                        navController.navigate(AppRoute.ScanFrameEditor.route)
                    },
                )
            }

            composable(AppRoute.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    journalCollection = selectedCollection,
                    mode = if (settingsSource == SettingsSource.Journal) {
                        SettingsMode.Journal
                    } else {
                        SettingsMode.Home
                    },
                    contentPadding = contentPadding,
                    onSelectTheme = viewModel::setThemeMode,
                    onSelectAppIcon = viewModel::setAppIconMode,
                    onNumberSizeChange = viewModel::setNumberFontSize,
                    onNewEntryPositionChange = { position ->
                        selectedCollection?.id?.let { collectionId ->
                            viewModel.setNewEntryPosition(collectionId, position)
                        }
                    },
                    onIncludeDirectionInCopyChange = { include ->
                        selectedCollection?.id?.let { collectionId ->
                            viewModel.setIncludeDirectionInCopy(collectionId, include)
                        }
                    },
                    onPhotoQualityChange = viewModel::setPhotoQualityJpeg,
                    onSharePhotoQualityChange = viewModel::setSharePhotoQualityJpeg,
                    onOpenScanFrameEditor = {
                        Log.d(CameraLogTag, "Navigate to scan frame editor from settings")
                        navController.navigate(AppRoute.ScanFrameEditor.route)
                    },
                )
            }

            composable(AppRoute.ShareQr.route) {
                QrShareScreen(
                    session = qrShareSession,
                    contentPadding = contentPadding,
                    onBack = {
                        viewModel.clearQrShareSession()
                        navController.popBackStack()
                    },
                )
            }

            composable(AppRoute.ImportQr.route) {
                QrImportScreen(
                    viewModel = viewModel,
                    contentPadding = contentPadding,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(AppRoute.ScanFrameEditor.route) {
                ScanFrameEditorScreen(
                    initialSettings = settings.scanFrameSettings,
                    contentPadding = contentPadding,
                    onBack = { navController.popBackStack() },
                    onSave = viewModel::setScanFrameSettings,
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
    showJournalActions: Boolean,
    showSearchAction: Boolean,
    showImportAction: Boolean,
    onNavigateJournal: () -> Unit,
    onNavigateCamera: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateSearch: () -> Unit,
    onImportCollection: () -> Unit,
    onImportQr: () -> Unit,
) {
    val metrics = rememberHopperUiMetrics()
    var importMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = metrics.horizontalPadding + 2.dp,
                    vertical = metrics.smallSpacing + 2.dp,
                ),
            horizontalArrangement = if (!showJournalActions && !showImportAction) {
                Arrangement.spacedBy(metrics.sectionSpacing + 4.dp, Alignment.End)
            } else {
                Arrangement.SpaceBetween
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showJournalActions) {
                IconButton(onClick = onNavigateJournal) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = "Журнал",
                        tint = if (currentRoute == AppRoute.Journal.route) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                IconButton(
                    onClick = onNavigateCamera,
                    enabled = canOpenCamera,
                    modifier = Modifier.size(metrics.navButtonSize),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = "Съемка",
                        tint = when {
                            !canOpenCamera -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            currentRoute == AppRoute.Camera.route -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            if (showSearchAction) {
                IconButton(onClick = onNavigateSearch) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Поиск",
                        tint = if (
                            currentRoute == AppRoute.SearchGlobal.route ||
                            currentRoute == AppRoute.SearchCollection.route
                        ) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            if (showImportAction) {
                Box {
                    IconButton(
                        onClick = { importMenuExpanded = true },
                        modifier = Modifier.size(metrics.navButtonSize),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Открыть подборку",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    DropdownMenu(
                        expanded = importMenuExpanded,
                        onDismissRequest = { importMenuExpanded = false },
                        offset = DpOffset(x = 0.dp, y = (-8).dp),
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("Файл Hopper")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                importMenuExpanded = false
                                onImportCollection()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text("Сканировать QR")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.QrCodeScanner,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                importMenuExpanded = false
                                onImportQr()
                            },
                        )
                    }
                }
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
