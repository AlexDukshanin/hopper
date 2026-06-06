package com.alex.hopper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.hopper.exchange.CollectionExchangeManager
import com.alex.hopper.ui.MainViewModel
import com.alex.hopper.ui.XdwApp
import com.alex.hopper.ui.theme.XdwTheme

class MainActivity : ComponentActivity() {
    private var pendingImportUri: Uri? = null
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            repository = (application as XdwApplication).container.repository,
            settingsRepository = (application as XdwApplication).container.settingsRepository,
            appIconManager = (application as XdwApplication).container.appIconManager,
            collectionExchangeManager = (application as XdwApplication).container.collectionExchangeManager,
        )
    }
    private val readExternalStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        val uri = pendingImportUri
        pendingImportUri = null
        if (isGranted && uri != null) {
            viewModel.importCollectionFromUri(uri)
        } else if (uri != null) {
            viewModel.notifyImportAccessDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings = viewModel.settings.collectAsStateWithLifecycle()

            XdwTheme(themeMode = settings.value.themeMode) {
                XdwApp(
                    viewModel = viewModel,
                    settings = settings.value,
                )
            }
        }

        if (savedInstanceState == null) {
            handleImportIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleImportIntent(intent)
    }

    private fun handleImportIntent(intent: Intent?) {
        val uri = intent.extractImportUri() ?: return
        if (uri.requiresLegacyExternalStoragePermission() && !hasLegacyExternalStoragePermission()) {
            pendingImportUri = uri
            readExternalStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            viewModel.importCollectionFromUri(uri)
        }
    }

    private fun Intent?.extractImportUri(): Uri? {
        if (this == null) return null
        val uri = when (action) {
            Intent.ACTION_VIEW -> data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        } ?: return null

        val mimeType = type ?: contentResolver.getType(uri)
        val hasSupportedMimeType = mimeType in CollectionExchangeManager.SUPPORTED_IMPORT_MIME_TYPES
        val hasSupportedExtension = uri.getImportFileName()
            ?.endsWith(CollectionExchangeManager.FILE_EXTENSION, ignoreCase = true)
            ?: false

        return uri.takeIf { hasSupportedMimeType || hasSupportedExtension }
    }

    private fun Uri.getImportFileName(): String? {
        if (scheme == "content") {
            contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            return cursor.getString(index)
                        }
                    }
                }
        }
        return lastPathSegment?.substringAfterLast('/')
    }

    private fun Uri.requiresLegacyExternalStoragePermission(): Boolean {
        return scheme == "file" && Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2
    }

    private fun hasLegacyExternalStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2 ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
    }
}
