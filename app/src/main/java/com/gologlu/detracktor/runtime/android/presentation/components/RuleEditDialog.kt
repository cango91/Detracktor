package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.gologlu.detracktor.runtime.android.presentation.ui.theme.DetracktorTheme
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
                // Domains Section
                DomainsSection(
                    domainsInput = formData.domainsInput,
                    onDomainsInputChange = { formData = formData.copy(domainsInput = it) }
                )
                
                // Subdomain Mode Section (hidden when catch-all domain "*")
                if (formData.domainsInput.trim() != "*") {
                    SubdomainModeSection(
                        subdomainMode = formData.subdomainMode,
                        subdomainsInput = formData.subdomainsInput,
                        onSubdomainModeChange = { formData = formData.copy(subdomainMode = it) },
                        onSubdomainsInputChange = { formData = formData.copy(subdomainsInput = it) }
                    )
                }
                
                // Remove Patterns Section
                RemovePatternsSection(
                    removePatternsInput = formData.removePatternsInput,
                    onRemovePatternsInputChange = { formData = formData.copy(removePatternsInput = it) }
                )
                
                // Warning Settings Section
                WarningSettingsSection(
                    warnOnCredentials = formData.warnOnCredentials,
                    sensitiveParamsInput = formData.sensitiveParamsInput,
                    mergeMode = formData.sensitiveMergeMode,
                    onWarnOnCredentialsChange = { formData = formData.copy(warnOnCredentials = it) },
                    onSensitiveParamsInputChange = { formData = formData.copy(sensitiveParamsInput = it) },
                    onMergeModeChange = { formData = formData.copy(sensitiveMergeMode = it) }
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
private fun DomainsSection(
    domainsInput: String,
    onDomainsInputChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Domains",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = DetracktorTheme.colors.onSurface
        )
        
        OutlinedTextField(
            value = domainsInput,
            onValueChange = onDomainsInputChange,
            label = { Text("Domains (comma-separated)") },
            placeholder = { Text("example.com, test.org") },
            supportingText = { 
                Text(
                    "Enter one or more domains separated by commas",
                    style = MaterialTheme.typography.bodySmall,
                    color = DetracktorTheme.colors.onSurfaceVariant
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("domains-input"),
            singleLine = false,
            maxLines = 3
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubdomainModeSection(
    subdomainMode: SubdomainMode,
    subdomainsInput: String,
    onSubdomainModeChange: (SubdomainMode) -> Unit,
    onSubdomainsInputChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Subdomain Mode",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = DetracktorTheme.colors.onSurface
        )
        
        // Dropdown for subdomain mode
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.testTag("subdomain-mode-dropdown")
        ) {
            OutlinedTextField(
                value = when (subdomainMode) {
                    SubdomainMode.NONE -> "None"
                    SubdomainMode.ANY -> "Any"
                    SubdomainMode.SPECIFIC_LIST -> "Specific List"
                },
                onValueChange = { },
                readOnly = true,
                label = { Text("Subdomain Mode") },
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SubdomainMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = when (mode) {
                                        SubdomainMode.NONE -> "None"
                                        SubdomainMode.ANY -> "Any"
                                        SubdomainMode.SPECIFIC_LIST -> "Specific List"
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = when (mode) {
                                        SubdomainMode.NONE -> "Exact domain match only"
                                        SubdomainMode.ANY -> "Include all subdomains (*.domain.com)"
                                        SubdomainMode.SPECIFIC_LIST -> "Only specified subdomains"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DetracktorTheme.colors.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onSubdomainModeChange(mode)
                            expanded = false
                        },
                        modifier = Modifier.testTag("subdomain-mode-${mode.name.lowercase()}")
                    )
                }
            }
        }
        
        // Specific subdomains input (conditional)
        if (subdomainMode == SubdomainMode.SPECIFIC_LIST) {
            OutlinedTextField(
                value = subdomainsInput,
                onValueChange = onSubdomainsInputChange,
                label = { Text("Subdomains (comma-separated)") },
                placeholder = { Text("www, api, cdn") },
                supportingText = { 
                    Text(
                        "Enter subdomain names without dots (e.g., www, api, cdn)",
                        style = MaterialTheme.typography.bodySmall,
                        color = DetracktorTheme.colors.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subdomains-input"),
                singleLine = false,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun RemovePatternsSection(
    removePatternsInput: String,
    onRemovePatternsInputChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Remove Patterns",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = DetracktorTheme.colors.onSurface
        )
        
        OutlinedTextField(
            value = removePatternsInput,
            onValueChange = onRemovePatternsInputChange,
            label = { Text("Patterns (comma-separated)") },
            placeholder = { Text("utm_*, gclid, fbclid") },
            supportingText = { 
                Text(
                    "Glob patterns for parameters to remove (e.g., utm_*, gclid)",
                    style = MaterialTheme.typography.bodySmall,
                    color = DetracktorTheme.colors.onSurfaceVariant
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("remove-patterns-input"),
            singleLine = false,
            maxLines = 3
        )
        
        // Hint text for common patterns
        Text(
            text = "Common patterns: ${RuleFormValidator.COMMON_TRACKING_PATTERNS.take(6).joinToString(", ")}",
            style = MaterialTheme.typography.bodySmall,
            color = DetracktorTheme.colors.onSurfaceVariant
        )
    }
}


@Composable
private fun WarningSettingsSection(
    warnOnCredentials: Boolean,
    sensitiveParamsInput: String,
    mergeMode: SensitiveMergeModeUi,
    onWarnOnCredentialsChange: (Boolean) -> Unit,
    onSensitiveParamsInputChange: (String) -> Unit,
    onMergeModeChange: (SensitiveMergeModeUi) -> Unit
) {
    var showWarnings by remember { mutableStateOf(warnOnCredentials || sensitiveParamsInput.isNotBlank()) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Warning Settings Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("warning-settings-toggle")
        ) {
            Switch(
                checked = showWarnings,
                onCheckedChange = { 
                    showWarnings = it
                    if (!it) {
                        onWarnOnCredentialsChange(false)
                        onSensitiveParamsInputChange("")
                    } else {
                        // Default "warn on embed creds" to true when warnings are enabled
                        onWarnOnCredentialsChange(true)
                    }
                }
            )
            Text(
                text = "Warning Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = DetracktorTheme.colors.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        if (showWarnings) {
            // Warn on credentials checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .testTag("warn-credentials-row")
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
            
            // Sensitive parameters input
            Column(
                modifier = Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = sensitiveParamsInput,
                    onValueChange = onSensitiveParamsInputChange,
                    label = { Text("Sensitive Parameters (comma-separated)") },
                    placeholder = { Text("token, key, password") },
                    supportingText = { 
                        Text(
                            "Parameters that should trigger warnings when present",
                            style = MaterialTheme.typography.bodySmall,
                            color = DetracktorTheme.colors.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sensitive-params-input"),
                    singleLine = false,
                    maxLines = 2
                )
                
                // Hint text for sensitive parameters
                Text(
                    text = "Common sensitive parameters: ${RuleFormValidator.COMMON_SENSITIVE_PARAMS.take(6).joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DetracktorTheme.colors.onSurfaceVariant
                )

                // Merge mode selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("sensitive-merge-row")
                ) {
                    Text(
                        text = "Merge behavior:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FilterChip(
                        selected = mergeMode == SensitiveMergeModeUi.UNION,
                        onClick = { onMergeModeChange(SensitiveMergeModeUi.UNION) },
                        label = { Text("Union") }
                    )
                    FilterChip(
                        selected = mergeMode == SensitiveMergeModeUi.REPLACE,
                        onClick = { onMergeModeChange(SensitiveMergeModeUi.REPLACE) },
                        label = { Text("Replace") }
                    )
                }
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
