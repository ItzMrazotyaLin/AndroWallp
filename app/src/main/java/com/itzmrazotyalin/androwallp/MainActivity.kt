package com.itzmrazotyalin.androwallp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itzmrazotyalin.androwallp.ui.MainViewModel
import com.itzmrazotyalin.androwallp.ui.navigation.AndroWallpApp
import com.itzmrazotyalin.androwallp.ui.theme.AndroWallpTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            AndroWallpTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.useDynamicColor,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AndroWallpApp()
                }
            }
        }
    }
}