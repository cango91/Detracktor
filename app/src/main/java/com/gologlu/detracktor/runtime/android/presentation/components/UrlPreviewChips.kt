package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gologlu.detracktor.application.service.match.TokenEffect
import com.gologlu.detracktor.domain.model.QueryPairs
import com.gologlu.detracktor.domain.model.QueryToken
import com.gologlu.detracktor.domain.model.UrlParts

/**
 * URL preview component using chip-based display for query parameters.
 * This is the current/original display mode extracted from MainActivity.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UrlPreviewChips(
    parts: UrlParts,
    tokenEffects: List<TokenEffect>,
    blurEnabled: Boolean,
    highlight: Color,
    muted: Color,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp), 
        modifier = modifier.testTag("url-preview-chips")
    ) {
        // Top line: scheme://userinfo@host:port/path (single wrapped Text)
        val head = buildUrlHead(
            parts = parts,
            blurEnabled = blurEnabled,
            highlight = highlight,
            muted = muted
        )
        Text(
            text = head, 
            style = MaterialTheme.typography.bodyLarge, 
            maxLines = 2, 
            overflow = TextOverflow.Ellipsis, 
            modifier = Modifier.testTag("url-head")
        )

        // Query chips
        val tokens = parts.queryPairs.getTokens()
        if (tokens.isNotEmpty()) {
            Text(
                text = "Parameters:", 
                style = MaterialTheme.typography.labelLarge, 
                color = muted, 
                modifier = Modifier.testTag("params-label")
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp), 
                verticalArrangement = Arrangement.spacedBy(8.dp), 
                modifier = Modifier.testTag("params-flow")
            ) {
                renderQueryChips(
                    tokens = tokens,
                    tokenEffects = tokenEffects,
                    blurEnabled = blurEnabled,
                    highlight = highlight,
                    muted = muted
                )
            }
        }
    }
}

/**
 * Build the URL head (everything except query parameters) as AnnotatedString
 */
private fun buildUrlHead(
    parts: UrlParts,
    blurEnabled: Boolean,
    highlight: Color,
    muted: Color
): AnnotatedString {
    fun obfuscate(value: String): String = if (!blurEnabled) value else if (value.isEmpty()) "" else "••••••"
    
    return buildAnnotatedString {
        // scheme://
        parts.scheme?.let { append("$it://") }

        // userInfo@
        parts.userInfo?.let { ui ->
            val shown = obfuscate(ui)
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

        // #fragment
        parts.fragment?.let { f ->
            append("#")
            pushStyle(SpanStyle(color = muted))
            append(f)
            pop()
        }
    }
}

/**
 * Render query parameters as chips in a FlowRow
 */
@Composable
private fun renderQueryChips(
    tokens: List<QueryToken>,
    tokenEffects: List<TokenEffect>,
    blurEnabled: Boolean,
    highlight: Color,
    muted: Color
) {
    fun obfuscate(value: String): String = if (!blurEnabled) value else if (value.isEmpty()) "" else "••••••"
    
    val effectsByIndex = tokenEffects.associateBy { it.tokenIndex }
    tokens.forEachIndexed { idx, tok ->
        val eff = effectsByIndex[idx]
        val isRemoval = eff?.willBeRemoved == true
        val chipColor = if (isRemoval) highlight else muted
        val valueShown = if (isRemoval || !blurEnabled) tok.decodedValue else obfuscate(tok.decodedValue)
        Text(
            text = "${tok.decodedKey}${if (tok.hasEquals) "=$valueShown" else ""}",
            color = chipColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("param-$idx")
        )
    }
}
