package com.alex.hopper.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.hopper.settings.ScanFrameSettings
import com.alex.hopper.ui.MainViewModel
import com.alex.hopper.util.await
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

private object ScanGuideSpec {
    val cornerRadius: Dp = 24.dp
}

private const val CameraLogTag = "HopperCamera"
private val PreferredPreviewSize = Size(1280, 720)

@Composable
fun CameraScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    replaceEntryId: Long?,
    collectionId: Long?,
    jpegQuality: Int,
    scanFrameSettings: ScanFrameSettings,
    onOpenScanFrameEditor: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    var hasPermission by remember {
        mutableStateOf(context.hasCameraPermission())
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
                    val granted = context.hasCameraPermission()
                    hasPermission = granted
                    shouldShowCamera = granted
                }

                Lifecycle.Event.ON_PAUSE -> {
                    shouldShowCamera = false
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetCameraState()
        }
    }

    if (!hasPermission) {
        CameraPermissionState(
            contentPadding = contentPadding,
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
        )
        return
    }

    if (!shouldShowCamera) {
        CameraPreparingState(
            contentPadding = contentPadding,
        )
        return
    }

    CameraCaptureState(
        viewModel = viewModel,
        contentPadding = contentPadding,
        statusMessage = cameraState.statusMessage,
        isProcessing = cameraState.isProcessing,
        errorMessage = cameraState.errorMessage,
        replaceEntryId = replaceEntryId,
        collectionId = collectionId,
        jpegQuality = jpegQuality,
        scanFrameSettings = scanFrameSettings,
        onOpenScanFrameEditor = onOpenScanFrameEditor,
    )
}

