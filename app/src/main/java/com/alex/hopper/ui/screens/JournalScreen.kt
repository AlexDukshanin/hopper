package com.alex.hopper.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.alex.hopper.data.WagonCollection
import com.alex.hopper.data.WagonEntry
import com.alex.hopper.settings.AppSettings
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
    onDeleteEntry: (WagonEntry) -> Unit,
    onDeletePhoto: (Long) -> Unit,
    onReplacePhoto: (Long) -> Unit,
    onToggleDirection: () -> Unit,
    onUpdateDirections: (String, String) -> Unit,
    onSaveNote: (Long, String) -> Unit,
    onSaveJournalDescription: (String) -> Unit,
    onTogglePhotoVisibility: (Boolean) -> Unit,
    onMoveEntry: (Long, Int) -> Unit,
    onCopied: () -> Unit,
) {
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
    val rowNumbers = buildPrimaryNumbers(entries, settings, CopyListFormat.Row)
    val columnNumbers = buildPrimaryNumbers(entries, settings, CopyListFormat.Column)
    var editingEntry by remember { mutableStateOf<WagonEntry?>(null) }
    var editingNoteEntry by remember { mutableStateOf<WagonEntry?>(null) }
    var deleteEntryTarget by remember { mutableStateOf<WagonEntry?>(null) }
    var photoActionTarget by remember { mutableStateOf<WagonEntry?>(null) }
    var movingEntry by remember { mutableStateOf<WagonEntry?>(null) }
    var editingDirections by remember { mutableStateOf(false) }
    var copyListDialogVisible by remember { mutableStateOf(false) }
    var sendListDialogVisible by remember { mutableStateOf(false) }
    var journalDescriptionDraft by rememberSaveable(activeCollection.id, activeCollection.description) {
        mutableStateOf(activeCollection.description)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                style = MaterialTheme.typography.headlineSmall,
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
                            modifier = Modifier.size(46.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Home,
                                contentDescription = "Подборки",
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = { copyListDialogVisible = true },
                            enabled = rowNumbers.isNotBlank(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Копировать список")
                        }
                        FilledTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = { sendListDialogVisible = true },
                            enabled = rowNumbers.isNotBlank(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Отправить")
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = { onSaveJournalDescription(journalDescriptionDraft.trim()) },
                            enabled = journalDescriptionDraft != activeCollection.description,
                        ) {
                            Text("Сохранить описание")
                        }
                        FilledTonalIconButton(
                            onClick = { onTogglePhotoVisibility(!settings.showPhotosInJournal) },
                            modifier = Modifier.size(48.dp),
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
                EmptyJournalState(onOpenCamera = onOpenCamera)
            }
        } else {
            item {
                DirectionHeaderRow(
                    currentLabel = settings.topDirectionLabel,
                    onToggleDirection = onToggleDirection,
                    onEdit = { editingDirections = true },
                )
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
                        onOpenEntry = onOpenEntry,
                        onOpenPhoto = onOpenPhoto,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(it))
                            onCopied()
                        },
                        onEdit = { editingEntry = entry },
                        onMove = { movingEntry = entry },
                        onEditNote = { editingNoteEntry = entry },
                        onAskDeleteEntry = { deleteEntryTarget = entry },
                        onAskPhotoActions = { photoActionTarget = entry },
                    )
                } else {
                    CompactJournalEntryRow(
                        order = index + 1,
                        numberFontSizeSp = settings.numberFontSizeSp,
                        entry = entry,
                        onOpenEntry = onOpenEntry,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(it))
                            onCopied()
                        },
                        onEdit = { editingEntry = entry },
                        onMove = { movingEntry = entry },
                        onAskDeleteEntry = { deleteEntryTarget = entry },
                    )
                }
            }

            item {
                BottomDirectionButton(
                    label = settings.bottomDirectionLabel,
                    onToggleDirection = onToggleDirection,
                )
            }
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
        ListFormatDialog(
            title = "Как отправить список?",
            subtitle = "Выберите формат и откроется системное окно отправки.",
            onDismiss = { sendListDialogVisible = false },
            onRow = {
                shareTextList(context, rowNumbers)
                sendListDialogVisible = false
            },
            onColumn = {
                shareTextList(context, columnNumbers)
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
            initialPrimary = settings.primaryDirectionLabel,
            initialSecondary = settings.secondaryDirectionLabel,
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
            onConfirm = {
                onDeleteEntry(entry)
                deleteEntryTarget = null
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

@Composable
private fun CompactJournalEntryRow(
    order: Int,
    numberFontSizeSp: Float,
    entry: WagonEntry,
    onOpenEntry: (Long) -> Unit,
    onCopy: (String) -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onAskDeleteEntry: () -> Unit,
) {
    Card(
        modifier = Modifier.combinedClickable(
            onClick = { onOpenEntry(entry.id) },
            onLongClick = onAskDeleteEntry,
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = order.toString(),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = entry.primaryNumber ?: "Номер не найден",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = numberFontSizeSp.sp,
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            FilledTonalIconButton(
                onClick = { entry.primaryNumber?.let(onCopy) },
                enabled = !entry.primaryNumber.isNullOrBlank(),
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Копировать номер",
                )
            }
            FilledTonalIconButton(
                onClick = onEdit,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Редактировать номер",
                )
            }
            FilledTonalIconButton(
                onClick = onMove,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = "Переместить карточку",
                )
            }
        }
    }
}

