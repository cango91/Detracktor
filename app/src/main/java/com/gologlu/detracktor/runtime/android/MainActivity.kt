package com.gologlu.detracktor.runtime.android

import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
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
import com.gologlu.detracktor.domain.service.UrlParser
import com.gologlu.detracktor.runtime.android.presentation.ui.theme.DetracktorTheme
import com.gologlu.detracktor.runtime.android.presentation.components.UrlPreviewChips
import com.gologlu.detracktor.runtime.android.presentation.components.UrlPreviewInlineBlur
import com.gologlu.detracktor.runtime.android.presentation.components.WarningPanel
import com.gologlu.detracktor.runtime.android.presentation.components.RulesSummary
import com.gologlu.detracktor.runtime.android.presentation.types.UrlPreviewMode
import com.gologlu.detracktor.runtime.android.presentation.types.WarningDisplayData
import com.gologlu.detracktor.runtime.android.presentation.types.RuleMatchDisplayData
import com.gologlu.detracktor.runtime.android.presentation.types.RuleMatchSummary
import com.gologlu.detracktor.runtime.android.presentation.types.AfterCleaningAction
import com.gologlu.detracktor.runtime.android.service.UiSettingsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.jvm.java

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsService: SettingsService = CompositionRoot.provideSettingsService(this)
        val ruleEngine: RuleEngine = CompositionRoot.provideRuleEngine()
        val hostCanonicalizer: HostCanonicalizer = CompositionRoot.provideHostCanonicalizer()
        val urlParser: UrlParser = CompositionRoot.provideUrlParser()

        // Preload settings and rule engine
        lifecycleScope.launch(Dispatchers.IO) {
            when (val res = settingsService.loadEffective()) {
                is AppResult.Success -> {
                    with(hostCanonicalizer) { ruleEngine.load(res.value) }
                }
                else -> { /* TODO: surface error state in UI if needed */ }
            }
        }

        val initialShared: String? = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        setContent {
            val uiSettingsService = CompositionRoot.provideUiSettingsService(this@MainActivity)
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
                        initialShared = initialShared
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
    initialShared: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var original by remember { mutableStateOf<String?>(initialShared) }
    var cleaned by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String>("Ready") }
    var blurEnabled by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val colorScheme = MaterialTheme.colorScheme
    var currentParts by remember { mutableStateOf<UrlParts?>(null) }
    var currentEffects by remember { mutableStateOf<List<TokenEffect>>(emptyList()) }
    var currentEvaluation by remember { mutableStateOf<Evaluation?>(null) }
    var warningData by remember { mutableStateOf(WarningDisplayData(false, emptyList())) }
    var ruleMatchData by remember { mutableStateOf(RuleMatchDisplayData(emptyList())) }
    var resumeTick by remember { mutableStateOf(0) }
    var shareHandled by remember { mutableStateOf(false) }
    var showCleanDialog by remember { mutableStateOf(false) }
    var pendingCleaned by remember { mutableStateOf<String?>(null) }

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
        }
    }

    LaunchedEffect(Unit) {
        if (original.isNullOrBlank()) {
            original = readClipboard(context)
        }
    }

    LaunchedEffect(original, blurEnabled) {
        val raw = original
        if (raw.isNullOrBlank()) {
            currentParts = null
            currentEffects = emptyList()
            currentEvaluation = null
            warningData = WarningDisplayData(false, emptyList())
            ruleMatchData = RuleMatchDisplayData(emptyList())
            errorMessage = null
            status = "Ready"
            return@LaunchedEffect
        }
        when (val parsed: DomainResult<UrlParts> = urlParser.parse(raw as MaybeUrl)) {
            is DomainResult.Success -> {
                val parts = parsed.value
                val eval = with(hostCanonicalizer) { ruleEngine.evaluate(parts) }
                currentParts = parts
                currentEffects = eval.tokenEffects
                currentEvaluation = eval
                
                // Build separated warning and rule match data
                warningData = buildWarningData(parts, eval)
                ruleMatchData = buildRuleMatchData(parts, eval, currentEffects)
                
                errorMessage = null
                status = if (eval.matches.isEmpty()) "No rules matched" else "${eval.matches.size} rule(s) matched"

                // Share intent pass-thru: auto-clean only when no warnings
                if (initialShared != null && !shareHandled) {
                    val hasWarnings = warningData.hasWarnings
                    val cleanedUrl = with(hostCanonicalizer) { ruleEngine.applyRemovals(parts) }.toUrlString()
                    if (!hasWarnings) {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            this.type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, cleanedUrl)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share cleaned URL"))
                        Toast.makeText(context, "Cleaned and ready to share", Toast.LENGTH_SHORT).show()
                        shareHandled = true
                        if (context is ComponentActivity) context.finish()
                    } else {
                        pendingCleaned = cleanedUrl
                        showCleanDialog = true
                        shareHandled = true
                    }
                }
            }
            is DomainResult.Failure -> {
                currentParts = null
                currentEffects = emptyList()
                currentEvaluation = null
                warningData = WarningDisplayData(false, emptyList())
                ruleMatchData = RuleMatchDisplayData(emptyList())
                errorMessage = "Clipboard doesn't contain a valid URL"
                status = "Invalid URL"
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
            text = "Detracktor",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("title")
        )
        Text(text = "Status: $status", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("status"))
        if (errorMessage != null) {
            Text(text = errorMessage!!, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("error"))
        } else {
            currentParts?.let { parts ->
                // Use new component-based URL preview based on settings
                when (uiSettings.urlPreviewMode) {
                    UrlPreviewMode.CHIPS -> {
                        UrlPreviewChips(
                            parts = parts,
                            tokenEffects = currentEffects,
                            blurEnabled = blurEnabled,
                            highlight = colorScheme.primary,
                            muted = colorScheme.onSurface
                        )
                    }
                    UrlPreviewMode.INLINE_BLUR -> {
                        UrlPreviewInlineBlur(
                            parts = parts,
                            tokenEffects = currentEffects,
                            blurEnabled = blurEnabled,
                            highlight = colorScheme.primary,
                            muted = colorScheme.onSurface
                        )
                    }
                }
                
                // Enhanced warning panel
                WarningPanel(
                    warningData = warningData,
                    onToggleExpanded = { 
                        warningData = warningData.copy(isExpanded = !warningData.isExpanded)
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
        
        Button(onClick = { blurEnabled = !blurEnabled }, modifier = Modifier.testTag("toggle-blur")) {
            Text(if (blurEnabled) "Reveal values" else "Hide values")
        }
        Button(onClick = {
            currentParts?.let { p ->
                val cleanedUrl = with(hostCanonicalizer) { ruleEngine.applyRemovals(p) }.toUrlString()
                pendingCleaned = cleanedUrl
                showCleanDialog = true
            }
        }, modifier = Modifier.testTag("clean-action")) {
            Text("Clean URL")
        }
        Button(onClick = {
            context.startActivity(Intent(context, ConfigActivity::class.java))
        }, modifier = Modifier.testTag("open-settings")) {
            Text("Open Settings")
        }

        if (showCleanDialog && pendingCleaned != null) {
            AlertDialog(
                onDismissRequest = { showCleanDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        // Share cleaned
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            this.type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, pendingCleaned)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share cleaned URL"))
                        Toast.makeText(context, "Cleaned and ready to share", Toast.LENGTH_SHORT).show()
                        showCleanDialog = false
                    }) { Text("Share") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        // Copy cleaned
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("cleaned-url", pendingCleaned))
                        // Refresh preview by assigning cleaned text back to original
                        original = pendingCleaned
                        Toast.makeText(context, "Copied cleaned URL", Toast.LENGTH_SHORT).show()
                        showCleanDialog = false
                    }) { Text("Copy") }
                },
                text = { Text("Cleaned URL is ready.") },
                title = { Text("URL Cleaned") }
            )
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

private fun buildAnnotatedUrl(
    parts: UrlParts,
    tokenEffects: List<TokenEffect>,
    blurEnabled: Boolean,
    blurUserInfo: Boolean,
    highlight: Color,
    muted: Color
): AnnotatedString {
    fun obfuscate(value: String): String = if (!blurEnabled) value else if (value.isEmpty()) "" else "••••••"

    val tokens: List<QueryToken> = parts.queryPairs.getTokens()
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
    parts: UrlParts,
    eval: Evaluation
): String? {
    val warnings = mutableListOf<String>()
    if ((eval.effectiveWarnings.warnOnEmbeddedCredentials == true) && !parts.userInfo.isNullOrEmpty()) {
        warnings.add("Embedded credentials detected")
    }
    eval.effectiveWarnings.sensitiveParams?.let { sens ->
        val present = parts.queryPairs.getTokens().map { it.decodedKey }.toSet().intersect(sens.toSet())
        if (present.isNotEmpty()) warnings.add("Sensitive params: ${present.joinToString(", ")}")
    }
    if (eval.matches.isNotEmpty()) {
        warnings.add("Matched rules: ${eval.matches.size}")
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
        val present = parts.queryPairs.getTokens().map { it.decodedKey }.toSet().intersect(sens.toSet())
        if (present.isNotEmpty()) return true
    }
    return false
}

private fun buildWarningData(
    parts: UrlParts,
    eval: Evaluation
): WarningDisplayData {
    val hasCredentials = (eval.effectiveWarnings.warnOnEmbeddedCredentials == true) && !parts.userInfo.isNullOrEmpty()
    
    val sensitiveParams = eval.effectiveWarnings.sensitiveParams?.let { sens ->
        parts.queryPairs.getTokens().map { it.decodedKey }.toSet().intersect(sens.toSet()).toList()
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
