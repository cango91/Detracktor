package com.gologlu.detracktor.runtime.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.gologlu.detracktor.R
import com.gologlu.detracktor.runtime.android.presentation.screens.ConfigScreen
import com.gologlu.detracktor.runtime.android.presentation.ui.theme.DetracktorTheme
import com.gologlu.detracktor.runtime.android.service.UiSettingsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConfigActivity : ComponentActivity() {
    
    private lateinit var uiSettingsService: UiSettingsService
    private val _uiSettings = MutableStateFlow(com.gologlu.detracktor.runtime.android.presentation.types.UiSettings())
    private val uiSettings = _uiSettings.asStateFlow()
    
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize UI settings service
        uiSettingsService = CompositionRoot.provideUiSettingsService(this)
        
        // Load current settings
        lifecycleScope.launch {
            _uiSettings.value = uiSettingsService.getCurrentSettings()
        }
        
        setContent {
            val currentSettings by uiSettings.collectAsState()
            
            DetracktorTheme(themeMode = currentSettings.themeMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.config_title)) },
                            navigationIcon = {
                                IconButton(
                                    onClick = { finish() }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.content_description_back)
                                    )
                                }
                            }
                        )
                    }
                ) { padding ->
                    ConfigScreen(
                        uiSettings = currentSettings,
                        onSettingsChange = { newSettings ->
                            lifecycleScope.launch {
                                uiSettingsService.updateSettings(newSettings)
                                _uiSettings.value = newSettings
                            }
                        },
                        onNavigateToRuleEdit = {
                            startActivity(Intent(this@ConfigActivity, RuleEditActivity::class.java))
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}
