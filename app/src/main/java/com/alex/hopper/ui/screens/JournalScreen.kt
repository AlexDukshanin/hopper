package com.alex.hopper.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.alex.hopper.data.WagonCollection
import com.alex.hopper.data.ClassifierStateYellow
import com.alex.hopper.data.DefectStateRed
import com.alex.hopper.data.EmptyStateBlue
import com.alex.hopper.data.LoadedStateGreen
import com.alex.hopper.data.WagonEntry
import com.alex.hopper.data.WagonCondition
import com.alex.hopper.data.containerColor
import com.alex.hopper.settings.AppSettings
import com.alex.hopper.ui.HopperUiMetrics
import com.alex.hopper.ui.ProvideCappedFontScale
import com.alex.hopper.ui.cappedTextStyle
import com.alex.hopper.ui.rememberHopperUiMetrics
import com.alex.hopper.util.formatTimestamp
import java.io.File

@Composable
fun JournalScreen(
    collection: WagonCollection?,
    entries: List<WagonEntry>,
    settings: AppSettings,
    contentPadding: PaddingValues,
    onGoHome: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onOpenPhoto: (Long) -> Unit,
    onUpdateNumber: (Long, String) -> Unit,
    onUpdateCondition: (Long, WagonCondition) -> Unit,
    onDeleteEntry: (WagonEntry) -> Unit,
    onDeletePhoto: (Long) -> Unit,
    onReplacePhoto: (Long) -> Unit,
    onToggleDirection: () -> Unit,
    onUpdateDirections: (String, String) -> Unit,
    onSaveNote: (Long, String) -> Unit,
    onSaveJournalDescription: (String) -> Unit,
    onAddEmptyEntry: () -> Unit,
    onTogglePhotoVisibility: (Boolean) -> Unit,
    onMoveEntry: (Long, Int) -> Unit,
    onCopied: () -> Unit,
    onDeleteEntries: (List<WagonEntry>) -> Unit,
    onShareCollectionFile: (Boolean) -> Unit,
    onShareCollectionQr: () -> Unit,
) {
    val metrics = rememberHopperUiMetrics()
    val activeCollection = collection
    if (activeCollection == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Подборка загружается...",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val rowNumbers = buildPrimaryNumbers(entries, activeCollection, CopyListFormat.Row)
    val columnNumbers = buildPrimaryNumbers(entries, activeCollection, CopyListFormat.Column)
    val emptyCount = remember(entries) { entries.count { it.condition == WagonCondition.Empty } }
    val loadedCount = remember(entries) { entries.count { it.condition == WagonCondition.Loaded } }
    val defectCount = remember(entries) { entries.count { it.condition == WagonCondition.Defect } }
    val classifierCount = remember(entries) { entries.count { it.condition == WagonCondition.Classifier } }
    var editingEntry by remember { mutableStateOf<WagonEntry?>(null) }
    var editingNoteEntry by remember { mutableStateOf<WagonEntry?>(null) }
    var deleteEntryTarget by remember { mutableStateOf<WagonEntry?>(null) }
    var photoActionTarget by remember { mutableStateOf<WagonEntry?>(null) }
    var movingEntry by remember { mutableStateOf<WagonEntry?>(null) }
    var editingDirections by remember { mutableStateOf(false) }
    var multiDeleteConfirmVisible by remember { mutableStateOf(false) }
    var selectionMode by remember(activeCollection.id) { mutableStateOf(false) }
    var selectedEntryIds by remember(activeCollection.id) { mutableStateOf<Set<Long>>(emptySet()) }
    var copyListDialogVisible by remember { mutableStateOf(false) }
    var sendListDialogVisible by remember { mutableStateOf(false) }
    var journalDescriptionDraft by rememberSaveable(activeCollection.id, activeCollection.description) {
        mutableStateOf(activeCollection.description)
    }

    LaunchedEffect(entries) {
        val validIds = entries.map(WagonEntry::id).toSet()
        val filteredIds = selectedEntryIds.intersect(validIds)
        if (filteredIds != selectedEntryIds) {
            selectedEntryIds = filteredIds
        }
        if (selectionMode && filteredIds.isEmpty()) {
            selectionMode = false
        }
    }

    val selectedEntries = remember(entries, selectedEntryIds) {
        entries.filter { it.id in selectedEntryIds }
    }

    fun toggleSelection(entryId: Long) {
        val nextIds = if (entryId in selectedEntryIds) {
            selectedEntryIds - entryId
        } else {
            selectedEntryIds + entryId
        }
        selectedEntryIds = nextIds
        selectionMode = nextIds.isNotEmpty()
    }

    fun enterSelectionMode(initialEntryId: Long) {
        selectionMode = true
        selectedEntryIds = setOf(initialEntryId)
        deleteEntryTarget = null
    }

    ProvideCappedFontScale(maxFontScale = 1.18f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = if (selectionMode && entries.isNotEmpty()) {
                    metrics.journalSelectionTopInset
                } else {
                    metrics.verticalPadding - 4.dp
                },
                bottom = metrics.verticalPadding - 4.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(metrics.smallSpacing),
        ) {
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = metrics.cardPadding - 2.dp,
                            vertical = metrics.cardPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "Журнал вагонов",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = (metrics.sectionTitleSize.value + 2f).sp,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = activeCollection.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            FilledTonalIconButton(
                                onClick = onGoHome,
                                modifier = Modifier.size(metrics.largeIconButtonSize),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Home,
                                    contentDescription = "Подборки",
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(metrics.smallSpacing),
                        ) {
                            FilledTonalButton(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(metrics.primaryActionHeight),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                ),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                ),
                                onClick = { copyListDialogVisible = true },
                                enabled = rowNumbers.isNotBlank(),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Копировать",
                                    style = cappedTextStyle(
                                        if (metrics.isCompact) {
                                            MaterialTheme.typography.labelMedium
                                        } else {
                                            MaterialTheme.typography.labelLarge
                                        },
                                        maxFontScale = 1.0f,
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            FilledTonalButton(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(metrics.primaryActionHeight),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                ),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                ),
                                onClick = { sendListDialogVisible = true },
                                enabled = rowNumbers.isNotBlank(),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Отправить",
                                    style = cappedTextStyle(
                                        if (metrics.isCompact) {
                                            MaterialTheme.typography.labelMedium
                                        } else {
                                            MaterialTheme.typography.labelLarge
                                        },
                                        maxFontScale = 1.0f,
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = journalDescriptionDraft,
                            onValueChange = { journalDescriptionDraft = it.take(240) },
                            minLines = 3,
                            maxLines = 3,
                            shape = RoundedCornerShape(18.dp),
                            label = { Text("Описание журнала") },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(metrics.smallSpacing),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilledTonalButton(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    onSaveJournalDescription(journalDescriptionDraft.trim())
                                },
                                enabled = journalDescriptionDraft != activeCollection.description,
                            ) {
                                Text(
                                    "Сохранить описание",
                                    style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.0f),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            FilledTonalIconButton(
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                ),
                                onClick = { onTogglePhotoVisibility(!settings.showPhotosInJournal) },
                                modifier = Modifier
                                    .size(metrics.largeIconButtonSize),
                            ) {
                                Icon(
                                    imageVector = if (settings.showPhotosInJournal) {
                                        Icons.Rounded.Visibility
                                    } else {
                                        Icons.Rounded.VisibilityOff
                                    },
                                    contentDescription = "Изменить вид списка",
                                )
                            }
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    EmptyJournalState(
                        metrics = metrics,
                        onOpenCamera = onOpenCamera,
                        onAddEmptyEntry = onAddEmptyEntry,
                    )
                }
            } else {
                item {
                    if (!selectionMode) {
                        DirectionHeaderRow(
                            metrics = metrics,
                            currentLabel = activeCollection.topDirectionLabel,
                            onToggleDirection = onToggleDirection,
                            onEdit = { editingDirections = true },
                        )
                    }
                }

                itemsIndexed(
                    items = entries,
                    key = { _, item -> item.id },
                ) { index, entry ->
                    if (settings.showPhotosInJournal) {
                        JournalEntryCard(
                            order = index + 1,
                            numberFontSizeSp = settings.numberFontSizeSp,
                            entry = entry,
                            selectionMode = selectionMode,
                            selected = entry.id in selectedEntryIds,
                            onOpenEntry = onOpenEntry,
                            onOpenPhoto = onOpenPhoto,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(it))
                                onCopied()
                            },
                            onToggleSelection = { toggleSelection(entry.id) },
                            onEdit = { editingEntry = entry },
                            onMove = { movingEntry = entry },
                            onToggleCondition = { condition -> onUpdateCondition(entry.id, condition) },
                            onEditNote = { editingNoteEntry = entry },
                            onAskDeleteEntry = { deleteEntryTarget = entry },
                            onAskPhotoActions = { photoActionTarget = entry },
                        )
                    } else {
                        CompactJournalEntryRow(
                            order = index + 1,
                            numberFontSizeSp = settings.numberFontSizeSp,
                            entry = entry,
                            selectionMode = selectionMode,
                            selected = entry.id in selectedEntryIds,
                            onOpenEntry = onOpenEntry,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(it))
                                onCopied()
                            },
                            onToggleSelection = { toggleSelection(entry.id) },
                            onEdit = { editingEntry = entry },
                            onMove = { movingEntry = entry },
                            onToggleCondition = { condition -> onUpdateCondition(entry.id, condition) },
                            onAskDeleteEntry = { deleteEntryTarget = entry },
                        )
                    }
                }

                if (!selectionMode) {
                    item {
                        BottomDirectionButton(
                            metrics = metrics,
                            label = activeCollection.bottomDirectionLabel,
                            onToggleDirection = onToggleDirection,
                        )
                    }

                    item {
                        AddEmptyEntryButton(
                            modifier = Modifier.padding(horizontal = metrics.smallSpacing),
                            metrics = metrics,
                            onClick = onAddEmptyEntry,
                        )
                    }

                    item {
                        LoadStateSummary(
                            emptyCount = emptyCount,
                            loadedCount = loadedCount,
                            defectCount = defectCount,
                            classifierCount = classifierCount,
                        )
                    }
                }
            }
        }

            if (selectionMode && entries.isNotEmpty()) {
                SelectionModeBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = metrics.verticalPadding - 4.dp),
                    selectedCount = selectedEntryIds.size,
                    allSelected = selectedEntryIds.size == entries.size,
                    onSelectAll = {
                        selectedEntryIds = entries.map(WagonEntry::id).toSet()
                    },
                    onClearSelection = {
                        selectedEntryIds = emptySet()
                        selectionMode = false
                    },
                    onDeleteSelection = { multiDeleteConfirmVisible = true },
                )
            }
        }

        if (copyListDialogVisible) {
            ListFormatDialog(
                title = "Как скопировать список?",
                subtitle = "Выберите формат: строка или столбец.",
                onDismiss = { copyListDialogVisible = false },
                onRow = {
                    clipboardManager.setText(AnnotatedString(rowNumbers))
                    onCopied()
                    copyListDialogVisible = false
                },
                onColumn = {
                    clipboardManager.setText(AnnotatedString(columnNumbers))
                    onCopied()
                    copyListDialogVisible = false
                },
            )
        }

        if (sendListDialogVisible) {
            SendJournalDialog(
                onDismiss = { sendListDialogVisible = false },
                onSendTextRow = {
                    shareTextList(context, rowNumbers)
                    sendListDialogVisible = false
                },
                onSendTextColumn = {
                    shareTextList(context, columnNumbers)
                    sendListDialogVisible = false
                },
                onSendFileWithoutPhotos = {
                    onShareCollectionFile(false)
                    sendListDialogVisible = false
                },
                onSendFileWithPhotos = {
                    onShareCollectionFile(true)
                    sendListDialogVisible = false
                },
                onSendQr = {
                    onShareCollectionQr()
                    sendListDialogVisible = false
                },
            )
        }

        editingEntry?.let { entry ->
            EditNumberDialog(
                initialValue = entry.primaryNumber.orEmpty(),
                onDismiss = { editingEntry = null },
                onSave = { value ->
                    onUpdateNumber(entry.id, value)
                    editingEntry = null
                },
            )
        }

        editingNoteEntry?.let { entry ->
            NoteDialog(
                initialValue = entry.note,
                onDismiss = { editingNoteEntry = null },
                onSave = { note ->
                    onSaveNote(entry.id, note)
                    editingNoteEntry = null
                },
            )
        }

        movingEntry?.let { entry ->
            MoveEntryDialog(
                entry = entry,
                entries = entries,
                onDismiss = { movingEntry = null },
                onMove = { targetIndex ->
                    onMoveEntry(entry.id, targetIndex)
                    movingEntry = null
                },
            )
        }

        if (editingDirections) {
            EditDirectionsDialog(
                initialPrimary = activeCollection.primaryDirectionLabel,
                initialSecondary = activeCollection.secondaryDirectionLabel,
                onDismiss = { editingDirections = false },
                onSave = { primary, secondary ->
                    onUpdateDirections(primary, secondary)
                    editingDirections = false
                },
            )
        }

        deleteEntryTarget?.let { entry ->
            ConfirmDeleteEntryDialog(
                onDismiss = { deleteEntryTarget = null },
                onMultiDelete = { enterSelectionMode(entry.id) },
                onConfirm = {
                    onDeleteEntry(entry)
                    deleteEntryTarget = null
                },
            )
        }

        if (multiDeleteConfirmVisible) {
            ConfirmDeleteMultipleDialog(
                count = selectedEntryIds.size,
                onDismiss = { multiDeleteConfirmVisible = false },
                onConfirm = {
                    onDeleteEntries(selectedEntries)
                    selectedEntryIds = emptySet()
                    selectionMode = false
                    multiDeleteConfirmVisible = false
                },
            )
        }

        photoActionTarget?.let { entry ->
            PhotoActionsDialog(
                hasPhoto = entry.imagePath.isNotBlank(),
                onDismiss = { photoActionTarget = null },
                onDeletePhoto = {
                    onDeletePhoto(entry.id)
                    photoActionTarget = null
                },
                onReplacePhoto = {
                    onReplacePhoto(entry.id)
                    photoActionTarget = null
                },
            )
        }
    }
}

