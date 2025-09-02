package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gologlu.detracktor.application.types.UrlRule
import com.gologlu.detracktor.runtime.android.presentation.types.*
import com.gologlu.detracktor.runtime.android.presentation.utils.RuleFormValidator

/**
 * Structured rule creation/editing dialog with dynamic form fields
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditDialog(
    rule: UrlRule? = null,
    onSave: (UrlRule) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val validator = remember { RuleFormValidator() }
    var formData by remember { 
        mutableStateOf(rule?.toFormData() ?: RuleEditFormData()) 
    }
    var validationResult by remember { 
        mutableStateOf(validator.validateComplete(formData)) 
    }
    
    // Update validation when form data changes
    LaunchedEffect(formData) {
        validationResult = validator.validateComplete(formData)
    }
    
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = modifier.testTag("rule-edit-dialog"),
        title = {
            Text(
                text = if (rule == null) "Add New Rule" else "Edit Rule",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Host Pattern Section
                HostPatternSection(
                    hostPattern = formData.hostPattern,
                    subdomainMode = formData.subdomainMode,
                    onHostPatternChange = { formData = formData.copy(hostPattern = it) },
                    onSubdomainModeChange = { formData = formData.copy(subdomainMode = it) }
                )
                
                // Remove Patterns Section
                RemovePatternsSection(
                    patterns = formData.removePatterns,
                    onPatternsChange = { formData = formData.copy(removePatterns = it) }
                )
                
                // Schemes Section
                SchemesSection(
                    schemes = formData.schemes,
                    onSchemesChange = { formData = formData.copy(schemes = it) }
                )
                
                // Warning Settings Section
                WarningSettingsSection(
                    warnOnCredentials = formData.warnOnCredentials,
                    sensitiveParams = formData.sensitiveParams,
                    onWarnOnCredentialsChange = { formData = formData.copy(warnOnCredentials = it) },
                    onSensitiveParamsChange = { formData = formData.copy(sensitiveParams = it) }
                )
                
                // Validation Results
                if (validationResult.errors.isNotEmpty() || validationResult.warnings.isNotEmpty()) {
                    ValidationResultsSection(validationResult = validationResult)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validationResult.isValid) {
                        onSave(formData.toUrlRule())
                    }
                },
                enabled = validationResult.isValid,
                modifier = Modifier.testTag("save-rule-button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag("cancel-rule-button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun HostPatternSection(
    hostPattern: String,
    subdomainMode: SubdomainMode,
    onHostPatternChange: (String) -> Unit,
    onSubdomainModeChange: (SubdomainMode) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Host Pattern",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        // Subdomain Mode Selection
        Text(
            text = "Subdomain Mode",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Column {
            SubdomainMode.values().forEach { mode ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("subdomain-mode-${mode.name.lowercase()}")
                ) {
                    RadioButton(
                        selected = subdomainMode == mode,
                        onClick = { onSubdomainModeChange(mode) }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = when (mode) {
                                SubdomainMode.EXACT -> "Exact match"
                                SubdomainMode.WILDCARD -> "Include subdomains"
                                SubdomainMode.ANY -> "All domains"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = when (mode) {
                                SubdomainMode.EXACT -> "example.com only"
                                SubdomainMode.WILDCARD -> "*.example.com"
                                SubdomainMode.ANY -> "Global rule"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        
        // Host Pattern Input (conditional)
        if (subdomainMode != SubdomainMode.ANY) {
            OutlinedTextField(
                value = hostPattern,
                onValueChange = onHostPatternChange,
                label = { Text("Domain") },
                placeholder = { Text("example.com") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("host-pattern-input"),
                singleLine = true
            )
        }
    }
}

@Composable
private fun RemovePatternsSection(
    patterns: List<String>,
    onPatternsChange: (List<String>) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Remove Patterns",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            IconButton(
                onClick = { onPatternsChange(patterns + "") },
                modifier = Modifier.testTag("add-pattern-button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add pattern")
            }
        }
        
        Text(
            text = "Glob patterns for parameters to remove (e.g., utm_*, gclid)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        patterns.forEachIndexed { index, pattern ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { newValue ->
                        val newPatterns = patterns.toMutableList()
                        newPatterns[index] = newValue
                        onPatternsChange(newPatterns)
                    },
                    label = { Text("Pattern ${index + 1}") },
                    placeholder = { Text("utm_*") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("remove-pattern-$index"),
                    singleLine = true
                )
                
                if (patterns.size > 1) {
                    IconButton(
                        onClick = {
                            val newPatterns = patterns.toMutableList()
                            newPatterns.removeAt(index)
                            onPatternsChange(newPatterns)
                        },
                        modifier = Modifier.testTag("delete-pattern-$index")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete pattern")
                    }
                }
            }
        }
        
        // Quick selection chips
        Text(
            text = "Common patterns:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RuleFormValidator.COMMON_TRACKING_PATTERNS.take(3).forEach { commonPattern ->
                FilterChip(
                    onClick = {
                        if (!patterns.contains(commonPattern)) {
                            val newPatterns = patterns.toMutableList()
                            val firstEmpty = newPatterns.indexOfFirst { it.isBlank() }
                            if (firstEmpty >= 0) {
                                newPatterns[firstEmpty] = commonPattern
                            } else {
                                newPatterns.add(commonPattern)
                            }
                            onPatternsChange(newPatterns)
                        }
                    },
                    label = { Text(commonPattern, style = MaterialTheme.typography.labelSmall) },
                    selected = patterns.contains(commonPattern),
                    modifier = Modifier.testTag("common-pattern-$commonPattern")
                )
            }
        }
    }
}

@Composable
private fun SchemesSection(
    schemes: List<String>,
    onSchemesChange: (List<String>) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "URL Schemes",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("http", "https").forEach { scheme ->
                FilterChip(
                    onClick = {
                        val newSchemes = if (schemes.contains(scheme)) {
                            schemes - scheme
                        } else {
                            schemes + scheme
                        }
                        onSchemesChange(newSchemes)
                    },
                    label = { Text(scheme.uppercase()) },
                    selected = schemes.contains(scheme),
                    modifier = Modifier.testTag("scheme-$scheme")
                )
            }
        }
    }
}

@Composable
private fun WarningSettingsSection(
    warnOnCredentials: Boolean,
    sensitiveParams: List<String>,
    onWarnOnCredentialsChange: (Boolean) -> Unit,
    onSensitiveParamsChange: (List<String>) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Warning Settings",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        // Warn on credentials checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("warn-credentials-row")
        ) {
            Checkbox(
                checked = warnOnCredentials,
                onCheckedChange = onWarnOnCredentialsChange
            )
            Text(
                text = "Warn on embedded credentials",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // Sensitive parameters
        Text(
            text = "Sensitive Parameters (optional)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        // Quick selection for sensitive params
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RuleFormValidator.COMMON_SENSITIVE_PARAMS.take(3).forEach { param ->
                FilterChip(
                    onClick = {
                        val newParams = if (sensitiveParams.contains(param)) {
                            sensitiveParams - param
                        } else {
                            sensitiveParams + param
                        }
                        onSensitiveParamsChange(newParams)
                    },
                    label = { Text(param, style = MaterialTheme.typography.labelSmall) },
                    selected = sensitiveParams.contains(param),
                    modifier = Modifier.testTag("sensitive-param-$param")
                )
            }
        }
    }
}

@Composable
private fun ValidationResultsSection(
    validationResult: RuleValidationResult
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (validationResult.errors.isNotEmpty()) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            }
        ),
        modifier = Modifier.testTag("validation-results")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (validationResult.errors.isNotEmpty()) {
                Text(
                    text = "Errors:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
                validationResult.errors.forEach { error ->
                    Text(
                        text = "• $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            if (validationResult.warnings.isNotEmpty()) {
                if (validationResult.errors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = "Warnings:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                validationResult.warnings.forEach { warning ->
                    Text(
                        text = "• $warning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
