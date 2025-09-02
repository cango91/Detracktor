package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gologlu.detracktor.runtime.android.presentation.types.*
import com.gologlu.detracktor.runtime.android.presentation.utils.PreviewDataGenerator
import com.gologlu.detracktor.runtime.android.presentation.utils.PreviewScenario

/**
 * Live preview component for ConfigActivity showing both URL preview modes
 */
@Composable
fun PreviewSection(
    currentMode: UrlPreviewMode,
    modifier: Modifier = Modifier
) {
    var selectedScenario by remember { mutableStateOf(PreviewScenario.WITH_CREDENTIALS) }
    var blurEnabled by remember { mutableStateOf(true) }
    
    val previewData = remember(selectedScenario) {
        PreviewDataGenerator.generateScenarioPreviewData(selectedScenario)
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("preview-section"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with controls
            PreviewHeader(
                selectedScenario = selectedScenario,
                onScenarioChange = { selectedScenario = it },
                blurEnabled = blurEnabled,
                onBlurToggle = { blurEnabled = !blurEnabled }
            )
            
            // Current mode preview
            CurrentModePreview(
                mode = currentMode,
                previewData = previewData,
                blurEnabled = blurEnabled
            )
            
            // Comparison with other mode
            ComparisonPreview(
                currentMode = currentMode,
                previewData = previewData,
                blurEnabled = blurEnabled
            )
        }
    }
}

/**
 * Header section with scenario selection and blur toggle
 */
@Composable
private fun PreviewHeader(
    selectedScenario: PreviewScenario,
    onScenarioChange: (PreviewScenario) -> Unit,
    blurEnabled: Boolean,
    onBlurToggle: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("preview-title")
            )
            
            IconButton(
                onClick = onBlurToggle,
                modifier = Modifier.testTag("blur-toggle")
            ) {
                Icon(
                    imageVector = if (blurEnabled) Icons.Default.Refresh else Icons.Default.Settings,
                    contentDescription = if (blurEnabled) "Show values" else "Hide values",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Scenario selection chips
        Text(
            text = "Sample scenarios:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PreviewScenario.values().forEach { scenario ->
                FilterChip(
                    onClick = { onScenarioChange(scenario) },
                    label = { 
                        Text(
                            text = getScenarioDisplayName(scenario),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    selected = selectedScenario == scenario,
                    modifier = Modifier.testTag("scenario-${scenario.name.lowercase()}")
                )
            }
        }
    }
}

/**
 * Preview of the currently selected mode
 */
@Composable
private fun CurrentModePreview(
    mode: UrlPreviewMode,
    previewData: PreviewData,
    blurEnabled: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Current Mode: ${getModeDisplayName(mode)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.testTag("current-mode-title")
        )
        
        PreviewModeDisplay(
            mode = mode,
            previewData = previewData,
            blurEnabled = blurEnabled,
            isCurrentMode = true
        )
    }
}

/**
 * Comparison preview showing the other mode
 */
@Composable
private fun ComparisonPreview(
    currentMode: UrlPreviewMode,
    previewData: PreviewData,
    blurEnabled: Boolean
) {
    val otherMode = when (currentMode) {
        UrlPreviewMode.CHIPS -> UrlPreviewMode.INLINE_BLUR
        UrlPreviewMode.INLINE_BLUR -> UrlPreviewMode.CHIPS
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Alternative: ${getModeDisplayName(otherMode)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.testTag("alternative-mode-title")
        )
        
        PreviewModeDisplay(
            mode = otherMode,
            previewData = previewData,
            blurEnabled = blurEnabled,
            isCurrentMode = false
        )
    }
}

/**
 * Display a specific preview mode with mock data
 */
@Composable
private fun PreviewModeDisplay(
    mode: UrlPreviewMode,
    previewData: PreviewData,
    blurEnabled: Boolean,
    isCurrentMode: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("preview-mode-${mode.name.lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentMode) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            }
        ),
        border = if (isCurrentMode) {
            androidx.compose.foundation.BorderStroke(
                1.dp, 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mock URL display based on mode
            when (mode) {
                UrlPreviewMode.CHIPS -> {
                    MockChipsPreview(
                        previewData = previewData,
                        blurEnabled = blurEnabled
                    )
                }
                UrlPreviewMode.INLINE_BLUR -> {
                    MockInlineBlurPreview(
                        previewData = previewData,
                        blurEnabled = blurEnabled
                    )
                }
            }
            
            // Show warnings and rules if present
            if (previewData.warningData.hasWarnings) {
                MockWarningDisplay(previewData.warningData)
            }
            
            if (previewData.matchedRules.isNotEmpty()) {
                MockRulesDisplay(previewData.matchedRules)
            }
        }
    }
}

/**
 * Mock chips-style preview
 */
@Composable
private fun MockChipsPreview(
    previewData: PreviewData,
    blurEnabled: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (blurEnabled) "https://••••••@example.com/path" else previewData.originalUrl,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("mock-chips-url")
        )
        
        // Mock parameter chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("token", "normal_param", "tracker_param").forEach { param ->
                val isRemoved = param in listOf("token", "tracker_param")
                AssistChip(
                    onClick = { },
                    label = { 
                        Text(
                            text = if (blurEnabled && !isRemoved) "••••••" else param,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isRemoved) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    modifier = Modifier.testTag("mock-chip-$param")
                )
            }
        }
    }
}

/**
 * Mock inline blur preview
 */
@Composable
private fun MockInlineBlurPreview(
    previewData: PreviewData,
    blurEnabled: Boolean
) {
    val displayUrl = if (blurEnabled) {
        "https://••••••@example.com/path?token=abc&normal_param=••••••&tracker_param=5678"
    } else {
        previewData.originalUrl
    }
    
    Text(
        text = displayUrl,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.testTag("mock-inline-url")
    )
}

/**
 * Mock warning display
 */
@Composable
private fun MockWarningDisplay(warningData: WarningDisplayData) {
    Row(
        modifier = Modifier.testTag("mock-warning"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⚠️",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Embedded credentials detected",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Mock rules display
 */
@Composable
private fun MockRulesDisplay(rules: List<RuleMatchSummary>) {
    Text(
        text = "✓ ${rules.size} rule${if (rules.size != 1) "s" else ""} matched",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag("mock-rules")
    )
}

/**
 * Get display name for preview scenario
 */
private fun getScenarioDisplayName(scenario: PreviewScenario): String {
    return when (scenario) {
        PreviewScenario.NO_MATCHES -> "Clean"
        PreviewScenario.WITH_TRACKING -> "Tracking"
        PreviewScenario.WITH_CREDENTIALS -> "Credentials"
        PreviewScenario.MULTIPLE_RULES -> "Multiple"
    }
}

/**
 * Get display name for URL preview mode
 */
private fun getModeDisplayName(mode: UrlPreviewMode): String {
    return when (mode) {
        UrlPreviewMode.CHIPS -> "Chips"
        UrlPreviewMode.INLINE_BLUR -> "Inline Blur"
    }
}
