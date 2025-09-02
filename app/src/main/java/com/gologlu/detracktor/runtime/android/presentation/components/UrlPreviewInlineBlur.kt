package com.gologlu.detracktor.runtime.android.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gologlu.detracktor.application.service.match.TokenEffect
import com.gologlu.detracktor.domain.model.QueryToken
import com.gologlu.detracktor.domain.model.UrlParts

/**
 * URL preview component using inline blur effects for query parameters.
 * This is the new variation that displays parameters inline with blur effects
 * instead of using chips below the URL.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UrlPreviewInlineBlur(
    parts: UrlParts,
    tokenEffects: List<TokenEffect>,
    blurEnabled: Boolean,
    highlight: Color,
    muted: Color,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag("url-preview-inline-blur")
    ) {
        // Build complete URL with inline parameters and blur effects
        val inlineUrl = buildInlineBlurredUrl(
            parts = parts,
            tokenEffects = tokenEffects,
            blurEnabled = blurEnabled,
            highlight = highlight,
            muted = muted
        )
        
        // Display as flowing text components
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.testTag("url-inline-flow")
        ) {
            renderInlineUrlComponents(
                parts = parts,
                tokenEffects = tokenEffects,
                blurEnabled = blurEnabled,
                highlight = highlight,
                muted = muted
            )
        }
    }
}

/**
 * Build the complete URL with inline parameters as AnnotatedString
 */
private fun buildInlineBlurredUrl(
    parts: UrlParts,
    tokenEffects: List<TokenEffect>,
    blurEnabled: Boolean,
    highlight: Color,
    muted: Color
): AnnotatedString {
    val tokens: List<QueryToken> = parts.queryPairs.getTokens()
    val effectsByIndex = tokenEffects.associateBy { it.tokenIndex }
    
    return buildAnnotatedString {
        // scheme://
        parts.scheme?.let { 
            pushStyle(SpanStyle(color = muted))
            append("$it://")
            pop()
        }

        // userInfo@
        parts.userInfo?.let { ui ->
            pushStyle(SpanStyle(color = muted))
            append(ui)
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

        // ?query parameters inline
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
                    // value - will be blurred via modifier if needed
                    pushStyle(SpanStyle(color = valueColor))
                    append(tok.decodedValue)
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

/**
 * Render URL components as separate Text elements in FlowRow for blur effects
 */
@Composable
private fun renderInlineUrlComponents(
    parts: UrlParts,
    tokenEffects: List<TokenEffect>,
    blurEnabled: Boolean,
    highlight: Color,
    muted: Color
) {
    val tokens: List<QueryToken> = parts.queryPairs.getTokens()
    val effectsByIndex = tokenEffects.associateBy { it.tokenIndex }
    
    // Scheme
    parts.scheme?.let { scheme ->
        Text(
            text = "$scheme://",
            color = muted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("scheme")
        )
    }
    
    // UserInfo
    parts.userInfo?.let { ui ->
        Text(
            text = "$ui@",
            color = muted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .testTag("userinfo")
                .then(if (blurEnabled) Modifier.blur(4.dp) else Modifier)
        )
    }
    
    // Host
    parts.host?.let { host ->
        Text(
            text = host,
            color = muted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("host")
        )
    }
    
    // Port
    parts.port?.let { port ->
        Text(
            text = ":$port",
            color = muted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("port")
        )
    }
    
    // Path
    parts.path?.let { path ->
        Text(
            text = path,
            color = muted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("path")
        )
    }
    
    // Query parameters
    if (tokens.isNotEmpty()) {
        Text(
            text = "?",
            color = muted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("query-start")
        )
        
        tokens.forEachIndexed { idx, tok ->
            if (idx > 0) {
                Text(
                    text = "&",
                    color = muted,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("query-separator-$idx")
                )
            }
            
            val eff = effectsByIndex[idx]
            val isRemoval = eff?.willBeRemoved == true
            val keyColor = if (isRemoval) highlight else muted
            val valueColor = if (isRemoval) highlight else muted
            
            // Parameter key
            Text(
                text = tok.decodedKey,
                color = keyColor,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("param-key-$idx")
            )
            
            if (tok.hasEquals) {
                Text(
                    text = "=",
                    color = muted,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("param-equals-$idx")
                )
                
                // Parameter value with blur effect
                val shouldBlurValue = blurEnabled && !isRemoval
                Text(
                    text = tok.decodedValue,
                    color = valueColor,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .testTag("param-value-$idx")
                        .then(if (shouldBlurValue) Modifier.blur(4.dp) else Modifier)
                )
            }
        }
    }
    
    // Fragment
    parts.fragment?.let { fragment ->
        Text(
            text = "#",
            color = muted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("fragment-start")
        )
        Text(
            text = fragment,
            color = muted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("fragment")
        )
    }
}
