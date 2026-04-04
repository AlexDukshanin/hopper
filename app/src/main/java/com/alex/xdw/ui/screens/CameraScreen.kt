package com.alex.xdw.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.xdw.ui.MainViewModel
import com.alex.xdw.util.await
import java.io.File
import kotlin.math.roundToInt

private object ScanGuideSpec {
    const val widthFraction = 1f
    const val heightFraction = 0.255f
    const val topFraction = 0.30f
    val cornerRadius: Dp = 24.dp
}

@Composable
fun CameraScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    replaceEntryId: Long?,
) {
    val context = LocalContext.current
    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    var hasPermission by remember {
        mutableStateOf(context.hasCameraPermission())
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
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

    CameraCaptureState(
        viewModel = viewModel,
        contentPadding = contentPadding,
        statusMessage = cameraState.statusMessage,
        isProcessing = cameraState.isProcessing,
        errorMessage = cameraState.errorMessage,
        replaceEntryId = replaceEntryId,
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
private fun CameraCaptureState(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    statusMessage: String,
    isProcessing: Boolean,
    errorMessage: String?,
    replaceEntryId: Long?,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var bindError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(previewView, lifecycleOwner) {
        val currentPreview = previewView ?: return@LaunchedEffect
        runCatching {
            bindCamera(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = currentPreview,
            )
        }.onSuccess { boundCamera ->
            cameraProvider = boundCamera.first
            imageCapture = boundCamera.second
            bindError = null
        }.onFailure { exception ->
            bindError = exception.message ?: "Не удалось запустить камеру."
        }
    }

    DisposableEffect(cameraProvider) {
        onDispose {
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
                }.also { previewView = it }
            },
            modifier = Modifier.fillMaxSize(),
        )

        CameraGuideOverlay(
            modifier = Modifier.fillMaxSize(),
            statusMessage = statusMessage,
            isProcessing = isProcessing,
            errorMessage = errorMessage ?: bindError,
            onCapture = {
                val capture = imageCapture ?: return@CameraGuideOverlay
                val outputFile = viewModel.createCaptureFile()
                val scanBitmap = previewView?.bitmap?.let(::cropPreviewBitmapForScan)
                takePicture(
                    context = context,
                    imageCapture = capture,
                    outputFile = outputFile,
                    onSaved = { savedFile ->
                        if (replaceEntryId != null) {
                            viewModel.replacePhoto(replaceEntryId, savedFile)
                        } else {
                            viewModel.processCapture(savedFile, scanBitmap)
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
            captureEnabled = imageCapture != null && !isProcessing && bindError == null,
        )
    }
}

@Composable
private fun CameraGuideOverlay(
    modifier: Modifier = Modifier,
    statusMessage: String,
    isProcessing: Boolean,
    errorMessage: String?,
    captureEnabled: Boolean,
    onCapture: () -> Unit,
) {
    var showHint by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier.padding(vertical = 18.dp),
    ) {
        val frameHeight = maxHeight * ScanGuideSpec.heightFraction
        val frameTop = maxHeight * ScanGuideSpec.topFraction
        val visibleStatus = when {
            errorMessage != null -> errorMessage
            isProcessing -> statusMessage
            else -> null
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val holeLeft = size.width * ((1f - ScanGuideSpec.widthFraction) / 2f)
            val holeWidth = size.width * ScanGuideSpec.widthFraction
            val holeTop = size.height * ScanGuideSpec.topFraction
            val holeHeight = size.height * ScanGuideSpec.heightFraction
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
                .align(Alignment.TopCenter)
                .padding(top = frameTop)
                .fillMaxWidth()
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
): Pair<ProcessCameraProvider, ImageCapture> {
    val cameraProvider = ProcessCameraProvider.getInstance(context).await()
    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
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
                onError(exception.message ?: "Ошибка камеры.")
            }
        },
    )
}

private fun cropPreviewBitmapForScan(bitmap: Bitmap): Bitmap {
    val cropWidth = (bitmap.width * ScanGuideSpec.widthFraction).roundToInt()
    val cropHeight = (bitmap.height * ScanGuideSpec.heightFraction).roundToInt()
    val left = ((bitmap.width - cropWidth) / 2f).roundToInt()
    val top = (bitmap.height * ScanGuideSpec.topFraction).roundToInt()
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