@Composable
private fun CompactJournalEntryRow(
    order: Int,
    numberFontSizeSp: Float,
    entry: WagonEntry,
    selectionMode: Boolean,
    selected: Boolean,
    onOpenEntry: (Long) -> Unit,
    onCopy: (String) -> Unit,
    onToggleSelection: () -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onToggleCondition: (WagonCondition) -> Unit,
    onAskDeleteEntry: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "compact_selection_color",
    )
    Card(
        modifier = Modifier.combinedClickable(
            onClick = {
                if (selectionMode) onToggleSelection() else onOpenEntry(entry.id)
            },
            onLongClick = {
                if (selectionMode) onToggleSelection() else onAskDeleteEntry()
            },
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (selectionMode) {
                SelectionStateBadge(selected = selected)
            } else {
                Text(
                    text = order.toString(),
                    style = cappedTextStyle(MaterialTheme.typography.titleMedium, maxFontScale = 1.05f),
                )
            }
            Text(
                text = entry.displayNumber(),
                modifier = Modifier.weight(1f),
                style = cappedTextStyle(
                    MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = numberFontSizeSp.sp,
                    ),
                    maxFontScale = 1.1f,
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.note.isNotBlank()) {
                NoteIndicatorBadge(
                    color = entry.condition.containerColor(),
                )
            }
            FilledTonalIconButton(
                onClick = { entry.primaryNumber?.let(onCopy) },
                enabled = !selectionMode && !entry.primaryNumber.isNullOrBlank(),
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Копировать номер",
                )
            }
            FilledTonalIconButton(
                onClick = onEdit,
                enabled = !selectionMode,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Редактировать номер",
                )
            }
            FilledTonalIconButton(
                onClick = onMove,
                enabled = !selectionMode,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = "Переместить карточку",
                )
            }
            LoadStateToggleButton(
                condition = entry.condition,
                onClick = if (selectionMode) ({}) else ({ onToggleCondition(entry.condition.nextTapState()) }),
                onLongClick = if (selectionMode) ({}) else ({ onToggleCondition(entry.condition.toggleLongPressState()) }),
                modifier = Modifier.size(34.dp),
                enabled = !selectionMode,
            )
        }
    }
}

