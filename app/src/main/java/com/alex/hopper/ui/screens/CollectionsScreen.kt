package com.alex.hopper.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alex.hopper.data.CollectionSummary
import com.alex.hopper.settings.CollectionLayoutMode

@Composable
fun CollectionsScreen(
    collections: List<CollectionSummary>,
    selectedCollectionId: Long?,
    layoutMode: CollectionLayoutMode,
    contentPadding: PaddingValues,
    onOpenCollection: (Long) -> Unit,
    onCreateCollection: (String) -> Unit,
    onRenameCollection: (Long, String) -> Unit,
    onDeleteCollection: (Long) -> Unit,
    onLayoutModeChange: (CollectionLayoutMode) -> Unit,
) {
    var createDialogVisible by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<CollectionSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<CollectionSummary?>(null) }
    var pendingCreateName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingCreateName) {
        val createName = pendingCreateName ?: return@LaunchedEffect
        pendingCreateName = null
        onCreateCollection(createName)
    }

    if (layoutMode == CollectionLayoutMode.Grid) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CollectionsHeader(
                    layoutMode = layoutMode,
                    onAskCreate = { createDialogVisible = true },
                    onLayoutModeChange = onLayoutModeChange,
                )
            }
            if (collections.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyCollectionsState(onAskCreate = { createDialogVisible = true })
                }
            } else {
                items(collections, key = { it.id }) { collection ->
                    CollectionGridCard(
                        collection = collection,
                        selected = collection.id == selectedCollectionId,
                        onOpen = { onOpenCollection(collection.id) },
                        onRename = { renameTarget = collection },
                        onDelete = { deleteTarget = collection },
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CollectionsHeader(
                    layoutMode = layoutMode,
                    onAskCreate = { createDialogVisible = true },
                    onLayoutModeChange = onLayoutModeChange,
                )
            }
            if (collections.isEmpty()) {
                item {
                    EmptyCollectionsState(onAskCreate = { createDialogVisible = true })
                }
            } else {
                items(
                    count = collections.size,
                    key = { index -> collections[index].id },
                ) { index ->
                    val collection = collections[index]
                    CollectionListCard(
                        collection = collection,
                        selected = collection.id == selectedCollectionId,
                        onOpen = { onOpenCollection(collection.id) },
                        onRename = { renameTarget = collection },
                        onDelete = { deleteTarget = collection },
                    )
                }
            }
        }
    }

    if (createDialogVisible) {
        CollectionNameDialog(
            title = "Новая подборка",
            initialValue = "",
            confirmText = "Создать",
            onDismiss = { createDialogVisible = false },
            onConfirm = { value ->
                createDialogVisible = false
                pendingCreateName = value
            },
        )
    }

    renameTarget?.let { collection ->
        CollectionNameDialog(
            title = "Изменить название",
            initialValue = collection.name,
            confirmText = "Сохранить",
            onDismiss = { renameTarget = null },
            onConfirm = { value ->
                onRenameCollection(collection.id, value)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { collection ->
        ConfirmDeleteCollectionDialog(
            collection = collection,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                onDeleteCollection(collection.id)
                deleteTarget = null
            },
        )
    }
}

@Composable
private fun CollectionsHeader(
    layoutMode: CollectionLayoutMode,
    onAskCreate: () -> Unit,
    onLayoutModeChange: (CollectionLayoutMode) -> Unit,
) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Подборки",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Создавайте отдельные журналы и переключайтесь между ними.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(
                        onClick = { onLayoutModeChange(CollectionLayoutMode.Grid) },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GridView,
                            contentDescription = "Плитка",
                            tint = if (layoutMode == CollectionLayoutMode.Grid) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { onLayoutModeChange(CollectionLayoutMode.List) },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ViewList,
                            contentDescription = "Список",
                            tint = if (layoutMode == CollectionLayoutMode.List) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAskCreate,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Создать подборку")
            }
        }
    }
}

@Composable
private fun CollectionListCard(
    collection: CollectionSummary,
    selected: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = collection.description.ifBlank { "Без описания" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalIconButton(
                onClick = onRename,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Изменить название",
                )
            }
            FilledTonalIconButton(
                onClick = onDelete,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Удалить подборку",
                )
            }
        }
    }
}

@Composable
private fun CollectionGridCard(
    collection: CollectionSummary,
    selected: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = collection.description.ifBlank { "Без описания" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalIconButton(
                    onClick = onRename,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Изменить название",
                    )
                }
                FilledTonalIconButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Удалить подборку",
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCollectionsState(
    onAskCreate: () -> Unit,
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Подборок пока нет",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Создайте первую подборку, затем внутри нее можно будет снимать вагоны и вести карточки.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(onClick = onAskCreate) {
                Text("Создать первую подборку")
            }
        }
    }
}

@Composable
private fun CollectionNameDialog(
    title: String,
    initialValue: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = { value = it.take(48) },
                singleLine = true,
                label = { Text("Название подборки") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }) {
                Text(confirmText)
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
private fun ConfirmDeleteCollectionDialog(
    collection: CollectionSummary,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить подборку?") },
        text = {
            Text(
                text = "Подборка «${collection.name}» и все карточки внутри нее будут удалены.",
            )
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
