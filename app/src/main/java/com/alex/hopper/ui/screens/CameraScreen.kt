package com.alex.hopper.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import java.util.Locale
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

private object ScanGuideSpec {
    val cornerRadius: Dp = 24.dp
}

private const val CameraLogTag = "HopperCamera"
private const val MinCaptureZoomRatioFloor = 0.6f
private const val StandardReferenceFocalLengthMm = 4.5f
private val PreferredPreviewSize = Size(1280, 720)
private const val MaxCaptureZoomRatioCap = 6f

private enum class CaptureLensMode {
    Main,
    UltraWide,
}

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
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var bindError by remember { mutableStateOf<String?>(null) }
    var isCameraBound by remember { mutableStateOf(false) }
    var isPreviewStreaming by remember { mutableStateOf(false) }
    var zoomSetup by remember { mutableStateOf<CameraZoomSetup?>(null) }
    var boundLensMode by remember { mutableStateOf(CaptureLensMode.Main) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var minZoomRatio by remember { mutableStateOf(1f) }
    var maxZoomRatio by remember { mutableStateOf(1f) }
    val targetLensMode = zoomSetup
        ?.lensModeForDisplayZoom(zoomRatio)
        ?: CaptureLensMode.Main

    LaunchedEffect(previewView, lifecycleOwner, jpegQuality, targetLensMode) {
        val currentPreview = previewView ?: return@LaunchedEffect
        imageCapture = null
        boundCamera = null
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
                targetLensMode = targetLensMode,
                existingZoomSetup = zoomSetup,
            )
        }.onSuccess { binding ->
            cameraProvider = binding.provider
            imageCapture = binding.imageCapture
            boundCamera = binding.camera
            boundLensMode = binding.lensMode
            zoomSetup = binding.zoomSetup
            isCameraBound = true
            bindError = null
            minZoomRatio = binding.zoomSetup.minimumDisplayZoom()
            maxZoomRatio = binding.zoomSetup.maximumDisplayZoom()
            val clampedDisplayZoom = binding.zoomSetup.coerceDisplayZoom(zoomRatio)
            zoomRatio = clampedDisplayZoom
            binding.camera.cameraControl.setZoomRatio(
                binding.zoomSetup.actualZoomRatioFor(
                    displayZoomRatio = clampedDisplayZoom,
                    lensMode = binding.lensMode,
                ),
            )
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
            boundCamera = null
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
            zoomRatio = zoomRatio,
            minZoomRatio = minZoomRatio,
            maxZoomRatio = maxZoomRatio,
            zoomEnabled = isCameraBound && isPreviewStreaming && bindError == null,
            onZoomRatioChange = { requestedZoomRatio ->
                val setup = zoomSetup ?: return@CameraGuideOverlay
                val clampedZoomRatio = setup.coerceDisplayZoom(requestedZoomRatio)
                zoomRatio = clampedZoomRatio
                val camera = boundCamera ?: return@CameraGuideOverlay
                val desiredLensMode = setup.lensModeForDisplayZoom(clampedZoomRatio)
                if (desiredLensMode == boundLensMode) {
                    camera.cameraControl.setZoomRatio(
                        setup.actualZoomRatioFor(
                            displayZoomRatio = clampedZoomRatio,
                            lensMode = desiredLensMode,
                        ),
                    )
                }
            },
            onOpenScanFrameEditor = onOpenScanFrameEditor,
            onCapture = {
                val capture = imageCapture?.takeIf { isCameraBound && isPreviewStreaming }
                if (capture == null) {
                    viewModel.onCaptureError("Камера еще запускается. Подождите секунду и повторите.")
                    return@CameraGuideOverlay
                }
                val outputFile = viewModel.createCaptureFile()
                takePicture(
                    context = context,
                    imageCapture = capture,
                    outputFile = outputFile,
                    onSaved = { savedFile ->
                        if (replaceEntryId != null) {
                            viewModel.replacePhoto(replaceEntryId, savedFile)
                        } else if (collectionId != null) {
                            viewModel.processCapture(savedFile, scanFrameSettings, collectionId)
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
    zoomRatio: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float,
    zoomEnabled: Boolean,
    onZoomRatioChange: (Float) -> Unit,
    captureEnabled: Boolean,
    onOpenScanFrameEditor: () -> Unit,
    onCapture: () -> Unit,
) {
    var showHint by rememberSaveable { mutableStateOf(false) }
    val latestZoomRatio by rememberUpdatedState(zoomRatio)
    val latestMinZoomRatio by rememberUpdatedState(minZoomRatio)
    val latestMaxZoomRatio by rememberUpdatedState(maxZoomRatio)
    val latestOnZoomRatioChange by rememberUpdatedState(onZoomRatioChange)
    val canZoom = zoomEnabled && maxZoomRatio > minZoomRatio + 0.02f

    BoxWithConstraints(
        modifier = modifier
            .padding(vertical = 18.dp)
            .pointerInput(canZoom) {
                if (!canZoom) return@pointerInput
                detectTransformGestures(
                    panZoomLock = true,
                ) { _, _, gestureZoom, _ ->
                    if (abs(gestureZoom - 1f) < 0.01f) return@detectTransformGestures
                    latestOnZoomRatioChange(
                        (latestZoomRatio * gestureZoom).coerceIn(
                            latestMinZoomRatio,
                            latestMaxZoomRatio,
                        ),
                    )
                }
            },
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.size(42.dp))
                TextButton(
                    onClick = { showHint = !showHint },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White,
                        containerColor = Color.Transparent,
                    ),
                ) {
                    Text(if (showHint) "Скрыть подсказку" else "Показать подсказку")
                }
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

            if (canZoom) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 220.dp, max = 320.dp)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = formatZoomRatio(zoomRatio),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Slider(
                            value = zoomRatio.coerceIn(minZoomRatio, maxZoomRatio),
                            onValueChange = onZoomRatioChange,
                            valueRange = minZoomRatio..maxZoomRatio,
                            enabled = zoomEnabled,
                        )
                    }
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
    targetLensMode: CaptureLensMode,
    existingZoomSetup: CameraZoomSetup?,
): BoundCaptureSession {
    val cameraProvider = ProcessCameraProvider.getInstance(context).await()
    val sanitizedExistingZoomSetup = existingZoomSetup?.sanitizedForDevice()
    val requestedLensProfile = when (targetLensMode) {
        CaptureLensMode.Main -> sanitizedExistingZoomSetup?.main
        CaptureLensMode.UltraWide -> sanitizedExistingZoomSetup?.ultraWide
    }
    val requestedPhysicalCameraId = when (targetLensMode) {
        CaptureLensMode.Main -> null
        CaptureLensMode.UltraWide -> requestedLensProfile?.physicalCameraId
    }
    val cameraSelector = when {
        requestedLensProfile == null -> CameraSelector.DEFAULT_BACK_CAMERA
        requestedLensProfile.physicalCameraId != null -> CameraSelector.DEFAULT_BACK_CAMERA
        targetLensMode == CaptureLensMode.UltraWide &&
            requestedLensProfile.cameraId != sanitizedExistingZoomSetup?.main?.cameraId -> {
            selectorForCameraId(requestedLensProfile.cameraId)
        }

        else -> CameraSelector.DEFAULT_BACK_CAMERA
    }
    val requiresLensSpecificBinding = targetLensMode == CaptureLensMode.UltraWide &&
        requestedLensProfile != null &&
        (
            requestedLensProfile.physicalCameraId != null ||
                requestedLensProfile.cameraId != sanitizedExistingZoomSetup?.main?.cameraId
            )

    Log.d(
        CameraLogTag,
        "bindCamera targetLensMode=$targetLensMode requestedCameraId=${requestedLensProfile?.cameraId} " +
            "requestedPhysicalCameraId=$requestedPhysicalCameraId hasExistingZoomSetup=${existingZoomSetup != null}",
    )

    val bindingResult = runCatching {
        bindUseCases(
            cameraProvider = cameraProvider,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            jpegQuality = jpegQuality,
            cameraSelector = cameraSelector,
            physicalCameraId = requestedPhysicalCameraId,
        )
    }.recoverCatching { exception ->
        if (!requiresLensSpecificBinding) throw exception
        Log.w(
            CameraLogTag,
            "Lens-specific bind failed for cameraId=${requestedLensProfile?.cameraId} physicalCameraId=$requestedPhysicalCameraId. Falling back to logical back camera.",
            exception,
        )
        bindUseCases(
            cameraProvider = cameraProvider,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            jpegQuality = jpegQuality,
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
            physicalCameraId = null,
        )
    }.getOrThrow()

    val resolvedZoomSetup = (
        existingZoomSetup ?: buildCameraZoomSetup(
            cameraProvider = cameraProvider,
            mainCameraInfo = bindingResult.camera.cameraInfo,
        )
        ).sanitizedForDevice()
    val resolvedLensMode = if (bindingResult.physicalCameraId != null) {
        CaptureLensMode.UltraWide
    } else {
        CaptureLensMode.Main
    }

    return BoundCaptureSession(
        provider = cameraProvider,
        imageCapture = bindingResult.imageCapture,
        camera = bindingResult.camera,
        lensMode = resolvedLensMode,
        zoomSetup = resolvedZoomSetup,
    )
}

private data class CameraBindingResult(
    val camera: Camera,
    val imageCapture: ImageCapture,
    val physicalCameraId: String?,
)

private fun bindUseCases(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    jpegQuality: Int,
    cameraSelector: CameraSelector,
    physicalCameraId: String?,
): CameraBindingResult {
    val previewBuilder = Preview.Builder()
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
    val imageCaptureBuilder = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setJpegQuality(jpegQuality)

    physicalCameraId?.takeIf {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }?.let { cameraId ->
        Camera2Interop.Extender(previewBuilder)
            .setPhysicalCameraId(cameraId)
        Camera2Interop.Extender(imageCaptureBuilder)
            .setPhysicalCameraId(cameraId)
    }

    val preview = previewBuilder
        .build()
        .also { it.surfaceProvider = previewView.surfaceProvider }
    val imageCapture = imageCaptureBuilder.build()

    cameraProvider.unbindAll()
    val camera = cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        imageCapture,
    )
    Log.d(
        CameraLogTag,
        "bindUseCases success logicalCameraId=${Camera2CameraInfo.from(camera.cameraInfo).cameraId} " +
            "physicalCameraId=$physicalCameraId",
    )
    return CameraBindingResult(
        camera = camera,
        imageCapture = imageCapture,
        physicalCameraId = physicalCameraId,
    )
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

private data class BoundCaptureSession(
    val provider: ProcessCameraProvider,
    val imageCapture: ImageCapture,
    val camera: Camera,
    val lensMode: CaptureLensMode,
    val zoomSetup: CameraZoomSetup,
)

private data class CameraLensProfile(
    val cameraId: String,
    val physicalCameraId: String?,
    val focalLengthMm: Float,
    val minActualZoomRatio: Float,
    val maxActualZoomRatio: Float,
)

private data class CameraZoomSetup(
    val main: CameraLensProfile,
    val ultraWide: CameraLensProfile?,
) {
    private val ultraWideBaseDisplayZoomRatio: Float?
        get() = ultraWide?.let { profile ->
            (profile.focalLengthMm / main.focalLengthMm)
                .coerceAtMost(1f)
                .coerceAtLeast(MinCaptureZoomRatioFloor)
        }

    fun lensModeForDisplayZoom(displayZoomRatio: Float): CaptureLensMode {
        val ultraWideThreshold = ultraWideBaseDisplayZoomRatio ?: return CaptureLensMode.Main
        return if (displayZoomRatio < 1f && ultraWideThreshold < 1f) {
            CaptureLensMode.UltraWide
        } else {
            CaptureLensMode.Main
        }
    }

    fun coerceDisplayZoom(displayZoomRatio: Float): Float {
        return displayZoomRatio.coerceIn(minimumDisplayZoom(), maximumDisplayZoom())
    }

    fun minimumDisplayZoom(): Float {
        val ultraWideBase = ultraWideBaseDisplayZoomRatio
        return if (ultraWide != null && ultraWideBase != null) {
            maxOf(
                MinCaptureZoomRatioFloor,
                ultraWideBase * ultraWide.minActualZoomRatio,
            )
        } else {
            maxOf(
                MinCaptureZoomRatioFloor,
                main.minActualZoomRatio,
            )
        }
    }

    fun maximumDisplayZoom(): Float = main.maxActualZoomRatio

    fun actualZoomRatioFor(
        displayZoomRatio: Float,
        lensMode: CaptureLensMode,
    ): Float {
        val safeDisplayZoom = coerceDisplayZoom(displayZoomRatio)
        return when (lensMode) {
            CaptureLensMode.Main -> safeDisplayZoom.coerceIn(
                main.minActualZoomRatio,
                main.maxActualZoomRatio,
            )

            CaptureLensMode.UltraWide -> {
                val profile = ultraWide ?: main
                val baseDisplayZoom = ultraWideBaseDisplayZoomRatio ?: 1f
                (safeDisplayZoom / baseDisplayZoom).coerceIn(
                    profile.minActualZoomRatio,
                    profile.maxActualZoomRatio,
                )
            }
        }
    }

    fun sanitizedForDevice(): CameraZoomSetup {
        val canBindPhysicalUltraWide = ultraWide?.physicalCameraId != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        val canBindStandaloneUltraWide = ultraWide != null && ultraWide.cameraId != main.cameraId
        return if (canBindPhysicalUltraWide || canBindStandaloneUltraWide) {
            this
        } else {
            copy(ultraWide = null)
        }
    }
}

private fun buildCameraZoomSetup(
    cameraProvider: ProcessCameraProvider,
    mainCameraInfo: CameraInfo,
): CameraZoomSetup {
    val logicalCameraInfo = Camera2CameraInfo.from(mainCameraInfo)
    val mainProfile = mainCameraInfo.toLensProfile()
    val characteristicsMap = logicalCameraInfo.getCameraCharacteristicsMap()
    val physicalUltraWideProfile = characteristicsMap
        .asSequence()
        .filter { (cameraId, _) -> cameraId != logicalCameraInfo.cameraId }
        .mapNotNull { (cameraId, characteristics) ->
            characteristics.toPhysicalLensProfile(cameraId)?.takeIf { profile ->
                profile.focalLengthMm < mainProfile.focalLengthMm - 0.01f
            }
        }
        .minByOrNull(CameraLensProfile::focalLengthMm)
    val providerBackProfiles = cameraProvider.availableCameraInfos
        .asSequence()
        .filter(::isBackCameraInfo)
        .map(CameraInfo::toLensProfile)
        .filter { profile ->
            profile.cameraId != mainProfile.cameraId &&
                profile.focalLengthMm < mainProfile.focalLengthMm - 0.01f
        }
        .toList()
    val ultraWideProfile = (
        providerBackProfiles + listOfNotNull(physicalUltraWideProfile)
        ).minByOrNull(CameraLensProfile::focalLengthMm)

    Log.d(
        CameraLogTag,
        "Zoom setup logicalCameraId=${logicalCameraInfo.cameraId} physicalCount=${characteristicsMap.size} " +
            "availableBackCameras=${
                providerBackProfiles.joinToString(
                    prefix = "[",
                    postfix = "]",
                ) { "${it.cameraId}:${it.focalLengthMm}" }
            } " +
            "mainCameraId=${mainProfile.cameraId} mainFocal=${mainProfile.focalLengthMm} " +
            "ultraWideId=${ultraWideProfile?.cameraId} ultraWideFocal=${ultraWideProfile?.focalLengthMm}",
    )

    return CameraZoomSetup(
        main = mainProfile,
        ultraWide = ultraWideProfile,
    )
}

