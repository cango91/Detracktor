package com.gologlu.detracktor.runtime.android

import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.ClipDescription
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.gologlu.detracktor.R
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import android.os.FileObserver
import java.io.File
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gologlu.detracktor.application.error.AppResult
import com.gologlu.detracktor.application.service.SettingsService
import com.gologlu.detracktor.application.service.match.Evaluation
import com.gologlu.detracktor.application.service.match.RuleEngine
import com.gologlu.detracktor.application.service.match.TokenEffect
import com.gologlu.detracktor.application.service.net.HostCanonicalizer
import com.gologlu.detracktor.domain.error.DomainResult
import com.gologlu.detracktor.domain.model.MaybeUrl
import com.gologlu.detracktor.domain.model.QueryPairs
import com.gologlu.detracktor.domain.model.QueryToken
import com.gologlu.detracktor.domain.model.UrlParts
import com.gologlu.detracktor.domain.model.Url
import com.gologlu.detracktor.domain.service.UrlParser
import com.gologlu.detracktor.runtime.android.presentation.ui.theme.DetracktorTheme
import com.gologlu.detracktor.runtime.android.presentation.components.UrlPreviewInlineBlur
import com.gologlu.detracktor.runtime.android.presentation.components.WarningPanel
import com.gologlu.detracktor.runtime.android.presentation.components.RulesSummary
import com.gologlu.detracktor.runtime.android.presentation.types.UrlPreviewMode
import com.gologlu.detracktor.runtime.android.presentation.types.WarningDisplayData
import com.gologlu.detracktor.runtime.android.presentation.types.RuleMatchDisplayData
import com.gologlu.detracktor.runtime.android.presentation.types.RuleMatchSummary
import com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction
import com.gologlu.detracktor.runtime.android.presentation.types.DialogState
import com.gologlu.detracktor.runtime.android.presentation.types.DialogType
import com.gologlu.detracktor.runtime.android.service.UiSettingsService
import com.gologlu.detracktor.runtime.android.presentation.components.CleaningDialog
import com.gologlu.detracktor.runtime.android.presentation.components.ShareWarningDialog
import com.gologlu.detracktor.runtime.android.presentation.components.InstructionalPanel
import com.gologlu.detracktor.runtime.android.presentation.utils.BlurStateCalculator
import com.gologlu.detracktor.runtime.android.presentation.utils.BlurState
import com.gologlu.detracktor.runtime.android.presentation.types.ClipboardState
import com.gologlu.detracktor.runtime.android.presentation.types.InstructionalContent
import com.gologlu.detracktor.runtime.android.presentation.types.AppStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.jvm.java

class MainActivity : ComponentActivity() {
    private var lastCompiledSettingsHash: Int? = null
    private val rulesVersionFlow: MutableStateFlow<Int> = MutableStateFlow(0)
    private var settingsObserver: FileObserver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsService: SettingsService = CompositionRoot.provideSettingsService(this)
        val ruleEngine: RuleEngine = CompositionRoot.provideRuleEngine()
        val hostCanonicalizer: HostCanonicalizer = CompositionRoot.provideHostCanonicalizer()
        val urlParser: UrlParser = CompositionRoot.provideUrlParser()

