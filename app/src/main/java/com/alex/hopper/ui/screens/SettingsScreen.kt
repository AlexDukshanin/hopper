package com.alex.hopper.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.hopper.settings.AppIconMode
import com.alex.hopper.settings.AppSettings
import com.alex.hopper.settings.AppThemeMode
import com.alex.hopper.settings.NewEntryPosition
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    contentPadding: PaddingValues,
    onSelectTheme: (AppThemeMode) -> Unit,
    onSelectAppIcon: (AppIconMode) -> Unit,
    onNumberSizeChange: (Float) -> Unit,
    onNewEntryPositionChange: (NewEntryPosition) -> Unit,
    onIncludeDirectionInCopyChange: (Boolean) -> Unit,
    onPhotoQualityChange: (Int) -> Unit,
    onSharePhotoQualityChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionLabel = remember(context) { context.resolveAppVersionLabel() }
    var photoQualitySliderValue by remember(settings.photoQualityJpeg) {
        mutableFloatStateOf(settings.photoQualityJpeg.toFloat())
    }
    val currentPhotoQuality = photoQualitySliderValue.roundToInt().coerceIn(60, 92)
    var sharePhotoQualitySliderValue by remember(settings.sharePhotoQualityJpeg) {
        mutableFloatStateOf(settings.sharePhotoQualityJpeg.toFloat())
    }
    val currentSharePhotoQuality = sharePhotoQualitySliderValue.roundToInt().coerceIn(60, 92)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Настройки",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Выберите оформление и настройте размер номера в карточке.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                        text = "Качество фото",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Для съемки и отправки можно задать разное сжатие.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PhotoQualitySliderCard(
                        title = "Обычное фото",
                        helperText = "Влияет на новые снимки в журнале. Чем ниже качество, тем меньше размер файла.",
                        currentQuality = currentPhotoQuality,
                        sliderValue = photoQualitySliderValue,
                        onSliderChange = { photoQualitySliderValue = it },
                        onSliderSave = {
                            if (currentPhotoQuality != settings.photoQualityJpeg) {
                                onPhotoQualityChange(currentPhotoQuality)
                            }
                        },
                    )
                    PhotoQualitySliderCard(
                        title = "Фото при отправке",
                        helperText = "Используется только для файла Hopper с фото. Оригиналы в журнале не меняются.",
                        currentQuality = currentSharePhotoQuality,
                        sliderValue = sharePhotoQualitySliderValue,
                        onSliderChange = { sharePhotoQualitySliderValue = it },
                        onSliderSave = {
                            if (currentSharePhotoQuality != settings.sharePhotoQualityJpeg) {
                                onSharePhotoQualityChange(currentSharePhotoQuality)
                            }
                        },
                    )
                }
            }
        }

        item {
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Иконка приложения",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Выберите желтую, серую или зеленую иконку для рабочего стола.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconOptionCard(
                            modifier = Modifier.weight(1f),
                            title = "Желтая",
                            previewColor = Color(0xFFF1C40F),
                            selected = settings.appIconMode == AppIconMode.Yellow,
                            onClick = { onSelectAppIcon(AppIconMode.Yellow) },
                        )
                        IconOptionCard(
                            modifier = Modifier.weight(1f),
                            title = "Серая",
                            previewColor = Color(0xFF9CA3AF),
                            selected = settings.appIconMode == AppIconMode.Gray,
                            onClick = { onSelectAppIcon(AppIconMode.Gray) },
                        )
                        IconOptionCard(
                            modifier = Modifier.weight(1f),
                            title = "Зеленая",
                            previewColor = Color(0xFF22C55E),
                            selected = settings.appIconMode == AppIconMode.Green,
                            onClick = { onSelectAppIcon(AppIconMode.Green) },
                        )
                    }
                }
            }
        }

        item {
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Темы",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    ThemeOptionCard(
                        title = "Темная Supabase",
                        subtitle = "Темная база с зеленым акцентом",
                        swatches = listOf(
                            Color(0xFF171717),
                            Color(0xFF0F0F0F),
                            Color(0xFF3ECF8E),
                        ),
                        selected = settings.themeMode == AppThemeMode.SupabaseDark,
                        onClick = { onSelectTheme(AppThemeMode.SupabaseDark) },
                    )
                    ThemeOptionCard(
                        title = "Stripe",
                        subtitle = "Чистая светлая тема с фиолетово-синим акцентом",
                        swatches = listOf(
                            Color(0xFFF7F9FC),
                            Color(0xFFFFFFFF),
                            Color(0xFF635BFF),
                        ),
                        selected = settings.themeMode == AppThemeMode.StripeStyle,
                        onClick = { onSelectTheme(AppThemeMode.StripeStyle) },
                    )
                    ThemeOptionCard(
                        title = "Airbnb стиль",
                        subtitle = "Теплый светлый интерфейс с коралловым акцентом",
                        swatches = listOf(
                            Color(0xFFFFF8F8),
                            Color(0xFFFFFFFF),
                            Color(0xFFFF5A5F),
                        ),
                        selected = settings.themeMode == AppThemeMode.AirbnbStyle,
                        onClick = { onSelectTheme(AppThemeMode.AirbnbStyle) },
                    )
                    ThemeOptionCard(
                        title = "Hybrid clean",
                        subtitle = "Смешанный чистый стиль с холодными акцентами",
                        swatches = listOf(
                            Color(0xFFF4F7FB),
                            Color(0xFFFFFFFF),
                            Color(0xFF3A78F2),
                        ),
                        selected = settings.themeMode == AppThemeMode.HybridClean,
                        onClick = { onSelectTheme(AppThemeMode.HybridClean) },
                    )
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
                        text = "Размер номера",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${settings.numberFontSizeSp.toInt()} sp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = settings.numberFontSizeSp,
                        onValueChange = onNumberSizeChange,
                        valueRange = 18f..27f,
                        steps = 8,
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Превью номера",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "30870364",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = settings.numberFontSizeSp.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Порядок новых фото",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Выберите, где должен появляться каждый новый вагон.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SelectChipButton(
                            modifier = Modifier.weight(1f),
                            text = "Первыми",
                            selected = settings.newEntryPosition == NewEntryPosition.First,
                            onClick = { onNewEntryPositionChange(NewEntryPosition.First) },
                        )
                        SelectChipButton(
                            modifier = Modifier.weight(1f),
                            text = "Последними",
                            selected = settings.newEntryPosition == NewEntryPosition.Last,
                            onClick = { onNewEntryPositionChange(NewEntryPosition.Last) },
                        )
                    }
                }
            }
        }

        item {
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Копирование направления",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Добавлять название направления в начало и конец списка.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SelectChipButton(
                            modifier = Modifier.weight(1f),
                            text = "Да",
                            selected = settings.includeDirectionInCopy,
                            onClick = { onIncludeDirectionInCopyChange(true) },
                        )
                        SelectChipButton(
                            modifier = Modifier.weight(1f),
                            text = "Нет",
                            selected = !settings.includeDirectionInCopy,
                            onClick = { onIncludeDirectionInCopyChange(false) },
                        )
                    }
                }
            }
        }

        item {
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "О приложении",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = versionLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Разработчик: AlexDukshanin",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        onClick = { uriHandler.openUri("https://t.me/AlexDukshanin") },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text("Telegram: @AlexDukshanin")
                    }
                    Text(
                        text = "GitHub: скоро добавим",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun android.content.Context.resolveAppVersionLabel(): String {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val versionName = packageInfo.versionName ?: "0.0.0"
    val versionCode = packageInfo.longVersionCode
    return "HOPPER $versionName ($versionCode)"
}

@Composable
private fun PhotoQualitySliderCard(
    title: String,
    helperText: String,
    currentQuality: Int,
    sliderValue: Float,
    onSliderChange: (Float) -> Unit,
    onSliderSave: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "JPEG $currentQuality",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = describePhotoQuality(currentQuality),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Оценка размера одного фото: ${estimatePhotoRange(currentQuality)} МБ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Slider(
                value = sliderValue,
                onValueChange = onSliderChange,
                onValueChangeFinished = onSliderSave,
                valueRange = 60f..92f,
                steps = 31,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Очень низкое 60",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Высокое 92",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun describePhotoQuality(jpegQuality: Int): String {
    return when {
        jpegQuality <= 64 -> "Максимальное сжатие, самый легкий файл"
        jpegQuality <= 70 -> "Очень низкое качество, но журнал занимает меньше места"
        jpegQuality <= 76 -> "Низкое качество для длинных смен и больших архивов"
        jpegQuality <= 82 -> "Умеренное качество, хороший баланс размера"
        jpegQuality <= 88 -> "Хорошее качество с умеренным сжатием"
        else -> "Почти без потерь, файл будет крупнее"
    }
}

private fun estimatePhotoRange(jpegQuality: Int): String {
    return when {
        jpegQuality <= 64 -> "0.8-1.4"
        jpegQuality <= 70 -> "1.1-1.9"
        jpegQuality <= 76 -> "1.5-2.4"
        jpegQuality <= 82 -> "1.9-3.0"
        jpegQuality <= 88 -> "2.4-4.0"
        else -> "3.0-5.0"
    }
}

@Composable
private fun IconOptionCard(
    modifier: Modifier = Modifier,
    title: String,
    previewColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            },
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(previewColor, CircleShape),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SelectChipButton(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    swatches: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            },
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                swatches.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(color = color, shape = CircleShape),
                    )
                }
            }
        }
    }
}