private fun CameraInfo.toLensProfile(): CameraLensProfile {
    val camera2Info = Camera2CameraInfo.from(this)
    val cameraId = camera2Info.cameraId
    val focalLengthMm = representativeFocalLengthMm(
        camera2Info.getCameraCharacteristic(
        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS,
        ),
    )
    val zoomState = zoomState.value
    val minActualZoomRatio = zoomState?.minZoomRatio ?: 1f
    val maxActualZoomRatio = minOf(
        zoomState?.maxZoomRatio ?: MaxCaptureZoomRatioCap,
        MaxCaptureZoomRatioCap,
    ).coerceAtLeast(minActualZoomRatio)
    return CameraLensProfile(
        cameraId = cameraId,
        physicalCameraId = null,
        focalLengthMm = focalLengthMm,
        minActualZoomRatio = minActualZoomRatio,
        maxActualZoomRatio = maxActualZoomRatio,
    )
}

private fun representativeFocalLengthMm(focalLengths: FloatArray?): Float {
    val values = focalLengths?.toList().orEmpty()
    return values.minByOrNull { abs(it - StandardReferenceFocalLengthMm) } ?: 1f
}

private fun CameraCharacteristics.toPhysicalLensProfile(
    cameraId: String,
): CameraLensProfile? {
    val lensFacing = get(CameraCharacteristics.LENS_FACING)
    if (lensFacing != CameraCharacteristics.LENS_FACING_BACK) return null

    val focalLengthMm = representativeFocalLengthMm(
        get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS),
    )
    val maxDigitalZoom = minOf(
        get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f,
        MaxCaptureZoomRatioCap,
    ).coerceAtLeast(1f)

    return CameraLensProfile(
        cameraId = cameraId,
        physicalCameraId = cameraId,
        focalLengthMm = focalLengthMm,
        minActualZoomRatio = 1f,
        maxActualZoomRatio = maxDigitalZoom,
    )
}

private fun isBackCameraInfo(cameraInfo: CameraInfo): Boolean {
    val lensFacing = Camera2CameraInfo.from(cameraInfo)
        .getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
    return lensFacing == CameraCharacteristics.LENS_FACING_BACK
}

private fun selectorForCameraId(cameraId: String): CameraSelector {
    return CameraSelector.Builder()
        .addCameraFilter { cameraInfos ->
            cameraInfos.filter { info ->
                Camera2CameraInfo.from(info).cameraId == cameraId
            }
        }
        .build()
}

private fun formatZoomRatio(ratio: Float): String {
    val rounded = ratio.roundToInt()
    return if (abs(ratio - rounded) < 0.05f) {
        "${rounded}x"
    } else {
        String.format(Locale.US, "%.1fx", ratio)
    }
}

private fun String?.toFriendlyCameraMessage(): String {
    val rawMessage = this?.trim().orEmpty()
    if (rawMessage.contains("Not bound to a valid Camera", ignoreCase = true)) {
        return "Камера еще не готова. Подождите секунду и повторите."
    }
    return rawMessage.ifBlank { "Ошибка камеры." }
}
