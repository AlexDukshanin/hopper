package com.alex.hopper.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alex.hopper.data.CollectionSummary
import com.alex.hopper.data.WagonCollection
import com.alex.hopper.data.WagonEntry
import com.alex.hopper.ui.HopperUiMetrics
import com.alex.hopper.ui.rememberHopperUiMetrics
import com.alex.hopper.util.formatTimestamp

@Composable
fun SearchScreen(
    isGlobalSearch: Boolean,
    collections: List<CollectionSummary>,
    entries: List<WagonEntry>,
    currentCollection: WagonCollection?,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenCollection: (Long) -> Unit,
    onOpenEntry: (WagonEntry) -> Unit,
) {
    val metrics = rememberHopperUiMetrics()
    var query by rememberSaveable { mutableStateOf("") }
    var helpDialogVisible by rememberSaveable { mutableStateOf(false) }
    val trimmedQuery = query.trim()
    val collectionsById = remember(collections) { collections.associateBy { it.id } }
    val entryOrdersById = remember(entries) {
        entries
            .groupBy { it.collectionId }
            .flatMap { (_, collectionEntries) ->
                collectionEntries
                    .sortedBy { it.positionIndex }
                    .mapIndexed { index, entry -> entry.id to (index + 1) }
            }
            .toMap()
    }
    val matchingCollections = remember(trimmedQuery, isGlobalSearch, collections) {
        if (!isGlobalSearch || trimmedQuery.isBlank()) {
            emptyList()
        } else {
            collections.filter { item ->
                item.name.matchesSearch(trimmedQuery) || item.description.matchesSearch(trimmedQuery)
            }
        }
    }
    val matchingJournal = remember(trimmedQuery, currentCollection?.id, currentCollection?.name, currentCollection?.description) {
        currentCollection?.takeIf {
            trimmedQuery.isNotBlank() && (
                it.name.matchesSearch(trimmedQuery) ||
                    it.description.matchesSearch(trimmedQuery)
                )
        }
    }
    val matchingEntries = remember(trimmedQuery, entries) {
        if (trimmedQuery.isBlank()) {
            emptyList()
        } else {
            entries.filter { entry -> entry.matchesSearch(trimmedQuery) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(
            start = metrics.horizontalPadding,
            end = metrics.horizontalPadding,
            top = metrics.smallSpacing,
            bottom = metrics.verticalPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(metrics.smallSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Назад",
                    )
                }
                Text(
                    text = if (isGlobalSearch) "Общий поиск" else "Поиск внутренний",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = metrics.screenTitleSize,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { helpDialogVisible = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = "Справка по поиску",
                    )
                }
            }
        }

        item {
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(
                        horizontal = metrics.cardPadding,
                        vertical = metrics.cardPadding - 2.dp,
                    ),
                ) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = query,
                        onValueChange = { query = it.take(80) },
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Введите данные",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = if (metrics.isCompact) {
                                    MaterialTheme.typography.bodySmall
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                },
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }

        if (trimmedQuery.isBlank()) {
            item {
                SearchHintCard(
                    metrics = metrics,
                    text = if (isGlobalSearch) {
                        "Введите номер вагона, текст заметки или название подборки."
                    } else {
                        "Введите номер вагона или текст из заметки внутри этой подборки."
                    },
                )
            }
        } else {
            matchingJournal?.let { collection ->
                item {
                    SectionTitle("Описание журнала")
                }
                item {
                    JournalMatchCard(collection = collection)
                }
            }

            if (matchingCollections.isNotEmpty()) {
                item {
                    SectionTitle("Подборки")
                }
                items(
                    items = matchingCollections,
                    key = { collection -> "collection-${collection.id}" },
                ) { collection ->
                    SearchCollectionCard(
                        collection = collection,
                        onClick = { onOpenCollection(collection.id) },
                    )
                }
            }

            if (matchingEntries.isNotEmpty()) {
                item {
                    SectionTitle(if (isGlobalSearch) "Карточки вагонов" else "Результаты")
                }
                items(
                    items = matchingEntries,
                    key = { entry -> "entry-${entry.id}" },
                ) { entry ->
                    SearchEntryCard(
                        entry = entry,
                        order = entryOrdersById[entry.id],
                        collectionName = collectionsById[entry.collectionId]?.name,
                        showCollectionName = isGlobalSearch,
                        onClick = { onOpenEntry(entry) },
                    )
                }
            }

            if (matchingCollections.isEmpty() && matchingEntries.isEmpty() && matchingJournal == null) {
                item {
                    SearchHintCard(
                        metrics = metrics,
                        text = "Ничего не найдено. Попробуйте другой номер или текст.",
                    )
                }
            }
        }
    }

    if (helpDialogVisible) {
        SearchHelpDialog(
            isGlobalSearch = isGlobalSearch,
            onDismiss = { helpDialogVisible = false },
        )
    }
}

@Composable
private fun SearchHintCard(
    metrics: HopperUiMetrics,
    text: String,
) {
    ElevatedCard {
        Text(
            modifier = Modifier.padding(metrics.cardPadding),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchHelpDialog(
    isGlobalSearch: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isGlobalSearch) {
                    "Как работает поиск по всем подборкам"
                } else {
                    "Как работает поиск внутри подборки"
                },
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isGlobalSearch) {
                    Text("1. Поиск идет сразу по всем подборкам приложения.")
                    Text("2. Совпадения ищутся в названии подборки, ее описании, номерах вагонов и заметках карточек.")
                    Text("3. В результатах можно открыть и всю подборку, и конкретную карточку.")
                } else {
                    Text("1. Поиск работает только внутри текущей подборки.")
                    Text("2. Совпадения ищутся по описанию журнала, заметкам, распознанному тексту и номерам вагонов.")
                    Text("3. Результаты открывают карточки только из этой подборки.")
                }
                Text(
                    text = "Порядковый номер карточки показывается рядом с результатом, но в сам поиск не входит.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Понятно")
            }
        },
    )
}

@Composable
private fun SectionTitle(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun JournalMatchCard(
    collection: WagonCollection,
) {
    ElevatedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = collection.description.ifBlank { "Описание журнала пустое" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchCollectionCard(
    collection: CollectionSummary,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${collection.entryCount} карточек",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SearchEntryCard(
    entry: WagonEntry,
    order: Int?,
    collectionName: String?,
    showCollectionName: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                order?.let {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(
                            modifier = Modifier.size(30.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = it.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = entry.primaryNumber ?: "Номер не найден",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        text = formatTimestamp(entry.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            if (showCollectionName && !collectionName.isNullOrBlank()) {
                Text(
                    text = collectionName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = entry.note.ifBlank {
                    entry.recognizedText.lineSequence().firstOrNull()?.takeIf(String::isNotBlank)
                        ?: "Без описания"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun CollectionSummary.matchesSearch(query: String): Boolean =
    name.matchesSearch(query) || description.matchesSearch(query)

private fun WagonEntry.matchesSearch(query: String): Boolean {
    return primaryNumber.matchesSearch(query) ||
        note.matchesSearch(query) ||
        recognizedText.matchesSearch(query) ||
        candidateNumbers.any { it.matchesSearch(query) }
}

private fun String?.matchesSearch(query: String): Boolean {
    if (this.isNullOrBlank()) return false
    return contains(query, ignoreCase = true)
}