@Composable
private fun NoteIndicatorBadge(
    color: Color,
) {
    Surface(
        modifier = Modifier.size(18.dp),
        shape = CircleShape,
        color = color,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "!",
                style = cappedTextStyle(MaterialTheme.typography.labelSmall, maxFontScale = 1.0f),
                color = Color.White,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EmptyJournalState(
    metrics: HopperUiMetrics,
    onOpenCamera: () -> Unit,
    onAddEmptyEntry: () -> Unit,
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(metrics.cardPadding + 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing),
        ) {
            Text(
                text = "Журнал пока пуст",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Сделайте первый снимок вагона, затем при необходимости поправьте номер вручную.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (metrics.isCompact) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(metrics.smallSpacing),
                ) {
                    FilledTonalIconButton(
                        onClick = onOpenCamera,
                        modifier = Modifier.size(metrics.navButtonSize),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = "Открыть камеру",
                        )
                    }
                    AddEmptyEntryButton(metrics = metrics, onClick = onAddEmptyEntry)
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(metrics.smallSpacing + 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(
                        onClick = onOpenCamera,
                        modifier = Modifier.size(metrics.navButtonSize),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = "Открыть камеру",
                        )
                    }
                    AddEmptyEntryButton(
                        metrics = metrics,
                        modifier = Modifier.weight(1f),
                        onClick = onAddEmptyEntry,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEmptyEntryButton(
    metrics: HopperUiMetrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        modifier = modifier
            .fillMaxWidth()
            .height(metrics.primaryActionHeight),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Добавить пустую карточку",
            style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.0f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun JournalEntryCard(
    order: Int,
    numberFontSizeSp: Float,
    entry: WagonEntry,
    selectionMode: Boolean,
    selected: Boolean,
    onOpenEntry: (Long) -> Unit,
    onOpenPhoto: (Long) -> Unit,
    onCopy: (String) -> Unit,
    onToggleSelection: () -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onToggleCondition: (WagonCondition) -> Unit,
    onEditNote: () -> Unit,
    onAskDeleteEntry: () -> Unit,
    onAskPhotoActions: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "card_selection_color",
    )
    Card(
        modifier = Modifier.combinedClickable(
            onClick = {
                if (selectionMode) onToggleSelection() else onOpenEntry(entry.id)
            },
            onLongClick = {
                if (selectionMode) onToggleSelection() else onAskDeleteEntry()
            },
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                        .padding(start = 12.dp, top = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (selectionMode) {
                            SelectionStateBadge(selected = selected)
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Box(
                                    modifier = Modifier.size(30.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = order.toString(),
                                        style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.05f),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                        Text(
                            text = entry.displayNumber(),
                            modifier = Modifier
                                .weight(1f)
                                .marqueeWhenNeeded(),
                            style = cappedTextStyle(
                                MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = numberFontSizeSp.sp,
                                ),
                                maxFontScale = 1.1f,
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                        )
                    }

                    Text(
                        text = formatTimestamp(entry.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalIconButton(
                            onClick = { entry.primaryNumber?.let(onCopy) },
                            enabled = !selectionMode && !entry.primaryNumber.isNullOrBlank(),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Копировать номер",
                            )
                        }

                        FilledTonalIconButton(
                            onClick = onEdit,
                            enabled = !selectionMode,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Редактировать номер",
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onMove,
                            enabled = !selectionMode,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapVert,
                                contentDescription = "Переместить карточку",
                            )
                        }
                        LoadStateToggleButton(
                            condition = entry.condition,
                            onClick = if (selectionMode) ({}) else ({ onToggleCondition(entry.condition.nextTapState()) }),
                            onLongClick = if (selectionMode) ({}) else ({ onToggleCondition(entry.condition.toggleLongPressState()) }),
                            modifier = Modifier.size(36.dp),
                            enabled = !selectionMode,
                        )
                    }
                }

                PhotoThumb(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight(),
                    imagePath = entry.imagePath,
                    onOpenPhoto = if (selectionMode) onToggleSelection else { { onOpenPhoto(entry.id) } },
                    onPhotoActions = if (selectionMode) onToggleSelection else onAskPhotoActions,
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = if (selectionMode) {
                            onToggleSelection
                        } else {
                            onEditNote
                        },
                    ),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        text = entry.note.ifBlank { "Добавить описание" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LoadStateToggleButton(
    condition: WagonCondition,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier.combinedClickable(
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        shape = CircleShape,
        color = condition.containerColor(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = condition.shortLabel,
                style = cappedTextStyle(MaterialTheme.typography.labelMedium, maxFontScale = 1.05f),
                color = Color.White,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SelectionStateBadge(
    selected: Boolean,
) {
    Surface(
        modifier = Modifier.size(30.dp),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = BorderStroke(
            width = 2.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Выбрано",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun LoadStateSummary(
    emptyCount: Int,
    loadedCount: Int,
    defectCount: Int,
    classifierCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Порожних:",
                style = MaterialTheme.typography.bodyMedium,
                color = EmptyStateBlue,
            )
            Text(
                text = emptyCount.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Груженых:",
                style = MaterialTheme.typography.bodyMedium,
                color = LoadedStateGreen,
            )
            Text(
                text = loadedCount.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Брак:",
                style = MaterialTheme.typography.bodyMedium,
                color = DefectStateRed,
            )
            Text(
                text = defectCount.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Классник:",
                style = MaterialTheme.typography.bodyMedium,
                color = ClassifierStateYellow,
            )
            Text(
                text = classifierCount.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MoveEntryDialog(
    entry: WagonEntry,
    entries: List<WagonEntry>,
    onDismiss: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val currentIndex = entries.indexOfFirst { it.id == entry.id }.coerceAtLeast(0)
    val remainingEntries = remember(entries, entry.id) {
        entries.filterNot { it.id == entry.id }
    }
    var selectedSlot by remember(entry.id, remainingEntries.size) {
        mutableStateOf(currentIndex.coerceIn(0, remainingEntries.size))
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedSlot * 2).coerceAtLeast(0))
    val previewText = remember(entry.id, remainingEntries, selectedSlot) {
        buildMovePreview(remainingEntries, entry, selectedSlot)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Переместить карточку",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Нажмите в пустой промежуток, куда ее вставить.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoveEntrySlots(
                    entries = remainingEntries,
                    selectedSlot = selectedSlot,
                    listState = listState,
                    onSelect = { selectedSlot = it },
                )
                AnimatedContent(targetState = previewText, label = "move_preview") { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    TextButton(
                        onClick = { onMove(selectedSlot) },
                        enabled = selectedSlot != currentIndex,
                    ) {
                        Text("Переместить")
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveEntrySlots(
    entries: List<WagonEntry>,
    selectedSlot: Int,
    listState: LazyListState,
    onSelect: (Int) -> Unit,
) {
    val dialogItems = remember(entries) { buildMoveDialogItems(entries) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp),
        ) {
            itemsIndexed(dialogItems, key = { _, item -> item.key }) { _, item ->
                when (item) {
                    is MoveDialogItem.Slot -> {
                        MoveInsertionSlot(
                            selected = selectedSlot == item.slotIndex,
                            label = item.label,
                            onClick = { onSelect(item.slotIndex) },
                        )
                    }

                    is MoveDialogItem.Entry -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${item.order}.",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Text(
                                    text = item.entry.primaryNumber ?: "Номер не найден",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface MoveDialogItem {
    val key: String

    data class Slot(
        val slotIndex: Int,
        val label: String,
    ) : MoveDialogItem {
        override val key: String = "slot_$slotIndex"
    }

    data class Entry(
        val entry: WagonEntry,
        val order: Int,
    ) : MoveDialogItem {
        override val key: String = "entry_${entry.id}"
    }
}

private fun buildMoveDialogItems(
    entries: List<WagonEntry>,
): List<MoveDialogItem> {
    val items = mutableListOf<MoveDialogItem>()
    val topLabel = if (entries.isEmpty()) {
        "Вставить как первую карточку"
    } else {
        "Вставить перед ${entries.first().moveSlotReference(1)}"
    }
    items += MoveDialogItem.Slot(slotIndex = 0, label = topLabel)

    entries.forEachIndexed { index, item ->
        items += MoveDialogItem.Entry(
            entry = item,
            order = index + 1,
        )

        val nextItem = entries.getOrNull(index + 1)
        val slotLabel = if (nextItem != null) {
            "Вставить между ${item.moveSlotReference(index + 1)} и ${nextItem.moveSlotReference(index + 2)}"
        } else {
            "Вставить после ${item.moveSlotReference(index + 1)}"
        }
        items += MoveDialogItem.Slot(
            slotIndex = index + 1,
            label = slotLabel,
        )
    }

    return items
}

@Composable
private fun MoveInsertionSlot(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val slotHeight by animateDpAsState(
        targetValue = if (selected) 42.dp else 18.dp,
        label = "move_slot_height",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        label = "move_slot_color",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(slotHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(targetState = selected, label = "slot_label") { isSelected ->
                if (isSelected) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(3.dp),
                        shape = RoundedCornerShape(99.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun SelectionModeBar(
    modifier: Modifier = Modifier,
    selectedCount: Int,
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelection: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.defaultMinSize(minWidth = 40.dp),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = selectedCount.coerceAtMost(99).toString(),
                        style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.1f),
                        color = MaterialTheme.colorScheme.onError,
                        maxLines = 1,
                    )
                }
            }

            TextButton(
                modifier = Modifier.weight(1f),
                onClick = onSelectAll,
                enabled = !allSelected,
            ) {
                Text(
                    "Выбрать все",
                    style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.0f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = onClearSelection,
            ) {
                Text(
                    "Отмена",
                    style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.0f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalIconButton(
                onClick = onDeleteSelection,
                modifier = Modifier.size(46.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Удалить выбранные карточки",
                )
            }
        }
    }
}

@Composable
private fun DirectionHeaderRow(
    metrics: HopperUiMetrics,
    currentLabel: String,
    onToggleDirection: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(metrics.smallSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            modifier = Modifier.weight(1f),
            onClick = onToggleDirection,
        ) {
            Text(
                text = currentLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FilledTonalIconButton(
            onClick = onEdit,
            modifier = Modifier.size(metrics.largeIconButtonSize),
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "Изменить названия направлений",
            )
        }
    }
}

@Composable
private fun BottomDirectionButton(
    metrics: HopperUiMetrics,
    label: String,
    onToggleDirection: () -> Unit,
) {
    FilledTonalButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = metrics.smallSpacing),
        onClick = onToggleDirection,
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PhotoThumb(
    modifier: Modifier = Modifier,
    imagePath: String,
    onOpenPhoto: () -> Unit,
    onPhotoActions: () -> Unit,
) {
    Box(modifier = modifier) {
        if (imagePath.isBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onPhotoActions),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = "Нет фото",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            AsyncImage(
                model = File(imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onOpenPhoto),
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .clickable(onClick = onPhotoActions),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Действия с фото",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ListFormatDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onRow: () -> Unit,
    onColumn: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Text(subtitle)
        },
        confirmButton = {
            TextButton(onClick = onRow) {
                Text("Строка", style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.1f))
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onColumn) {
                    Text("Столбец", style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.1f))
                }
                TextButton(onClick = onDismiss) {
                    Text("Отмена", style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.1f))
                }
            }
        },
    )
}

@Composable
private fun SendJournalDialog(
    onDismiss: () -> Unit,
    onSendTextRow: () -> Unit,
    onSendTextColumn: () -> Unit,
    onSendFileWithoutPhotos: () -> Unit,
    onSendFileWithPhotos: () -> Unit,
    onSendQr: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Как отправить журнал?")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Можно отправить обычный текст или файл Hopper для полного переноса подборки.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSendTextRow,
                ) {
                    Text("Текст строкой")
                }
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSendTextColumn,
                ) {
                    Text("Текст столбцом")
                }
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSendFileWithoutPhotos,
                ) {
                    Text("Файл Hopper без фото")
                }
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSendFileWithPhotos,
                ) {
                    Text("Файл Hopper с фото")
                }
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSendQr,
                ) {
                    Text("QR-код Hopper")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
}

private fun shareTextList(
    context: Context,
    text: String,
) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "HOPPER")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooserIntent = Intent.createChooser(sendIntent, "Отправить список").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooserIntent)
}

@Composable
private fun EditDirectionsDialog(
    initialPrimary: String,
    initialSecondary: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var primary by remember(initialPrimary) { mutableStateOf(initialPrimary) }
    var secondary by remember(initialSecondary) { mutableStateOf(initialSecondary) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Названия направлений")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = primary,
                    onValueChange = { primary = it.take(24) },
                    singleLine = true,
                    label = { Text("Первое название") },
                )
                OutlinedTextField(
                    value = secondary,
                    onValueChange = { secondary = it.take(24) },
                    singleLine = true,
                    label = { Text("Второе название") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        primary.trim().ifBlank { "ЗАПАД" },
                        secondary.trim().ifBlank { "ВОСТОК" },
                    )
                },
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
}

@Composable
private fun NoteDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var note by remember(initialValue) { mutableStateOf(initialValue) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Text("Описание вагона")
        },
        text = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = note,
                onValueChange = { note = it.take(500) },
                minLines = 4,
                maxLines = 8,
                label = { Text("Описание") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    onSave(note.trim())
                },
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
}

@Composable
private fun EditNumberDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Изменить номер")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Исправьте номер вручную. В поле останутся только цифры.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter(Char::isDigit).take(12) },
                    singleLine = true,
                    label = { Text("Номер вагона") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(value) },
                enabled = value.isNotBlank(),
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
}

@Composable
private fun ConfirmDeleteEntryDialog(
    onDismiss: () -> Unit,
    onMultiDelete: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Удалить карточку?",
                    style = cappedTextStyle(MaterialTheme.typography.headlineSmall, maxFontScale = 1.15f),
                )
                Text(
                    "Карточка будет удалена. После этого останется кнопка вернуть на 5 секунд.",
                    style = cappedTextStyle(MaterialTheme.typography.bodyLarge, maxFontScale = 1.15f),
                )
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onConfirm,
                ) {
                    Text(
                        "Удалить",
                        style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.0f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onMultiDelete) {
                        Text(
                            "Удалить\nнесколько",
                            style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.0f),
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.0f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteMultipleDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    "Удалить несколько карточек?",
                    style = cappedTextStyle(MaterialTheme.typography.headlineSmall, maxFontScale = 1.15f),
                )
                Text(
                    "Будет удалено: $count. После этого останется кнопка вернуть на 5 секунд.",
                    style = cappedTextStyle(MaterialTheme.typography.bodyLarge, maxFontScale = 1.15f),
                )
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onConfirm,
                ) {
                    Text("Удалить", style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.0f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", style = cappedTextStyle(MaterialTheme.typography.labelLarge, maxFontScale = 1.0f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoActionsDialog(
    hasPhoto: Boolean,
    onDismiss: () -> Unit,
    onDeletePhoto: () -> Unit,
    onReplacePhoto: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Действия с фото")
        },
        text = {
            Text(
                if (hasPhoto) {
                    "Можно удалить текущее фото или сразу снять новое на замену."
                } else {
                    "У этой карточки пока нет фото. Можно сразу открыть камеру и добавить его позже."
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onReplacePhoto) {
                Text(if (hasPhoto) "Заменить фото" else "Добавить фото")
            }
        },
        dismissButton = {
            if (hasPhoto) {
                TextButton(onClick = onDeletePhoto) {
                    Text("Удалить фото")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }
        },
    )
}

private fun WagonEntry.displayNumber(): String {
    return primaryNumber?.takeIf(String::isNotBlank) ?: "Без номера"
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.marqueeWhenNeeded(): Modifier = basicMarquee()

private enum class CopyListFormat {
    Row,
    Column,
}

private fun buildPrimaryNumbers(
    entries: List<WagonEntry>,
    collection: WagonCollection,
    format: CopyListFormat,
): String {
    val body = entries.mapNotNull { item ->
        item.primaryNumber?.takeIf(String::isNotBlank)
    }.mapIndexed { index, number ->
        when (format) {
            CopyListFormat.Row -> "${index + 1}. $number"
            CopyListFormat.Column -> "${index + 1}. $number;"
        }
    }.joinToString(
        separator = if (format == CopyListFormat.Row) " ; " else "\n",
    )

    if (!collection.includeDirectionInCopy) {
        return body
    }

    return when (format) {
        CopyListFormat.Row -> {
            if (body.isBlank()) {
                "${collection.topDirectionLabel}; ${collection.bottomDirectionLabel}."
            } else {
                "${collection.topDirectionLabel}; $body; ${collection.bottomDirectionLabel}."
            }
        }
        CopyListFormat.Column -> {
            if (body.isBlank()) {
                "${collection.topDirectionLabel};\n${collection.bottomDirectionLabel}."
            } else {
                "${collection.topDirectionLabel};\n$body\n${collection.bottomDirectionLabel}."
            }
        }
    }
}

private fun buildMovePreview(
    entries: List<WagonEntry>,
    movingEntry: WagonEntry,
    slotIndex: Int,
): String {
    if (entries.isEmpty()) {
        return "Карточка станет первой и единственной"
    }

    val boundedSlot = slotIndex.coerceIn(0, entries.size)
    val previous = entries.getOrNull(boundedSlot - 1)
    val next = entries.getOrNull(boundedSlot)

    return when {
        boundedSlot == 0 && next != null -> "Встанет перед ${next.moveSlotReference(1)}"
        boundedSlot == entries.size && previous != null -> {
            "Встанет после ${previous.moveSlotReference(entries.size)}"
        }
        previous != null && next != null -> {
            val previousOrder = boundedSlot
            val nextOrder = boundedSlot + 1
            "Встанет между ${previous.moveSlotReference(previousOrder)} и ${next.moveSlotReference(nextOrder)}"
        }
        else -> "Карточка станет первой"
    }
}

private fun WagonEntry.moveSlotReference(order: Int): String {
    val number = primaryNumber?.takeIf(String::isNotBlank)
    return if (number == null) {
        order.toString()
    } else {
        "$order. $number"
    }
}