@Composable
private fun EmptyJournalState(
    onOpenCamera: () -> Unit,
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
            FilledTonalIconButton(onClick = onOpenCamera) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = "Открыть камеру",
                )
            }
        }
    }
}

@Composable
private fun JournalEntryCard(
    order: Int,
    numberFontSizeSp: Float,
    entry: WagonEntry,
    onOpenEntry: (Long) -> Unit,
    onOpenPhoto: (Long) -> Unit,
    onCopy: (String) -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onEditNote: () -> Unit,
    onAskDeleteEntry: () -> Unit,
    onAskPhotoActions: () -> Unit,
) {
    Card(
        modifier = Modifier.combinedClickable(
            onClick = { onOpenEntry(entry.id) },
            onLongClick = onAskDeleteEntry,
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
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
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Text(
                            text = entry.primaryNumber ?: "Номер не найден",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = numberFontSizeSp.sp,
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
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
                            enabled = !entry.primaryNumber.isNullOrBlank(),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Копировать номер",
                            )
                        }

                        FilledTonalIconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Редактировать номер",
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onMove,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapVert,
                                contentDescription = "Переместить карточку",
                            )
                        }
                    }
                }

                PhotoThumb(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight(),
                    imagePath = entry.imagePath,
                    onOpenPhoto = { onOpenPhoto(entry.id) },
                    onPhotoActions = onAskPhotoActions,
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditNote),
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Переместить карточку")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Карточка временно убрана из списка. Нажмите в пустой промежуток, куда ее вставить.",
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onMove(selectedSlot) },
                enabled = selectedSlot != currentIndex,
            ) {
                Text("Переместить")
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
                .height(260.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
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
        "Вставить перед ${entries.first().primaryNumber ?: "первой карточкой"}"
    }
    items += MoveDialogItem.Slot(slotIndex = 0, label = topLabel)

    entries.forEachIndexed { index, item ->
        items += MoveDialogItem.Entry(
            entry = item,
            order = index + 1,
        )

        val nextItem = entries.getOrNull(index + 1)
        val slotLabel = if (nextItem != null) {
            "Вставить между ${item.primaryNumber ?: "текущей карточкой"} и ${nextItem.primaryNumber ?: "следующей карточкой"}"
        } else {
            "Вставить после ${item.primaryNumber ?: "последней карточки"}"
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
                            .padding(horizontal = 18.dp)
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
private fun DirectionHeaderRow(
    currentLabel: String,
    onToggleDirection: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            modifier = Modifier.weight(1f),
            onClick = onToggleDirection,
        ) {
            Text(currentLabel)
        }
        FilledTonalIconButton(
            onClick = onEdit,
            modifier = Modifier.size(44.dp),
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
    label: String,
    onToggleDirection: () -> Unit,
) {
    FilledTonalButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        onClick = onToggleDirection,
    ) {
        Text(label)
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
                Text("Строка")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onColumn) {
                    Text("Столбец")
                }
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
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

    AlertDialog(
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
            TextButton(onClick = { onSave(note.trim()) }) {
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
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Удалить карточку?")
        },
        text = {
            Text("Карточка будет удалена. После этого останется кнопка вернуть на 5 секунд.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Удалить")
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
            Text("Можно удалить текущее фото или сразу снять новое на замену.")
        },
        confirmButton = {
            TextButton(onClick = onReplacePhoto) {
                Text("Заменить фото")
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

private enum class CopyListFormat {
    Row,
    Column,
}

private fun buildPrimaryNumbers(
    entries: List<WagonEntry>,
    settings: AppSettings,
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

    if (!settings.includeDirectionInCopy) {
        return body
    }

    return when (format) {
        CopyListFormat.Row -> {
            if (body.isBlank()) {
                "${settings.topDirectionLabel}; ${settings.bottomDirectionLabel}."
            } else {
                "${settings.topDirectionLabel}; $body; ${settings.bottomDirectionLabel}."
            }
        }
        CopyListFormat.Column -> {
            if (body.isBlank()) {
                "${settings.topDirectionLabel};\n${settings.bottomDirectionLabel}."
            } else {
                "${settings.topDirectionLabel};\n$body\n${settings.bottomDirectionLabel}."
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
        return "Карточка ${movingEntry.primaryNumber ?: ""} станет первой и единственной"
    }

    val boundedSlot = slotIndex.coerceIn(0, entries.size)
    val previous = entries.getOrNull(boundedSlot - 1)?.primaryNumber
    val next = entries.getOrNull(boundedSlot)?.primaryNumber
    val movingNumber = movingEntry.primaryNumber ?: "без номера"

    return when {
        boundedSlot == 0 -> "Карточка $movingNumber встанет первой, перед ${next ?: "списком"}"
        boundedSlot == entries.size -> "Карточка $movingNumber встанет последней, после ${previous ?: "списка"}"
        else -> "Карточка $movingNumber встанет между ${previous ?: "предыдущей"} и ${next ?: "следующей"}"
    }
}
