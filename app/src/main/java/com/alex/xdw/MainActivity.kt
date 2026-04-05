package com.alex.xdw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.alex.xdw.ui.XdwApp
import com.alex.xdw.ui.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.xdw.ui.theme.XdwTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            repository = (application as XdwApplication).container.repository,
            settingsRepository = (application as XdwApplication).container.settingsRepository,
            appIconManager = (application as XdwApplication).container.appIconManager,
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
    }
}
