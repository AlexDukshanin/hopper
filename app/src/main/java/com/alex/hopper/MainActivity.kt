package com.alex.hopper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.hopper.exchange.CollectionExchangeManager
import com.alex.hopper.ui.MainViewModel
import com.alex.hopper.ui.XdwApp
import com.alex.hopper.ui.theme.XdwTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            repository = (application as XdwApplication).container.repository,
            settingsRepository = (application as XdwApplication).container.settingsRepository,
            appIconManager = (application as XdwApplication).container.appIconManager,
            collectionExchangeManager = (application as XdwApplication).container.collectionExchangeManager,
        )
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
        viewModel.importCollectionFromUri(uri)
    }

    private fun Intent?.extractImportUri(): Uri? {
        if (this == null) return null
        val mimeType = type ?: return null
        if (mimeType != CollectionExchangeManager.MIME_TYPE) return null

        return when (action) {
            Intent.ACTION_VIEW -> data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
    }
}
