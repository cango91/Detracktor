package com.gologlu.detracktor.runtime.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.gologlu.detracktor.runtime.android.presentation.ui.theme.DetracktorTheme
import com.gologlu.detracktor.runtime.android.presentation.screens.RuleEditScreen
import com.gologlu.detracktor.runtime.android.service.UiSettingsService

/**
 * Activity for structured rule editing interface.
 * Provides a user-friendly way to create and edit URL cleaning rules
 * without requiring direct JSON manipulation.
 */
class RuleEditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val uiSettingsService = CompositionRoot.provideUiSettingsService(this@RuleEditActivity)
            val uiSettings = uiSettingsService.getCurrentSettings()
            
            DetracktorTheme(themeMode = uiSettings.themeMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    RuleEditScreen(
                        modifier = Modifier.padding(padding),
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}
