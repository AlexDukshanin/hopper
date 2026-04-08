package com.alex.hopper.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Size
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.hopper.ui.MainViewModel
import com.alex.hopper.ui.QrImportUiState
import com.alex.hopper.util.await
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException

private const val QrImportLogTag = "HopperQrImport"
private val QrPreviewSize = Size(1280, 720)

@Composable
fun QrImportScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val qrImportState by viewModel.qrImportState.collectAsStateWithLifecycle()
    var hasPermission by remember {
        mutableStateOf(context.hasQrCameraPermission())
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

    LaunchedEffect(Unit) {
        viewModel.resetQrImportState()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetQrImportState()
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val granted = context.hasQrCameraPermission()
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
        QrImportPermissionState(
            contentPadding = contentPadding,
            onBack = onBack,
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        )
        return
    }

    if (!shouldShowCamera) {
        QrImportPreparingState(
            contentPadding = contentPadding,
            onBack = onBack,
        )
        return
    }

    QrImportCameraState(
        viewModel = viewModel,
        contentPadding = contentPadding,
        qrImportState = qrImportState,
        onBack = onBack,
    )
}

@Composable
private fun QrImportPermissionState(
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
                    text = "Без камеры Hopper не сможет считать QR-код подборки.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalIconButton(onClick = onRequestPermission) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = "Разрешить камеру",
                    )
                }
            }
        }
    }
}

@Composable
private fun QrImportPreparingState(
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
                    text = "Запускаю сканер",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Сейчас откроется камера для чтения QR-кода Hopper.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QrImportCameraState(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    qrImportState: QrImportUiState,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val qrReader = remember {
        MultiFormatReader().apply {
            setHints(
                EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                    put(DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
                    put(DecodeHintType.TRY_HARDER, true)
                },
            )
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val isAnalyzing = remember { AtomicBoolean(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isPreviewStreaming by remember { mutableStateOf(false) }
    var bindError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(previewView, lifecycleOwner) {
        val currentPreview = previewView ?: return@LaunchedEffect
        bindError = null
        runCatching {
            currentPreview.awaitPreviewReadyForQr()
            bindQrScannerCamera(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = currentPreview,
                qrReader = qrReader,
                analysisExecutor = analysisExecutor,
                isAnalyzing = isAnalyzing,
                onQrCodeScanned = viewModel::onQrCodeScanned,
            )
        }.onSuccess { boundProvider ->
            cameraProvider = boundProvider
        }.onFailure { exception ->
            if (exception is CancellationException) {
                return@onFailure
            }
            bindError = exception.message ?: "Не удалось запустить QR-сканер"
            android.util.Log.e(QrImportLogTag, "Bind QR scanner failed", exception)
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
            cameraProvider?.unbindAll()
            analysisExecutor.shutdown()
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

        QrImportOverlay(
            modifier = Modifier.fillMaxSize(),
            qrImportState = qrImportState,
            statusMessage = when {
                bindError != null -> bindError.orEmpty()
                !isPreviewStreaming -> "Запускаю камеру..."
                else -> qrImportState.statusMessage
            },
            onBack = onBack,
        )
    }
}

@Composable
private fun QrImportOverlay(
    modifier: Modifier = Modifier,
    qrImportState: QrImportUiState,
    statusMessage: String,
    onBack: () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.padding(vertical = 18.dp),
    ) {
        val guideSize = minOf(maxWidth * 0.74f, maxHeight * 0.38f)
        val guideTop = maxHeight * 0.23f
        val guideLeft = (maxWidth - guideSize) / 2f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val holeLeft = guideLeft.toPx()
            val holeTopPx = guideTop.toPx()
            val holeSize = guideSize.toPx()
            val holeRight = holeLeft + holeSize
            val holeBottom = holeTopPx + holeSize

            drawRect(
                color = Color.Black.copy(alpha = 0.4f),
                size = androidx.compose.ui.geometry.Size(size.width, holeTopPx),
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.4f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, holeTopPx),
                size = androidx.compose.ui.geometry.Size(holeLeft, holeSize),
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.4f),
                topLeft = androidx.compose.ui.geometry.Offset(holeRight, holeTopPx),
                size = androidx.compose.ui.geometry.Size(size.width - holeRight, holeSize),
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.4f),
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
                    text = "Импорт по QR",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = guideLeft, top = guideTop)
                .size(guideSize),
            shape = RoundedCornerShape(26.dp),
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            ),
        ) {}

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val shouldHidePrimaryHint = qrImportState.totalChunks == null &&
                    !qrImportState.isImporting &&
                    qrImportState.errorMessage == null &&
                    statusMessage.startsWith("Наведите камеру на QR-код Hopper")

                if (!shouldHidePrimaryHint) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (qrImportState.totalChunks != null) {
                    Text(
                        text = "Получено ${qrImportState.scannedChunks} из ${qrImportState.totalChunks}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = "Наведите камеру на QR-код Hopper другого устройства.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (qrImportState.errorMessage != null) {
                    Text(
                        text = qrImportState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (qrImportState.isImporting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Сохраняю подборку...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private suspend fun bindQrScannerCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    qrReader: MultiFormatReader,
    analysisExecutor: java.util.concurrent.ExecutorService,
    isAnalyzing: AtomicBoolean,
    onQrCodeScanned: (String) -> Unit,
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
                        QrPreviewSize,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    ),
                )
                .build(),
        )
        .build()
        .also {
            it.surfaceProvider = previewView.surfaceProvider
        }

    val analysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
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
                        QrPreviewSize,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    ),
                )
                .build(),
        )
        .build()

    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
        if (!isAnalyzing.compareAndSet(false, true)) {
            imageProxy.close()
            return@setAnalyzer
        }

        try {
            decodeQrValue(imageProxy, qrReader)?.let(onQrCodeScanned)
        } catch (exception: Exception) {
            android.util.Log.e(QrImportLogTag, "QR scan failed", exception)
        } finally {
            imageProxy.close()
            isAnalyzing.set(false)
            qrReader.reset()
        }
    }

    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        CameraSelector.DEFAULT_BACK_CAMERA,
        preview,
        analysis,
    )
    return cameraProvider
}

