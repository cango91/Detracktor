package com.gologlu.detracktor.runtime.android.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gologlu.detracktor.runtime.android.presentation.components.PreviewSection
import com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction
import com.gologlu.detracktor.runtime.android.presentation.types.ThemeMode
import com.gologlu.detracktor.runtime.android.presentation.types.UiSettings
import com.gologlu.detracktor.runtime.android.presentation.types.UrlPreviewMode

/**
 * Main configuration screen with all UI settings sections
 */
@Composable
fun ConfigScreen(
    uiSettings: UiSettings,
    onSettingsChange: (UiSettings) -> Unit,
    onNavigateToRuleEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("config-screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("config-title")
        )
        
        // Theme section
        ThemeSection(
            currentTheme = uiSettings.themeMode,
            onThemeChange = { theme ->
                onSettingsChange(uiSettings.copy(themeMode = theme))
            }
        )
        
        HorizontalDivider()
        
        // URL Preview section
        UrlPreviewSection(
            currentMode = uiSettings.urlPreviewMode,
            onModeChange = { mode ->
                onSettingsChange(uiSettings.copy(urlPreviewMode = mode))
            }
        )
        
        HorizontalDivider()
        
        // After cleaning section
        AfterCleaningSection(
            currentAction = uiSettings.afterCleaningAction,
            onActionChange = { action ->
                onSettingsChange(uiSettings.copy(afterCleaningAction = action))
            }
        )
        
        HorizontalDivider()
        
        // Rule management section
        RuleManagementSection(
            onNavigateToRuleEdit = onNavigateToRuleEdit
        )
    }
}

/**
 * Theme selection section
 */
@Composable
private fun ThemeSection(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    SettingsSection(
        title = "Theme",
        description = "Choose your preferred app theme"
    ) {
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.values().forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentTheme == theme,
                            onClick = { onThemeChange(theme) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp)
                        .testTag("theme-option-${theme.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = currentTheme == theme,
                        onClick = null,
                        modifier = Modifier.testTag("theme-radio-${theme.name.lowercase()}")
                    )
                    Column {
                        Text(
                            text = when (theme) {
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                                ThemeMode.SYSTEM -> "Follow System"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag("theme-title-${theme.name.lowercase()}")
                        )
                        Text(
                            text = when (theme) {
                                ThemeMode.LIGHT -> "Always use light theme"
                                ThemeMode.DARK -> "Always use dark theme"
                                ThemeMode.SYSTEM -> "Match system theme setting"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.testTag("theme-desc-${theme.name.lowercase()}")
                        )
                    }
                }
            }
        }
    }
}

/**
 * URL preview mode selection section with horizontal layout and live preview
 */
@Composable
private fun UrlPreviewSection(
    currentMode: UrlPreviewMode,
    onModeChange: (UrlPreviewMode) -> Unit
) {
    SettingsSection(
        title = "URL Preview Style",
        description = "Choose how URLs and parameters are displayed"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Horizontal radio button layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .testTag("preview-mode-selector"),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UrlPreviewMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = currentMode == mode,
                                onClick = { onModeChange(mode) },
                                role = Role.RadioButton
                            )
                            .padding(8.dp)
                            .testTag("preview-option-${mode.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = null,
                            modifier = Modifier.testTag("preview-radio-${mode.name.lowercase()}")
                        )
                        Column {
                            Text(
                                text = when (mode) {
                                    UrlPreviewMode.CHIPS -> "Chips"
                                    UrlPreviewMode.INLINE_BLUR -> "Inline Blur"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("preview-title-${mode.name.lowercase()}")
                            )
                            Text(
                                text = when (mode) {
                                    UrlPreviewMode.CHIPS -> "Chip-based"
                                    UrlPreviewMode.INLINE_BLUR -> "Inline text"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.testTag("preview-desc-${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }
            
            // Live preview section
            PreviewSection(
                currentMode = currentMode,
                modifier = Modifier.testTag("live-preview-section")
            )
        }
    }
}

/**
 * After cleaning action selection section
 */
@Composable
private fun AfterCleaningSection(
    currentAction: AfterCleaningAction,
    onActionChange: (AfterCleaningAction) -> Unit
) {
    SettingsSection(
        title = "After Cleaning URLs",
        description = "What to do with cleaned URLs"
    ) {
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AfterCleaningAction.values().forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentAction == action,
                            onClick = { onActionChange(action) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp)
                        .testTag("action-option-${action.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = currentAction == action,
                        onClick = null,
                        modifier = Modifier.testTag("action-radio-${action.name.lowercase()}")
                    )
                    Column {
                        Text(
                            text = when (action) {
                                AfterCleaningAction.ALWAYS_SHARE -> "Always Share"
                                AfterCleaningAction.ALWAYS_COPY -> "Always Copy"
                                AfterCleaningAction.ASK -> "Ask Each Time"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.testTag("action-title-${action.name.lowercase()}")
                        )
                        Text(
                            text = when (action) {
                                AfterCleaningAction.ALWAYS_SHARE -> "Automatically share cleaned URLs"
                                AfterCleaningAction.ALWAYS_COPY -> "Automatically copy to clipboard"
                                AfterCleaningAction.ASK -> "Show dialog to choose action"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.testTag("action-desc-${action.name.lowercase()}")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Rule management section
 */
@Composable
private fun RuleManagementSection(
    onNavigateToRuleEdit: () -> Unit
) {
    SettingsSection(
        title = "Rule Management",
        description = "Configure URL cleaning rules and patterns"
    ) {
        Button(
            onClick = onNavigateToRuleEdit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rule-edit-button")
        ) {
            Text(
                text = "Edit Rules",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Reusable settings section container
 */
@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings-section-${title.lowercase().replace(" ", "-")}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section header
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("section-title")
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("section-description")
                )
            }
            
            // Section content
            content()
        }
    }
}
