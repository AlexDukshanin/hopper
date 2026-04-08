package com.alex.hopper.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alex.hopper.settings.ScanFrameSettings
import com.alex.hopper.util.await
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

private const val MinFrameWidthFraction = 0.7f
private const val MaxFrameWidthFraction = 1.0f
private const val MinFrameHeightFraction = 0.16f
private const val MaxFrameHeightFraction = 0.42f
private const val MinFrameTopFraction = 0.02f
private const val CameraLogTag = "HopperCamera"
private val PreferredPreviewSize = Size(1280, 720)

private enum class ResizeEdge {
    Left,
    Right,
    Top,
    Bottom,
}

@Composable
fun ScanFrameEditorScreen(
    initialSettings: ScanFrameSettings,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (ScanFrameSettings) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var draftSettings by remember(initialSettings) { mutableStateOf(initialSettings.normalized()) }
    var hasPermission by remember {
        mutableStateOf(context.hasCameraPermissionForPreview())
    }
    var shouldShowCamera by remember {
        mutableStateOf(
            hasPermission && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        shouldShowCamera = granted && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val granted = context.hasCameraPermissionForPreview()
                    hasPermission = granted
                    shouldShowCamera = granted
                }

                Lifecycle.Event.ON_PAUSE -> shouldShowCamera = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!hasPermission) {
        ScanFramePermissionState(
            contentPadding = contentPadding,
            onBack = onBack,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        )
        return
    }

    if (!shouldShowCamera) {
        ScanFramePreparingState(
            contentPadding = contentPadding,
            onBack = onBack,
        )
        return
    }

    ScanFrameEditorContent(
        contentPadding = contentPadding,
        draftSettings = draftSettings,
        onDraftSettingsChange = { draftSettings = it.normalized() },
        onReset = {
            Log.d(CameraLogTag, "Reset scan frame settings to defaults")
            draftSettings = ScanFrameSettings().normalized()
        },
        onBack = onBack,
        onSave = {
            val normalized = draftSettings.normalized()
            Log.d(
                CameraLogTag,
                "Save scan frame settings left=${normalized.leftFraction} top=${normalized.topFraction} width=${normalized.widthFraction} height=${normalized.heightFraction}",
            )
            onSave(normalized)
            onBack()
        },
    )
}

