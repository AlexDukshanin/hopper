package com.alex.hopper.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.NavigateBefore
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alex.hopper.exchange.QrCodeGenerator
import com.alex.hopper.exchange.QrShareSession
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun QrShareScreen(
    session: QrShareSession?,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    if (session == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "QR-код пока не готов",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Вернитесь назад и попробуйте снова.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                }
            }
        }
        return
    }

    var currentIndex by rememberSaveable(session.chunks) { mutableIntStateOf(0) }
    var autoPlay by rememberSaveable(session.chunks) { mutableStateOf(session.chunks.size > 1) }

    LaunchedEffect(autoPlay, currentIndex, session.chunks.size) {
        if (!autoPlay || session.chunks.size <= 1) return@LaunchedEffect
        delay(1_500)
        currentIndex = (currentIndex + 1) % session.chunks.size
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        val density = LocalDensity.current
        val qrSizePx = remember(maxWidth, maxHeight, density) {
            with(density) {
                minOf(maxWidth * 0.86f, maxHeight * 0.42f).roundToPx().coerceAtLeast(240)
            }
        }
        val qrBitmap = remember(session.chunks, currentIndex, qrSizePx) {
            QrCodeGenerator.generate(session.chunks[currentIndex], qrSizePx)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Назад",
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "QR-код Hopper",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = session.collectionName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Box(
                            modifier = Modifier.padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR-код подборки",
                                modifier = Modifier.size(with(density) { qrSizePx.toDp() }),
                            )
                        }
                    }

                    Text(
                        text = if (session.chunks.size == 1) {
                            "Один QR-код готов к сканированию"
                        } else {
                            "Код ${currentIndex + 1} из ${session.chunks.size}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (session.chunks.size == 1) {
                            "Откройте Hopper на другом устройстве и отсканируйте этот код."
                        } else {
                            "На другом устройстве откройте импорт по QR и сканируйте коды по порядку."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (session.chunks.size > 1) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                autoPlay = false
                                currentIndex = if (currentIndex == 0) {
                                    session.chunks.lastIndex
                                } else {
                                    currentIndex - 1
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NavigateBefore,
                                contentDescription = "Предыдущий QR-код",
                            )
                        }

                        FilledTonalIconButton(
                            onClick = { autoPlay = !autoPlay },
                        ) {
                            Icon(
                                imageVector = if (autoPlay) {
                                    Icons.Rounded.Pause
                                } else {
                                    Icons.Rounded.PlayArrow
                                },
                                contentDescription = if (autoPlay) {
                                    "Остановить прокрутку QR-кодов"
                                } else {
                                    "Запустить прокрутку QR-кодов"
                                },
                            )
                        }

                        FilledTonalIconButton(
                            onClick = {
                                autoPlay = false
                                currentIndex = (currentIndex + 1) % session.chunks.size
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NavigateNext,
                                contentDescription = "Следующий QR-код",
                            )
                        }
                    }
                }
            }
        }
    }
}
