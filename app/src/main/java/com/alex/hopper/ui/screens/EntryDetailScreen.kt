package com.alex.hopper.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.alex.hopper.ui.MainViewModel
import com.alex.hopper.util.formatTimestamp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    entryId: Long,
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenPhoto: (Long) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val entryFlow = remember(entryId) { viewModel.observeEntry(entryId) }
    val entry by entryFlow.collectAsStateWithLifecycle(initialValue = null)
    var confirmDelete by remember { mutableStateOf(false) }

    if (entry == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CenterAlignedTopAppBar(
                title = { Text("Карточка вагона") },
                windowInsets = WindowInsets(0.dp),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
            )
            Text(
                text = "Запись не найдена или еще загружается.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    var noteDraft by rememberSaveable(entry!!.id, entry!!.note) {
        mutableStateOf(entry!!.note)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            CenterAlignedTopAppBar(
                title = { Text("Карточка вагона") },
                windowInsets = WindowInsets(0.dp),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { confirmDelete = true },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Удалить карточку",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        }

        item {
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AsyncImage(
                        model = File(entry!!.imagePath),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clickable { onOpenPhoto(entry!!.id) },
                        contentScale = ContentScale.Crop,
                    )
                    Text(
                        text = entry!!.primaryNumber ?: "Номер не найден",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                    Text(
                        text = "Снят: ${formatTimestamp(entry!!.createdAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = {
                            entry!!.primaryNumber?.let {
                                clipboardManager.setText(AnnotatedString(it))
                            }
                        },
                        enabled = !entry!!.primaryNumber.isNullOrBlank(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Копировать основной номер")
                    }
                }
            }
        }

        item {
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Заметка",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(20.dp),
                        label = { Text("Что важно по этому вагону") },
                    )
                    FilledTonalButton(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            viewModel.saveNote(entry!!.id, noteDraft)
                        },
                        enabled = noteDraft != entry!!.note,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранить заметку")
                    }
                }
            }
        }
    }

    if (confirmDelete && entry != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text("Удалить карточку?")
            },
            text = {
                Text("Карточка будет удалена. После удаления останется кнопка вернуть на 5 секунд.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.requestDelete(entry!!, popBack = true)
                        confirmDelete = false
                    },
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Отмена")
                }
            },
        )
    }
}