@Composable
private fun CameraPermissionState(
    contentPadding: PaddingValues,
    onRequestPermission: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard {
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
                    text = "Без разрешения камера не сможет сделать снимок вагона прямо внутри приложения.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalIconButton(onClick = onRequestPermission) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Разрешить камеру",
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreparingState(
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Запускаю камеру",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Подождите секунду, пока появится превью.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CameraCaptureState(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    statusMessage: String,
    isProcessing: Boolean,
    errorMessage: String?,
    replaceEntryId: Long?,
    collectionId: Long?,
    jpegQuality: Int,
    scanFrameSettings: ScanFrameSettings,
    onOpenScanFrameEditor: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var bindError by remember { mutableStateOf<String?>(null) }
    var isCameraBound by remember { mutableStateOf(false) }
    var isPreviewStreaming by remember { mutableStateOf(false) }

    LaunchedEffect(previewView, lifecycleOwner, jpegQuality) {
        val currentPreview = previewView ?: return@LaunchedEffect
        imageCapture = null
        bindError = null
        isCameraBound = false
        Log.d(CameraLogTag, "Bind capture camera started. jpegQuality=$jpegQuality replaceEntryId=$replaceEntryId collectionId=$collectionId")
        runCatching {
            currentPreview.awaitPreviewReady()
            bindCamera(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = currentPreview,
                jpegQuality = jpegQuality,
            )
        }.onSuccess { boundCamera ->
            cameraProvider = boundCamera.first
            imageCapture = boundCamera.second
            isCameraBound = true
            bindError = null
            Log.d(CameraLogTag, "Bind capture camera success. replaceEntryId=$replaceEntryId collectionId=$collectionId")
        }.onFailure { exception ->
            if (exception is CancellationException) {
                Log.d(CameraLogTag, "Bind capture camera cancelled by composition. replaceEntryId=$replaceEntryId collectionId=$collectionId")
                return@onFailure
            }
            bindError = exception.message.toFriendlyCameraMessage()
            isCameraBound = false
            Log.e(CameraLogTag, "Bind capture camera failed", exception)
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
            Log.d(CameraLogTag, "Dispose capture camera. replaceEntryId=$replaceEntryId collectionId=$collectionId")
            imageCapture = null
            isCameraBound = false
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

        CameraGuideOverlay(
            modifier = Modifier.fillMaxSize(),
            statusMessage = if (!isPreviewStreaming && bindError == null && !isProcessing) {
                "Запускаю камеру..."
            } else {
                statusMessage
            },
            scanFrameSettings = scanFrameSettings,
            isProcessing = isProcessing,
            errorMessage = errorMessage ?: bindError,
            onOpenScanFrameEditor = onOpenScanFrameEditor,
            onCapture = {
                val capture = imageCapture?.takeIf { isCameraBound && isPreviewStreaming }
                if (capture == null) {
                    viewModel.onCaptureError("Камера еще запускается. Подождите секунду и повторите.")
                    return@CameraGuideOverlay
                }
                val outputFile = viewModel.createCaptureFile()
                val scanBitmap = previewView?.bitmap?.let { cropPreviewBitmapForScan(it, scanFrameSettings) }
                takePicture(
                    context = context,
                    imageCapture = capture,
                    outputFile = outputFile,
                    onSaved = { savedFile ->
                        if (replaceEntryId != null) {
                            viewModel.replacePhoto(replaceEntryId, savedFile)
                        } else if (collectionId != null) {
                            viewModel.processCapture(savedFile, scanBitmap, collectionId)
                        } else {
                            if (savedFile.exists()) {
                                savedFile.delete()
                            }
                            viewModel.onCaptureError("Сначала выберите подборку.")
                        }
                    },
                    onError = { message ->
                        if (outputFile.exists()) {
                            outputFile.delete()
                        }
                        viewModel.onCaptureError(message)
                    },
                )
            },
            captureEnabled = imageCapture != null && isCameraBound && isPreviewStreaming && !isProcessing && bindError == null,
        )
    }
}

@Composable
private fun CameraGuideOverlay(
    modifier: Modifier = Modifier,
    statusMessage: String,
    scanFrameSettings: ScanFrameSettings,
    isProcessing: Boolean,
    errorMessage: String?,
    captureEnabled: Boolean,
    onOpenScanFrameEditor: () -> Unit,
    onCapture: () -> Unit,
) {
    var showHint by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier.padding(vertical = 18.dp),
    ) {
        val frameLeft = maxWidth * scanFrameSettings.leftFraction
        val frameWidth = maxWidth * scanFrameSettings.widthFraction
        val frameHeight = maxHeight * scanFrameSettings.heightFraction
        val frameTop = maxHeight * scanFrameSettings.topFraction
        val visibleStatus = when {
            errorMessage != null -> errorMessage
            isProcessing -> statusMessage
            else -> null
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val holeLeft = size.width * scanFrameSettings.leftFraction
            val holeWidth = size.width * scanFrameSettings.widthFraction
            val holeTop = size.height * scanFrameSettings.topFraction
            val holeHeight = size.height * scanFrameSettings.heightFraction
            val holeRight = holeLeft + holeWidth
            val holeBottom = holeTop + holeHeight

            drawRect(
                color = Color.Black.copy(alpha = 0.34f),
                size = androidx.compose.ui.geometry.Size(size.width, holeTop),
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.34f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, holeTop),
                size = androidx.compose.ui.geometry.Size(holeLeft, holeHeight),
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.34f),
                topLeft = androidx.compose.ui.geometry.Offset(holeRight, holeTop),
                size = androidx.compose.ui.geometry.Size(size.width - holeRight, holeHeight),
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.34f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, holeBottom),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - holeBottom),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                FilledTonalIconButton(
                    onClick = {
                        Log.d(CameraLogTag, "Open scan frame editor from camera overlay")
                        onOpenScanFrameEditor()
                    },
                    modifier = Modifier.size(42.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Настроить рамку",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            TextButton(
                onClick = { showHint = !showHint },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.Transparent,
                ),
            ) {
                Text(if (showHint) "Скрыть подсказку" else "Показать подсказку")
            }

            if (showHint) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CenterFocusStrong,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Заполняйте рамку номером почти целиком и снимайте ровно, без лишнего фона.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = frameLeft, top = frameTop)
                .width(frameWidth)
                .height(frameHeight)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(ScanGuideSpec.cornerRadius),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (visibleStatus != null) {
                Card {
                    Text(
                        text = visibleStatus,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (errorMessage == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }

            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                )
            } else {
                FilledTonalIconButton(
                    onClick = onCapture,
                    enabled = captureEnabled,
                    modifier = Modifier.size(76.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Сделать снимок",
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

private suspend fun bindCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    jpegQuality: Int,
): Pair<ProcessCameraProvider, ImageCapture> {
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
    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setJpegQuality(jpegQuality)
        .build()

    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        CameraSelector.DEFAULT_BACK_CAMERA,
        preview,
        imageCapture,
    )

    return cameraProvider to imageCapture
}

private fun takePicture(
    context: Context,
    imageCapture: ImageCapture,
    outputFile: File,
    onSaved: (File) -> Unit,
    onError: (String) -> Unit,
) {
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSaved(outputFile)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.message.toFriendlyCameraMessage())
            }
        },
    )
}

private fun cropPreviewBitmapForScan(
    bitmap: Bitmap,
    scanFrameSettings: ScanFrameSettings,
): Bitmap {
    val cropWidth = (bitmap.width * scanFrameSettings.widthFraction).roundToInt()
    val cropHeight = (bitmap.height * scanFrameSettings.heightFraction).roundToInt()
    val left = (bitmap.width * scanFrameSettings.leftFraction).roundToInt()
        .coerceIn(0, bitmap.width - cropWidth)
    val top = (bitmap.height * scanFrameSettings.topFraction).roundToInt()
        .coerceIn(0, bitmap.height - cropHeight)
    val safeWidth = cropWidth.coerceAtMost(bitmap.width - left.coerceAtLeast(0))
    val safeHeight = cropHeight.coerceAtMost(bitmap.height - top)

    return Bitmap.createBitmap(
        bitmap,
        left.coerceAtLeast(0),
        top,
        safeWidth,
        safeHeight,
    )
}

private fun Context.hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
}

private suspend fun PreviewView.awaitPreviewReady() {
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

private fun String?.toFriendlyCameraMessage(): String {
    val rawMessage = this?.trim().orEmpty()
    if (rawMessage.contains("Not bound to a valid Camera", ignoreCase = true)) {
        return "Камера еще не готова. Подождите секунду и повторите."
    }
    return rawMessage.ifBlank { "Ошибка камеры." }
}