@Composable
private fun ScanFramePermissionState(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(20.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Назад",
            )
        }

        ElevatedCard(
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Нужен доступ к камере",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Без камеры не получится показать рамку так, как ее видит пользователь.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalIconButton(onClick = onRequestPermission) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Разрешить камеру",
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanFramePreparingState(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(20.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Назад",
            )
        }

        ElevatedCard(
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Запускаю превью",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Сейчас откроется камера для настройки рамки без съемки.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ScanFrameEditorContent(
    contentPadding: PaddingValues,
    draftSettings: ScanFrameSettings,
    onDraftSettingsChange: (ScanFrameSettings) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isPreviewStreaming by remember { mutableStateOf(false) }
    var bindError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(previewView, lifecycleOwner) {
        val currentPreview = previewView ?: return@LaunchedEffect
        bindError = null
        Log.d(CameraLogTag, "Bind scan frame preview started")
        runCatching {
            currentPreview.awaitPreviewReadyForPreview()
            bindPreviewOnly(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = currentPreview,
            )
        }.onSuccess { boundCameraProvider ->
            cameraProvider = boundCameraProvider
            Log.d(CameraLogTag, "Bind scan frame preview success")
        }.onFailure { exception ->
            if (exception is CancellationException) {
                Log.d(CameraLogTag, "Bind scan frame preview cancelled by composition")
                return@onFailure
            }
            bindError = exception.message.toFriendlyPreviewMessage()
            Log.e(CameraLogTag, "Bind scan frame preview failed", exception)
        }
    }

    DisposableEffect(previewView) {
        val currentPreview = previewView
        if (currentPreview == null) {
            isPreviewStreaming = false
            onDispose { }
        } else {
            val observer = Observer<PreviewView.StreamState> { state ->
                isPreviewStreaming = state == PreviewView.StreamState.STREAMING
            }
            currentPreview.previewStreamState.observeForever(observer)
            onDispose {
                currentPreview.previewStreamState.removeObserver(observer)
                isPreviewStreaming = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(CameraLogTag, "Dispose scan frame preview")
            cameraProvider?.unbindAll()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        AndroidView(
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }.also { previewView = it }
            },
            modifier = Modifier.fillMaxSize(),
        )

        ScanFrameInteractiveOverlay(
            modifier = Modifier.fillMaxSize(),
            draftSettings = draftSettings,
            onDraftSettingsChange = onDraftSettingsChange,
            onBack = onBack,
            onReset = onReset,
            onSave = onSave,
            message = when {
                bindError != null -> bindError.orEmpty()
                !isPreviewStreaming -> "Запускаю превью..."
                else -> "Перетаскивайте рамку и тяните за круглые маркеры по краям."
            },
            hasError = bindError != null,
        )
    }
}

@Composable
private fun ScanFrameInteractiveOverlay(
    modifier: Modifier = Modifier,
    draftSettings: ScanFrameSettings,
    onDraftSettingsChange: (ScanFrameSettings) -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    message: String,
    hasError: Boolean,
) {
    BoxWithConstraints(
        modifier = modifier.padding(vertical = 18.dp),
    ) {
        val density = LocalDensity.current
        val currentDraftSettings = rememberUpdatedState(draftSettings)
        val currentOnDraftSettingsChange = rememberUpdatedState(onDraftSettingsChange)
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val frameLeftPx = widthPx * draftSettings.leftFraction
        val frameTopPx = heightPx * draftSettings.topFraction
        val frameWidthPx = widthPx * draftSettings.widthFraction
        val frameHeightPx = heightPx * draftSettings.heightFraction
        val handleTouchSizePx = with(density) { 58.dp.toPx() }
        val handleTouchSizeDp = with(density) { handleTouchSizePx.toDp() }
        val handleOffset = (handleTouchSizePx / 2f).roundToInt()
        val handleInsetPx = handleTouchSizePx * 0.24f
        val frameWidthDp = with(density) { frameWidthPx.toDp() }
        val frameHeightDp = with(density) { frameHeightPx.toDp() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val holeLeft = size.width * draftSettings.leftFraction
            val holeWidth = size.width * draftSettings.widthFraction
            val holeTop = size.height * draftSettings.topFraction
            val holeHeight = size.height * draftSettings.heightFraction
            val holeRight = holeLeft + holeWidth
            val holeBottom = holeTop + holeHeight

            drawRect(
                color = Color.Black.copy(alpha = 0.38f),
                size = androidx.compose.ui.geometry.Size(size.width, holeTop),
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.38f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, holeTop),
                size = androidx.compose.ui.geometry.Size(holeLeft, holeHeight),
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.38f),
                topLeft = androidx.compose.ui.geometry.Offset(holeRight, holeTop),
                size = androidx.compose.ui.geometry.Size(size.width - holeRight, holeHeight),
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.38f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, holeBottom),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - holeBottom),
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Назад",
                    )
                }
                Text(
                    text = "Настройка рамки",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = onSave) {
                    Text("Сохранить")
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = frameLeftPx.roundToInt(),
                        y = frameTopPx.roundToInt(),
                    )
                }
                .width(frameWidthDp)
                .height(frameHeightDp)
                .pointerInput(widthPx, heightPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        currentOnDraftSettingsChange.value(
                            currentDraftSettings.value.moveBy(
                                dxFraction = dragAmount.x / widthPx,
                                dyFraction = dragAmount.y / heightPx,
                            ),
                        )
                    }
                }
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(24.dp),
                ),
        )

        FrameHandle(
            x = (frameLeftPx - handleInsetPx).roundToInt().coerceAtLeast(0),
            y = (frameTopPx + frameHeightPx / 2f).roundToInt() - handleOffset,
            touchSize = handleTouchSizeDp,
            onDrag = { dragAmount ->
                currentOnDraftSettingsChange.value(
                    currentDraftSettings.value.resizeByEdge(
                        edge = ResizeEdge.Left,
                        deltaFraction = dragAmount.x / widthPx,
                    ),
                )
            },
        )
        FrameHandle(
            x = (
                frameLeftPx + frameWidthPx - handleTouchSizePx + handleInsetPx
            ).roundToInt().coerceAtMost((widthPx - handleTouchSizePx).roundToInt()),
            y = (frameTopPx + frameHeightPx / 2f).roundToInt() - handleOffset,
            touchSize = handleTouchSizeDp,
            onDrag = { dragAmount ->
                currentOnDraftSettingsChange.value(
                    currentDraftSettings.value.resizeByEdge(
                        edge = ResizeEdge.Right,
                        deltaFraction = dragAmount.x / widthPx,
                    ),
                )
            },
        )
        FrameHandle(
            x = (frameLeftPx + frameWidthPx / 2f).roundToInt() - handleOffset,
            y = (frameTopPx - handleInsetPx).roundToInt().coerceAtLeast(0),
            touchSize = handleTouchSizeDp,
            onDrag = { dragAmount ->
                currentOnDraftSettingsChange.value(
                    currentDraftSettings.value.resizeByEdge(
                        edge = ResizeEdge.Top,
                        deltaFraction = dragAmount.y / heightPx,
                    ),
                )
            },
        )
        FrameHandle(
            x = (frameLeftPx + frameWidthPx / 2f).roundToInt() - handleOffset,
            y = (
                frameTopPx + frameHeightPx - handleTouchSizePx + handleInsetPx
            ).roundToInt().coerceAtMost((heightPx - handleTouchSizePx).roundToInt()),
            touchSize = handleTouchSizeDp,
            onDrag = { dragAmount ->
                currentOnDraftSettingsChange.value(
                    currentDraftSettings.value.resizeByEdge(
                        edge = ResizeEdge.Bottom,
                        deltaFraction = dragAmount.y / heightPx,
                    ),
                )
            },
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onReset,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                        )
                        Text(
                            text = "Сбросить",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onSave,
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.FrameHandle(
    x: Int,
    y: Int,
    touchSize: androidx.compose.ui.unit.Dp,
    onDrag: (androidx.compose.ui.geometry.Offset) -> Unit,
) {
    val currentOnDrag = rememberUpdatedState(onDrag)
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset { IntOffset(x, y) }
            .size(touchSize)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    currentOnDrag.value(dragAmount)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                )
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ),
        )
    }
}

