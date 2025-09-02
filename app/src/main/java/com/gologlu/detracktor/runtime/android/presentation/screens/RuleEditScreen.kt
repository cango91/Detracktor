package com.gologlu.detracktor.runtime.android.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gologlu.detracktor.application.error.AppResult
import com.gologlu.detracktor.application.service.SettingsService
import com.gologlu.detracktor.application.types.UrlRule
import com.gologlu.detracktor.runtime.android.CompositionRoot
import com.gologlu.detracktor.runtime.android.presentation.components.RuleEditDialog
import com.gologlu.detracktor.runtime.android.presentation.components.RuleListItem
import kotlinx.coroutines.launch

/**
 * Screen for rule list management interface matching legacy ConfigActivity design.
 * Displays existing rules in a list format with edit/delete actions and provides
 * a structured rule creation dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsService = remember { CompositionRoot.provideSettingsService(context) }
    val scope = rememberCoroutineScope()
    
    var rules by remember { mutableStateOf<List<UrlRule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<Pair<Int, UrlRule>?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Load rules on startup
    LaunchedEffect(Unit) {
        scope.launch {
            when (val result = settingsService.loadEffective()) {
                is AppResult.Success -> {
                    rules = result.value.sites
                    isLoading = false
                }
                is AppResult.Failure -> {
                    errorMessage = "Failed to load rules: ${result.error}"
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("rule-edit-screen"),
        topBar = {
            TopAppBar(
                title = { Text("Manage Rules") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back-button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("reset-button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset to defaults")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add-rule-fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("loading-indicator")
                    )
                }
                errorMessage != null -> {
                    ErrorMessage(
                        message = errorMessage!!,
                        onRetry = {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                when (val result = settingsService.loadEffective()) {
                                    is AppResult.Success -> {
                                        rules = result.value.sites
                                        isLoading = false
                                    }
                                    is AppResult.Failure -> {
                                        errorMessage = "Failed to load rules: ${result.error}"
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    RuleManagementContent(
                        rules = rules,
                        onRuleEdit = { index, rule -> editingRule = index to rule },
                        onRuleDelete = { index ->
                            scope.launch {
                                val newRules = rules.toMutableList().apply { removeAt(index) }
                                saveRules(settingsService, newRules) { updatedRules ->
                                    rules = updatedRules
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Add Rule Dialog
    if (showAddDialog) {
        RuleEditDialog(
            rule = null,
            onSave = { newRule ->
                scope.launch {
                    val newRules = rules + newRule
                    saveRules(settingsService, newRules) { updatedRules ->
                        rules = updatedRules
                        showAddDialog = false
                    }
                }
            },
            onCancel = { showAddDialog = false }
        )
    }

    // Edit Rule Dialog
    editingRule?.let { (index, rule) ->
        RuleEditDialog(
            rule = rule,
            onSave = { updatedRule ->
                scope.launch {
                    val newRules = rules.toMutableList().apply { set(index, updatedRule) }
                    saveRules(settingsService, newRules) { updatedRules ->
                        rules = updatedRules
                        editingRule = null
                    }
                }
            },
            onCancel = { editingRule = null }
        )
    }

    // Reset to Defaults Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset to Defaults") },
            text = { 
                Text("This will replace all current rules with the default rule set. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            // For now, just reload the effective settings as a reset
                            when (val result = settingsService.loadEffective()) {
                                is AppResult.Success -> {
                                    rules = result.value.sites
                                    showResetDialog = false
                                }
                                is AppResult.Failure -> {
                                    errorMessage = "Failed to reset: ${result.error}"
                                    showResetDialog = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm-reset")
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    modifier = Modifier.testTag("cancel-reset")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("reset-dialog")
        )
    }
}

/**
 * Main content showing rule list or empty state
 */
@Composable
private fun RuleManagementContent(
    rules: List<UrlRule>,
    onRuleEdit: (Int, UrlRule) -> Unit,
    onRuleDelete: (Int) -> Unit
) {
    if (rules.isEmpty()) {
        EmptyRulesState(
            modifier = Modifier
                .fillMaxSize()
                .testTag("empty-rules-state")
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("rules-list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with instructions
            item {
                InstructionalHeader()
            }
            
            // Rule list items
            itemsIndexed(rules) { index, rule ->
                RuleListItem(
                    rule = rule,
                    index = index,
                    onEdit = { onRuleEdit(index, rule) },
                    onDelete = { onRuleDelete(index) }
                )
            }
            
            // Bottom spacing for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * Instructional header for rule management
 */
@Composable
private fun InstructionalHeader() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("instructional-header"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "URL Cleaning Rules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Rules are applied in order to remove tracking parameters from URLs. " +
                      "Each rule can target specific domains or apply globally.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            Text(
                text = "• Tap the + button to add a new rule\n" +
                      "• Tap edit to modify an existing rule\n" +
                      "• Use the refresh button to reset to defaults",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Empty state when no rules are configured
 */
@Composable
private fun EmptyRulesState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Rules Configured",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("empty-title")
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Add your first rule to start cleaning URLs automatically. " +
                  "Rules help remove tracking parameters and protect your privacy.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.testTag("empty-description")
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Tap the + button to get started",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("empty-hint")
        )
    }
}

/**
 * Error message component with retry option
 */
@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("error-title")
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.testTag("error-message")
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier.testTag("retry-button")
        ) {
            Text("Retry")
        }
    }
}

/**
 * Helper function to save rules using the settings service
 */
private suspend fun saveRules(
    settingsService: SettingsService,
    rules: List<UrlRule>,
    onSuccess: (List<UrlRule>) -> Unit
) {
    // This is a simplified implementation - in a real app you'd want proper error handling
    // and would need to create a complete AppSettings object
    when (val currentResult = settingsService.loadEffective()) {
        is AppResult.Success -> {
            val updatedSettings = currentResult.value.copy(sites = rules)
            when (settingsService.saveUser(updatedSettings)) {
                is AppResult.Success -> onSuccess(rules)
                is AppResult.Failure -> {
                    // Handle error - could show a snackbar or error dialog
                }
            }
        }
        is AppResult.Failure -> {
            // Handle error loading current settings
        }
    }
}
