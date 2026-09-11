package com.a8000053398.printly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.a8000053398.printly.model.AppearanceMode
import com.a8000053398.printly.service.persistence.AppSettingsStore
import com.a8000053398.printly.ui.RootTabsScreen
import com.a8000053398.printly.ui.theme.AppTheme
import com.a8000053398.printly.ui.theme.PrintlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearance by AppSettingsStore.shared.appearanceMode.collectAsState()
            val theme = when (appearance) {
                AppearanceMode.SYSTEM -> AppTheme.SYSTEM
                AppearanceMode.LIGHT -> AppTheme.LIGHT
                AppearanceMode.DARK -> AppTheme.DARK
            }
            PrintlyTheme(theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootTabsScreen(deepLinkIntent = intent)
                }
            }
        }
    }
}