        // Watch user settings file for changes and reload immediately
        val userSettingsFile = File(filesDir, "user_settings.json")
        settingsObserver = object : FileObserver(userSettingsFile.parentFile?.path ?: filesDir.path, CLOSE_WRITE or MOVED_TO or CREATE or DELETE) {
            override fun onEvent(event: Int, path: String?) {
                if (path != null && path.endsWith("user_settings.json")) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        when (val res = settingsService.loadEffective()) {
                            is AppResult.Success -> {
                                with(hostCanonicalizer) { ruleEngine.load(res.value) }
                                lastCompiledSettingsHash = res.value.hashCode()
                                rulesVersionFlow.value = rulesVersionFlow.value + 1
                            }
                            else -> {}
                        }
                    }
                }
            }
        }.also { it.startWatching() }

        // Preload settings and rule engine
        lifecycleScope.launch(Dispatchers.IO) {
            when (val res = settingsService.loadEffective()) {
                is AppResult.Success -> {
                    with(hostCanonicalizer) { ruleEngine.load(res.value) }
                    lastCompiledSettingsHash = res.value.hashCode()
                    rulesVersionFlow.value = rulesVersionFlow.value + 1
                }
                else -> { /* TODO: surface error state in UI if needed */ }
            }
        }

        // Recompile rule engine on every resume to ensure edits take effect immediately
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleScope.launch(Dispatchers.IO) {
                    when (val res = settingsService.loadEffective()) {
                        is AppResult.Success -> {
                            with(hostCanonicalizer) { ruleEngine.load(res.value) }
                            lastCompiledSettingsHash = res.value.hashCode()
                            rulesVersionFlow.value = rulesVersionFlow.value + 1
                        }
                        else -> {}
                    }
                }
            }
        })

        val initialShared: String? = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        setContent {
            val uiSettingsService = CompositionRoot.provideUiSettingsService(this@MainActivity)
            val rulesVersion by rulesVersionFlow.asStateFlow().collectAsState()
            var uiSettings by remember { mutableStateOf(uiSettingsService.getCurrentSettings()) }
            
            // Make UI settings reactive - refresh when returning from settings
            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        uiSettings = uiSettingsService.getCurrentSettings()
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
            
            DetracktorTheme(themeMode = uiSettings.themeMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) {padding ->
                    MainScreen(
                        modifier = Modifier.padding(padding),
                        settingsService = settingsService,
                        ruleEngine = ruleEngine,
                        hostCanonicalizer = hostCanonicalizer,
                        urlParser = urlParser,
                        uiSettingsService = uiSettingsService,
                        uiSettings = uiSettings,
                        initialShared = initialShared,
                        rulesVersion = rulesVersion
                    )
                }
            }
        }
    }
}
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    settingsService: SettingsService,
    ruleEngine: RuleEngine,
    hostCanonicalizer: HostCanonicalizer,
    urlParser: UrlParser,
    uiSettingsService: UiSettingsService,
    uiSettings: com.gologlu.detracktor.runtime.android.presentation.types.UiSettings,
    initialShared: String? = null,
    rulesVersion: Int
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var original by remember { mutableStateOf<String?>(initialShared) }
    var cleaned by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String>(context.getString(R.string.status_ready)) }
    var blurEnabled by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val colorScheme = MaterialTheme.colorScheme
    var currentParts by remember { mutableStateOf<UrlParts?>(null) }
    var currentEffects by remember { mutableStateOf<List<TokenEffect>>(emptyList()) }
    var currentEvaluation by remember { mutableStateOf<Evaluation?>(null) }
    var warningData by remember { mutableStateOf(WarningDisplayData(false, emptyList())) }
    var ruleMatchData by remember { mutableStateOf(RuleMatchDisplayData(emptyList())) }
    var blurState by remember { mutableStateOf(BlurState()) }
    var warningPanelExpanded by remember { mutableStateOf(false) }
    var resumeTick by remember { mutableStateOf(0) }
    var shareHandled by remember { mutableStateOf(false) }
    var dialogState by remember { mutableStateOf(DialogState()) }
    var clipboardState by remember { mutableStateOf(ClipboardState.EMPTY) }
    var instructionalContent by remember { mutableStateOf(getInstructionalContent(context, ClipboardState.EMPTY)) }
    
    // Make uiSettings mutable within this composable for immediate updates
    var currentUiSettings by remember { mutableStateOf(uiSettings) }
    
    // Update currentUiSettings when the parent uiSettings changes (e.g., on resume)
    LaunchedEffect(uiSettings) {
        currentUiSettings = uiSettings
    }

    // Auto-read clipboard on focus (onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resumeTick) {
        if (resumeTick > 0) {
            delay(250)
            original = readClipboard(context)
            clipboardState = determineClipboardState(context, urlParser)
            // Preserve expansion state when updating instructional content
            instructionalContent = getInstructionalContent(context, clipboardState).copy(
                isExpanded = instructionalContent.isExpanded
            )
        }
    }

    LaunchedEffect(Unit) {
        if (original.isNullOrBlank()) {
            original = readClipboard(context)
            clipboardState = determineClipboardState(context, urlParser)
            // Preserve expansion state when updating instructional content
            instructionalContent = getInstructionalContent(context, clipboardState).copy(
                isExpanded = instructionalContent.isExpanded
            )
        }
    }

    // Re-parse and re-evaluate when URL or rules version changes
    LaunchedEffect(original, rulesVersion, clipboardState) {
        val raw = original
        if (raw.isNullOrBlank()) {
            currentParts = null
            currentEffects = emptyList()
            currentEvaluation = null
            warningData = buildWarningData(null, null).copy(isExpanded = warningPanelExpanded)
            ruleMatchData = RuleMatchDisplayData(emptyList())
            
            // Use new status message logic - no error message duplication
            status = getStatusMessage(context, clipboardState, null)
            errorMessage = null // Don't duplicate the status message
            return@LaunchedEffect
        }
        when (val parsed: DomainResult<UrlParts> = urlParser.parse(raw as MaybeUrl)) {
            is DomainResult.Success -> {
                val parts = parsed.value
                // Validate that this is a proper URL (requires scheme and host)
                val isValidUrl = when (val validated = with(urlParser) { Url.from(raw as MaybeUrl) }) {
                    is DomainResult.Success -> true
                    is DomainResult.Failure -> false
                }
                if (!isValidUrl) {
                    currentParts = null
                    currentEffects = emptyList()
                    currentEvaluation = null
                    warningData = buildWarningData(null, null).copy(isExpanded = warningPanelExpanded)
                    ruleMatchData = RuleMatchDisplayData(emptyList())
                    errorMessage = context.getString(R.string.status_not_a_url)
                    status = context.getString(R.string.status_invalid_url)
                    return@LaunchedEffect
                }
                val eval = with(hostCanonicalizer) { ruleEngine.evaluate(parts) }
                currentParts = parts
                currentEffects = eval.tokenEffects
                currentEvaluation = eval
                
                // Build separated warning and rule match data, preserving expansion state
                warningData = buildWarningData(parts, eval).copy(isExpanded = warningPanelExpanded)
                ruleMatchData = buildRuleMatchData(parts, eval, currentEffects)
                blurState = BlurStateCalculator.calculateBlurState(parts, currentEffects)
                
                errorMessage = null
                status = if (eval.matches.isEmpty()) context.getString(R.string.status_no_rules_matched) else context.getString(R.string.status_rules_matched, eval.matches.size)

                // Share intent pass-thru: handle based on warnings and settings
                if (initialShared != null && !shareHandled) {
                    val hasWarnings = warningData.hasWarnings
                    val cleanedUrl = with(hostCanonicalizer) { ruleEngine.applyRemovals(parts) }.toUrlString()
                    
                    if (!hasWarnings || currentUiSettings.suppressShareWarnings) {
                        // Auto-share when no warnings or warnings are suppressed
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            this.type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, cleanedUrl)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title)))
                        Toast.makeText(context, context.getString(R.string.toast_cleaned_and_shared), Toast.LENGTH_SHORT).show()
                        shareHandled = true
                        if (context is ComponentActivity) context.finish()
                    } else {
                        // Show share warning dialog when warnings exist and not suppressed
                        dialogState = DialogState(
                            isVisible = true,
                            type = DialogType.SHARE_INTENT_WARNING,
                            cleanedUrl = cleanedUrl,
                            warningData = warningData
                        )
                        shareHandled = true
                    }
                }
            }
            is DomainResult.Failure -> {
                currentParts = null
                currentEffects = emptyList()
                currentEvaluation = null
                warningData = buildWarningData(null, null).copy(isExpanded = warningPanelExpanded)
                ruleMatchData = RuleMatchDisplayData(emptyList())
                errorMessage = context.getString(R.string.status_not_a_url)
                status = context.getString(R.string.status_invalid_url)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("main-screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ){
        Text(
            text = context.getString(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("title")
        )
        Text(text = "Status: $status", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("status"))
        if (errorMessage != null) {
            Text(text = errorMessage!!, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("error"))
        } else {
            currentParts?.let { parts ->
                // Use unified inline blur URL preview
                UrlPreviewInlineBlur(
                    parts = parts,
                    tokenEffects = currentEffects,
                    blurEnabled = blurEnabled,
                    highlight = colorScheme.primary,
                    muted = colorScheme.onSurface
                )
                
                // Show/Hide values button positioned near URL preview
                if (BlurStateCalculator.shouldShowRevealButton(blurState)) {
                    Button(
                        onClick = { blurEnabled = !blurEnabled }, 
                        modifier = Modifier.testTag("toggle-blur")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (blurEnabled) R.drawable.visibility_24px else R.drawable.visibility_off_24px
                                ),
                                contentDescription = null
                            )
                            Text(if (blurEnabled) context.getString(R.string.button_reveal_values) else context.getString(R.string.button_hide_values))
                        }
                    }
                }
                
                // Enhanced warning panel
                WarningPanel(
                    warningData = warningData,
                    onToggleExpanded = { 
                        warningPanelExpanded = !warningPanelExpanded
                        warningData = warningData.copy(isExpanded = warningPanelExpanded)
                    }
                )
                
                // Enhanced rules summary
                if (ruleMatchData.hasMatches) {
                    RulesSummary(
                        matchedRules = ruleMatchData.matchedRules
                    )
                }
            }
        }
        
        // Clean URL button - enabled based on clipboard state
        Button(
            onClick = {
                if (clipboardState == ClipboardState.VALID_URL) {
                    currentParts?.let { p ->
                        val cleanedUrl = with(hostCanonicalizer) { ruleEngine.applyRemovals(p) }.toUrlString()
                        handleManualCleaning(cleanedUrl, currentUiSettings, uiSettingsService, context) { newDialogState ->
                            dialogState = newDialogState
                        }
                    }
                } else {
                    // Provide feedback for non-valid clipboard states
                    val message = when (clipboardState) {
                        ClipboardState.EMPTY -> context.getString(R.string.error_clipboard_empty_copy_url)
                        ClipboardState.NON_TEXT -> context.getString(R.string.error_clipboard_non_text_content)
                        ClipboardState.TEXT_NOT_URL -> context.getString(R.string.error_clipboard_text_not_url)
                        ClipboardState.VALID_URL -> "" // Should not reach here
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            enabled = shouldEnableCleanButton(clipboardState),
            modifier = Modifier.testTag("clean-action")
        ) {
            Text(context.getString(R.string.button_clean_url))
        }
        
        // Instructional Panel - positioned between main content and settings
        InstructionalPanel(
            content = instructionalContent,
            onToggleExpanded = { 
                instructionalContent = instructionalContent.copy(isExpanded = !instructionalContent.isExpanded)
            },
            modifier = Modifier.testTag("instructional-panel")
        )
        
        // Spacer to push settings link to bottom
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
        
        // Settings as bottom navigation/footer
        androidx.compose.material3.TextButton(
            onClick = {
                context.startActivity(Intent(context, ConfigActivity::class.java))
            },
            modifier = Modifier.testTag("open-settings")
        ) {
            Text(
                text = context.getString(R.string.button_open_settings),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // New dialog system
        if (dialogState.isVisible && dialogState.cleanedUrl != null) {
            when (dialogState.type) {
                DialogType.MANUAL_CLEANING -> {
                    CleaningDialog(
                        cleanedUrl = dialogState.cleanedUrl!!,
                        onShare = { rememberChoice ->
                            executeAfterCleaningAction(AfterCleaningAction.ALWAYS_SHARE, dialogState.cleanedUrl!!, context)
                            if (rememberChoice) {
                                uiSettingsService.updateAfterCleaningAction(AfterCleaningAction.ALWAYS_SHARE)
                                // Update local state immediately for current session
                                currentUiSettings = currentUiSettings.copy(afterCleaningAction = AfterCleaningAction.ALWAYS_SHARE)
                            }
                            dialogState = DialogState()
                        },
                        onCopy = { rememberChoice ->
                            executeAfterCleaningAction(AfterCleaningAction.ALWAYS_COPY, dialogState.cleanedUrl!!, context)
                            if (rememberChoice) {
                                uiSettingsService.updateAfterCleaningAction(AfterCleaningAction.ALWAYS_COPY)
                                // Update local state immediately for current session
                                currentUiSettings = currentUiSettings.copy(afterCleaningAction = AfterCleaningAction.ALWAYS_COPY)
                            }
                            // Refresh preview by assigning cleaned text back to original
                            original = dialogState.cleanedUrl
                            dialogState = DialogState()
                        },
                        onDismiss = { dialogState = DialogState() }
                    )
                }
                DialogType.SHARE_INTENT_WARNING -> {
                    ShareWarningDialog(
                        cleanedUrl = dialogState.cleanedUrl!!,
                        warningData = dialogState.warningData ?: WarningDisplayData(false, emptyList()),
                        onShare = { dontWarnAgain ->
                            if (dontWarnAgain) {
                                uiSettingsService.updateSuppressShareWarnings(true)
                            }
                            executeAfterCleaningAction(AfterCleaningAction.ALWAYS_SHARE, dialogState.cleanedUrl!!, context)
                            dialogState = DialogState()
                            if (context is ComponentActivity) context.finish()
                        },
                        onCopy = { dontWarnAgain ->
                            if (dontWarnAgain) {
                                uiSettingsService.updateSuppressShareWarnings(true)
                            }
                            executeAfterCleaningAction(AfterCleaningAction.ALWAYS_COPY, dialogState.cleanedUrl!!, context)
                            dialogState = DialogState()
                            if (context is ComponentActivity) context.finish()
                        },
                        onDismiss = { dialogState = DialogState() }
                    )
                }
            }
        }
    }
}

private fun readClipboard(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip: ClipData? = cm.primaryClip
    val item = clip?.getItemAt(0)
    val text = item?.coerceToText(context)?.toString()
    return text
}

private fun clipboardHasNonText(context: Context): Boolean {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip: ClipData = cm.primaryClip ?: return false
    if (clip.itemCount == 0) return false
    val desc = clip.description
    val hasTextMime = desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
            desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
    if (hasTextMime) return false
    val text = clip.getItemAt(0).coerceToText(context)?.toString()
    return text.isNullOrBlank()
}

/**
 * Determine the current clipboard state for UX feedback
 */
private fun determineClipboardState(context: Context, urlParser: UrlParser): ClipboardState {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip: ClipData? = cm.primaryClip
    
    // No clipboard content at all
    if (clip == null || clip.itemCount == 0) {
        return ClipboardState.EMPTY
    }
    
    // Try to get text content
    val item = clip.getItemAt(0)
    val text = item?.coerceToText(context)?.toString()
    
    // Check if clipboard is truly empty (no text content)
    if (text.isNullOrEmpty()) {
        // Check if there's any content at all in the clipboard
        val hasAnyContent = item?.text != null || item?.htmlText != null || item?.uri != null
        return if (hasAnyContent) ClipboardState.NON_TEXT else ClipboardState.EMPTY
    }
    
    // Text is blank/whitespace only
    if (text.isBlank()) {
        return ClipboardState.EMPTY
    }
    
    // Check if text is a valid URL
    return when (val parsed = urlParser.parse(text as MaybeUrl)) {
        is DomainResult.Success -> {
            // Further validate that it's a proper URL with scheme and host
            when (val validated = with(urlParser) { Url.from(text as MaybeUrl) }) {
                is DomainResult.Success -> ClipboardState.VALID_URL
                is DomainResult.Failure -> ClipboardState.TEXT_NOT_URL
            }
        }
        is DomainResult.Failure -> ClipboardState.TEXT_NOT_URL
    }
}

/**
 * Generate appropriate status message based on clipboard state and evaluation
 */
private fun getStatusMessage(context: Context, clipboardState: ClipboardState, evaluation: Evaluation?): String {
    return when (clipboardState) {
        ClipboardState.EMPTY -> context.getString(R.string.status_clipboard_empty)
        ClipboardState.NON_TEXT -> context.getString(R.string.status_clipboard_non_text)
        ClipboardState.TEXT_NOT_URL -> context.getString(R.string.status_clipboard_text_not_url)
        ClipboardState.VALID_URL -> {
            if (evaluation == null) {
                context.getString(R.string.status_processing_url)
            } else if (evaluation.matches.isEmpty()) {
                context.getString(R.string.status_no_rules_matched)
            } else {
                context.getString(R.string.status_rules_matched, evaluation.matches.size)
            }
        }
    }
}

/**
 * Determine if Clean URL button should be enabled based on clipboard state
 */
private fun shouldEnableCleanButton(clipboardState: ClipboardState): Boolean {
    return clipboardState == ClipboardState.VALID_URL
}

/**
 * Generate context-appropriate instructional content
 */
private fun getInstructionalContent(context: Context, clipboardState: ClipboardState): InstructionalContent {
    return when (clipboardState) {
        ClipboardState.EMPTY -> InstructionalContent(
            title = context.getString(R.string.instructional_title_how_to_use),
            steps = listOf(
                context.getString(R.string.instructional_step_copy_url),
                context.getString(R.string.instructional_step_return_detracktor),
                context.getString(R.string.instructional_step_review_parameters),
                context.getString(R.string.instructional_step_tap_clean_url),
                context.getString(R.string.instructional_step_share_or_copy),
                context.getString(R.string.instructional_step_direct_share),
                context.getString(R.string.instructional_step_customize_rules)
            )
        )
        ClipboardState.NON_TEXT -> InstructionalContent(
            title = context.getString(R.string.instructional_title_clipboard_not_supported),
            steps = listOf(
                context.getString(R.string.instructional_step_text_urls_only),
                context.getString(R.string.instructional_step_copy_url_text),
                context.getString(R.string.instructional_step_return_to_clean)
            )
        )
        ClipboardState.TEXT_NOT_URL -> InstructionalContent(
            title = context.getString(R.string.instructional_title_invalid_url_format),
            steps = listOf(
                context.getString(R.string.instructional_step_text_not_valid_url),
                context.getString(R.string.instructional_step_include_https),
                context.getString(R.string.instructional_step_urls_start_http),
                context.getString(R.string.instructional_step_try_copying_again)
            )
        )
        ClipboardState.VALID_URL -> InstructionalContent(
            title = context.getString(R.string.instructional_title_url_ready),
            steps = listOf(
                context.getString(R.string.instructional_step_review_url_preview),
                context.getString(R.string.instructional_step_sensitive_data_warnings),
                context.getString(R.string.instructional_step_trackers_shown),
                context.getString(R.string.instructional_step_unknown_params_blurred),
                context.getString(R.string.instructional_step_tap_show_values),
                context.getString(R.string.instructional_step_tap_clean_url),
                context.getString(R.string.instructional_step_choose_share_copy)
            )
        )
    }
}

private fun buildAnnotatedUrl(
    parts: UrlParts,
    tokenEffects: List<TokenEffect>,
    blurEnabled: Boolean,
    blurUserInfo: Boolean,
    highlight: Color,
    muted: Color
): AnnotatedString {
    fun obfuscate(value: String): String = if (!blurEnabled) value else if (value.isEmpty()) "" else "••••••"

    val tokens: List<QueryToken> = parts.queryPairs.tokens
    val effectsByIndex = tokenEffects.associateBy { it.tokenIndex }
    
    return buildAnnotatedString {
        // scheme://
        parts.scheme?.let { append("$it://") }

        // userInfo@
        parts.userInfo?.let { ui ->
            val shown = if (blurUserInfo) obfuscate(ui) else ui
            pushStyle(SpanStyle(color = muted))
            append(shown)
            pop()
            append("@")
        }

        // host
        parts.host?.let { h ->
            pushStyle(SpanStyle(color = muted))
            append(h)
            pop()
        }

        // :port
        parts.port?.let { p ->
            pushStyle(SpanStyle(color = muted))
            append(":$p")
            pop()
        }

        // path
        parts.path?.let { p ->
            pushStyle(SpanStyle(color = muted))
            append(p)
            pop()
        }

        // ?query
        if (tokens.isNotEmpty()) {
            append("?")
            tokens.forEachIndexed { idx, tok ->
                if (idx > 0) append("&")
                val eff = effectsByIndex[idx]
                val isRemoval = eff?.willBeRemoved == true
                val keyColor = if (isRemoval) highlight else muted
                val valueColor = if (isRemoval) highlight else muted

                // key
                pushStyle(SpanStyle(color = keyColor))
                append(tok.decodedKey)
                pop()

                if (tok.hasEquals) {
                    append("=")
                    // value
                    val shownVal = if (isRemoval) tok.decodedValue else obfuscate(tok.decodedValue)
                    pushStyle(SpanStyle(color = valueColor))
                    append(shownVal)
                    pop()
                }
            }
        }

        // #fragment
        parts.fragment?.let { f ->
            append("#")
            pushStyle(SpanStyle(color = muted))
            append(f)
            pop()
        }
    }
}

private fun summarizeWarnings(
    context: Context,
    parts: UrlParts,
    eval: Evaluation
): String? {
    val warnings = mutableListOf<String>()
    if ((eval.effectiveWarnings.warnOnEmbeddedCredentials == true) && !parts.userInfo.isNullOrEmpty()) {
        warnings.add(context.getString(R.string.warning_embedded_detected))
    }
    eval.effectiveWarnings.sensitiveParams?.let { sens ->
        val present = parts.queryPairs.tokens.map { it.decodedKey }.toSet().intersect(sens.toSet())
        if (present.isNotEmpty()) warnings.add(context.getString(R.string.warning_sensitive_params_found, present.joinToString(", ")))
    }
    if (eval.matches.isNotEmpty()) {
        warnings.add(context.getString(R.string.warning_matched_rules, eval.matches.size))
    }
    return if (warnings.isEmpty()) null else warnings.joinToString(" • ")
}

private fun hasWarnings(
    parts: UrlParts,
    eval: Evaluation
): Boolean {
    val warnCreds = eval.effectiveWarnings.warnOnEmbeddedCredentials
    if (warnCreds == true && !parts.userInfo.isNullOrEmpty()) return true
    val sens = eval.effectiveWarnings.sensitiveParams
    if (sens != null && sens.isNotEmpty()) {
        val present = parts.queryPairs.tokens.map { it.decodedKey }.toSet().intersect(sens.toSet())
        if (present.isNotEmpty()) return true
    }
    return false
}

private fun buildWarningData(
    parts: UrlParts?,
    eval: Evaluation?
): WarningDisplayData {
    if (parts == null || eval == null) {
        return WarningDisplayData(hasCredentials = false, sensitiveParams = emptyList())
    }
    
    val hasCredentials = (eval.effectiveWarnings.warnOnEmbeddedCredentials == true) && !parts.userInfo.isNullOrEmpty()
    
    val sensitiveParams = eval.effectiveWarnings.sensitiveParams?.let { sens ->
        parts.queryPairs.tokens.map { it.decodedKey }.toSet().intersect(sens.toSet()).toList()
    } ?: emptyList()
    
    return WarningDisplayData(
        hasCredentials = hasCredentials,
        sensitiveParams = sensitiveParams
    )
}

private fun buildRuleMatchData(
    parts: UrlParts,
    eval: Evaluation,
    tokenEffects: List<TokenEffect>
): RuleMatchDisplayData {
    val matchedRules = eval.matches.mapNotNull { match ->
        // Get only the parameters that actually matched for this rule
        val actuallyMatchedParams = tokenEffects
            .filter { effect -> effect.willBeRemoved && effect.matchedRuleIndexes.contains(match.index) }
            .map { effect -> effect.name }
        
        // Only create a rule summary if this rule actually removed parameters
        if (actuallyMatchedParams.isNotEmpty()) {
            RuleMatchSummary(
                description = "Rule matched for ${parts.host ?: "unknown"}",
                matchedParams = actuallyMatchedParams,
                domain = parts.host ?: "unknown"
            )
        } else {
            null // Filter out rules that matched but didn't remove anything
        }
    }
    
    // Deduplicate identical rule entries
    val deduplicatedRules = deduplicateRuleMatches(matchedRules)
    
    return RuleMatchDisplayData(matchedRules = deduplicatedRules)
}

/**
 * Remove duplicate rule entries that have identical domain and matched parameters
 */
private fun deduplicateRuleMatches(
    matches: List<RuleMatchSummary>
): List<RuleMatchSummary> {
    return matches.distinctBy { rule ->
        // Create a unique key based on domain and sorted matched parameters
        "${rule.domain}:${rule.matchedParams.sorted().joinToString(",")}"
    }
}

/**
 * Handle manual URL cleaning based on user settings
 */
private fun handleManualCleaning(
    cleanedUrl: String,
    uiSettings: com.gologlu.detracktor.runtime.android.presentation.types.UiSettings,
    uiSettingsService: UiSettingsService,
    context: Context,
    onShowDialog: (DialogState) -> Unit
) {
    when (uiSettings.afterCleaningAction) {
        AfterCleaningAction.ALWAYS_SHARE -> {
            executeAfterCleaningAction(AfterCleaningAction.ALWAYS_SHARE, cleanedUrl, context)
        }
        AfterCleaningAction.ALWAYS_COPY -> {
            executeAfterCleaningAction(AfterCleaningAction.ALWAYS_COPY, cleanedUrl, context)
        }
        AfterCleaningAction.ASK -> {
            onShowDialog(
                DialogState(
                    isVisible = true,
                    type = DialogType.MANUAL_CLEANING,
                    cleanedUrl = cleanedUrl,
                    warningData = null
                )
            )
        }
    }
}

/**
 * Execute the specified after-cleaning action
 */
private fun executeAfterCleaningAction(
    action: AfterCleaningAction,
    cleanedUrl: String,
    context: Context
) {
    when (action) {
        AfterCleaningAction.ALWAYS_SHARE -> {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                this.type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, cleanedUrl)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share cleaned URL"))
            Toast.makeText(context, "Cleaned and ready to share", Toast.LENGTH_SHORT).show()
        }
        AfterCleaningAction.ALWAYS_COPY -> {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("cleaned-url", cleanedUrl))
            Toast.makeText(context, "Copied cleaned URL", Toast.LENGTH_SHORT).show()
        }
        AfterCleaningAction.ASK -> {
            // This should not be called directly for ASK action
            // ASK action should show a dialog instead
        }
    }
}