private fun decodeQrValue(
    imageProxy: ImageProxy,
    qrReader: MultiFormatReader,
): String? {
    val frame = imageProxy.toLuminanceFrame()
    val source = PlanarYUVLuminanceSource(
        frame.bytes,
        frame.width,
        frame.height,
        0,
        0,
        frame.width,
        frame.height,
        false,
    )

    return try {
        qrReader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
    } catch (_: NotFoundException) {
        null
    }
}

private fun ImageProxy.toLuminanceFrame(): LuminanceFrame {
    val yPlane = planes.first()
    val planeWidth = width
    val planeHeight = height
    val sourceBytes = yPlane.buffer.run {
        rewind()
        ByteArray(remaining()).also(::get)
    }
    val luminanceBytes = ByteArray(planeWidth * planeHeight)

    if (yPlane.pixelStride == 1 && yPlane.rowStride == planeWidth) {
        System.arraycopy(sourceBytes, 0, luminanceBytes, 0, luminanceBytes.size)
    } else {
        for (row in 0 until planeHeight) {
            val rowOffset = row * yPlane.rowStride
            for (column in 0 until planeWidth) {
                luminanceBytes[row * planeWidth + column] =
                    sourceBytes[rowOffset + (column * yPlane.pixelStride)]
            }
        }
    }

    val frame = LuminanceFrame(
        bytes = luminanceBytes,
        width = planeWidth,
        height = planeHeight,
    )

    return when (imageInfo.rotationDegrees) {
        90 -> frame.rotate90()
        180 -> frame.rotate180()
        270 -> frame.rotate270()
        else -> frame
    }
}

private data class LuminanceFrame(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
) {
    fun rotate90(): LuminanceFrame {
        val rotated = ByteArray(bytes.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val newX = height - 1 - y
                val newY = x
                rotated[newY * height + newX] = bytes[y * width + x]
            }
        }
        return LuminanceFrame(rotated, height, width)
    }

    fun rotate180(): LuminanceFrame {
        val rotated = ByteArray(bytes.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val newX = width - 1 - x
                val newY = height - 1 - y
                rotated[newY * width + newX] = bytes[y * width + x]
            }
        }
        return LuminanceFrame(rotated, width, height)
    }

    fun rotate270(): LuminanceFrame {
        val rotated = ByteArray(bytes.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val newX = y
                val newY = width - 1 - x
                rotated[newY * height + newX] = bytes[y * width + x]
            }
        }
        return LuminanceFrame(rotated, height, width)
    }
}

private suspend fun PreviewView.awaitPreviewReadyForQr() {
    if (width > 0 && height > 0) return
    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val listener = object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                view: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int,
            ) {
                if (right > left && bottom > top) {
                    view.removeOnLayoutChangeListener(this)
                    if (continuation.isActive) {
                        continuation.resume(Unit) {}
                    }
                }
            }
        }
        addOnLayoutChangeListener(listener)
        continuation.invokeOnCancellation {
            removeOnLayoutChangeListener(listener)
        }
    }
}

private fun Context.hasQrCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