private fun ScanFrameSettings.moveBy(
    dxFraction: Float,
    dyFraction: Float,
): ScanFrameSettings = copy(
    leftFraction = (leftFraction + dxFraction).coerceIn(0f, (1f - widthFraction).coerceAtLeast(0f)),
    topFraction = (topFraction + dyFraction).coerceIn(MinFrameTopFraction, (1f - heightFraction).coerceAtLeast(MinFrameTopFraction)),
).normalized()

private fun ScanFrameSettings.resizeByEdge(
    edge: ResizeEdge,
    deltaFraction: Float,
): ScanFrameSettings {
    return when (edge) {
        ResizeEdge.Left -> {
            val right = leftFraction + widthFraction
            var newLeft = (leftFraction + deltaFraction).coerceAtMost(right - MinFrameWidthFraction)
            newLeft = newLeft.coerceAtLeast(0f)
            newLeft = newLeft.coerceAtLeast(right - MaxFrameWidthFraction)
            copy(
                leftFraction = newLeft,
                widthFraction = right - newLeft,
            ).normalized()
        }

        ResizeEdge.Right -> {
            val minRight = leftFraction + MinFrameWidthFraction
            val maxRight = minOf(1f, leftFraction + MaxFrameWidthFraction)
            val newRight = (leftFraction + widthFraction + deltaFraction).coerceIn(minRight, maxRight)
            copy(widthFraction = newRight - leftFraction).normalized()
        }

        ResizeEdge.Top -> {
            val bottom = topFraction + heightFraction
            var newTop = (topFraction + deltaFraction).coerceAtMost(bottom - MinFrameHeightFraction)
            newTop = newTop.coerceAtLeast(MinFrameTopFraction)
            newTop = newTop.coerceAtLeast(bottom - MaxFrameHeightFraction)
            copy(
                topFraction = newTop,
                heightFraction = bottom - newTop,
            ).normalized()
        }

        ResizeEdge.Bottom -> {
            val minBottom = topFraction + MinFrameHeightFraction
            val maxBottom = minOf(1f, topFraction + MaxFrameHeightFraction)
            val newBottom = (topFraction + heightFraction + deltaFraction).coerceIn(minBottom, maxBottom)
            copy(heightFraction = newBottom - topFraction).normalized()
        }
    }
}

private fun ScanFrameSettings.normalized(): ScanFrameSettings {
    val normalizedWidth = widthFraction.coerceIn(MinFrameWidthFraction, MaxFrameWidthFraction)
    val normalizedHeight = heightFraction.coerceIn(MinFrameHeightFraction, MaxFrameHeightFraction)
    return copy(
        leftFraction = leftFraction.coerceIn(0f, (1f - normalizedWidth).coerceAtLeast(0f)),
        widthFraction = normalizedWidth,
        heightFraction = normalizedHeight,
        topFraction = topFraction.coerceIn(
            MinFrameTopFraction,
            (1f - normalizedHeight).coerceAtLeast(MinFrameTopFraction),
        ),
    )
}

private suspend fun bindPreviewOnly(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
): ProcessCameraProvider {
    val cameraProvider = ProcessCameraProvider.getInstance(context).await()
    val preview = Preview.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(
                    AspectRatioStrategy(
                        AspectRatio.RATIO_16_9,
                        AspectRatioStrategy.FALLBACK_RULE_AUTO,
                    ),
                )
                .setResolutionStrategy(
                    ResolutionStrategy(
                        PreferredPreviewSize,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    ),
                )
                .build(),
        )
        .build()
        .also { it.surfaceProvider = previewView.surfaceProvider }
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        CameraSelector.DEFAULT_BACK_CAMERA,
        preview,
    )
    return cameraProvider
}

private fun Context.hasCameraPermissionForPreview(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
}

private suspend fun PreviewView.awaitPreviewReadyForPreview() {
    suspendCancellableCoroutine { continuation ->
        if (isAttachedToWindow && width > 0 && height > 0) {
            post {
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
            return@suspendCancellableCoroutine
        }

        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                removeOnAttachStateChangeListener(this)
                post {
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            }

            override fun onViewDetachedFromWindow(view: View) = Unit
        }

        addOnAttachStateChangeListener(attachListener)
        continuation.invokeOnCancellation {
            removeOnAttachStateChangeListener(attachListener)
        }
    }
}

private fun String?.toFriendlyPreviewMessage(): String {
    val rawMessage = this?.trim().orEmpty()
    return rawMessage.ifBlank { "Не удалось открыть превью камеры." }
}
