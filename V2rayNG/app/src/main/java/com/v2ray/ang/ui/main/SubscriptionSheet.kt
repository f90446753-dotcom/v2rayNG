package com.v2ray.ang.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.LocalDarkTheme
import com.v2ray.ang.ui.compose.ZakiActionPill
import com.v2ray.ang.ui.compose.ZakiMuted
import com.v2ray.ang.ui.compose.ZakiPurple
import com.v2ray.ang.ui.compose.ZakiPurpleSoft
import com.v2ray.ang.ui.compose.glass
import com.v2ray.ang.ui.compose.zakiClickable
import com.v2ray.ang.util.Utils

/**
 * The "subscription link" popup, drawn inside the main screen (not in a separate dialog window)
 * so it can use the same backdrop as the other glass parts. It sits above the keyboard: the
 * content is padded by the IME insets, and it scrolls if the screen is too short.
 */
@Composable
internal fun SubscriptionSheet(
    visible: Boolean,
    backdrop: Backdrop,
    initialUrl: String,
    hasCurrent: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onUpdate: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val close: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
        currentOnDismiss()
    }
    val currentClose by rememberUpdatedState(close)

    // Back closes the popup first (and hides the keyboard) instead of leaving the app.
    BackHandler(enabled = visible) { currentClose() }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(160))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .pointerInput(Unit) { detectTapGestures { currentClose() } }
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            SheetCard(
                backdrop = backdrop,
                initialUrl = initialUrl,
                hasCurrent = hasCurrent,
                onCancel = { currentClose() },
                onSave = { url ->
                    focusManager.clearFocus()
                    keyboard?.hide()
                    onSave(url)
                },
                onUpdate = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                    onUpdate()
                }
            )
        }
    }
}

@Composable
private fun SheetCard(
    backdrop: Backdrop,
    initialUrl: String,
    hasCurrent: Boolean,
    onCancel: () -> Unit,
    onSave: (String) -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    val dark = LocalDarkTheme.current
    var text by remember { mutableStateOf(initialUrl) }
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(340, easing = FastOutSlowInEasing)) }

    val veil = if (dark) Color(0xFF0F0C18).copy(alpha = 0.42f) else Color.White.copy(alpha = 0.45f)
    val trimmed = text.trim()
    val looksValid = trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    val canSave = trimmed.isNotEmpty() && looksValid

    Box(
        modifier = Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            // Taps inside the card must not reach the dismiss layer behind it.
            .pointerInput(Unit) { detectTapGestures { } }
            .glass(
                backdrop = backdrop,
                shape = RoundedCornerShape(32.dp),
                blurRadius = 14.dp,
                refractionHeight = 18.dp,
                refractionAmount = 34.dp,
                liquid = true,
                veil = veil,
                layerBlock = {
                    val s = 0.92f + 0.08f * enter.value
                    scaleX = s
                    scaleY = s
                }
            )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Text(
                text = "Subscription link",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                isError = trimmed.isNotEmpty() && !looksValid,
                placeholder = { Text("https://", color = ZakiMuted) },
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { if (canSave) onSave(trimmed) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZakiPurple,
                    unfocusedBorderColor = Color.White.copy(alpha = if (dark) 0.22f else 0.7f),
                    focusedContainerColor = Color.White.copy(alpha = if (dark) 0.06f else 0.35f),
                    unfocusedContainerColor = Color.White.copy(alpha = if (dark) 0.04f else 0.25f),
                    cursorColor = ZakiPurple
                )
            )
            Text(
                text = if (trimmed.isNotEmpty() && !looksValid) "The link must start with http:// or https://"
                else "Saving it replaces the current subscription.",
                style = MaterialTheme.typography.bodySmall,
                color = if (trimmed.isNotEmpty() && !looksValid) MaterialTheme.colorScheme.error else ZakiMuted,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )
            TextButton(onClick = { text = Utils.getClipboard(context).trim() }) {
                Text("Paste from clipboard", color = ZakiPurple)
            }
            if (hasCurrent) {
                Spacer(Modifier.height(4.dp))
                ZakiActionPill(
                    iconRes = R.drawable.ic_zaki_refresh,
                    label = "Update current subscription",
                    onClick = onUpdate
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SheetButton(
                    label = "Cancel",
                    primary = false,
                    enabled = true,
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                )
                SheetButton(
                    label = "Save",
                    primary = true,
                    enabled = canSave,
                    onClick = { onSave(trimmed) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SheetButton(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalDarkTheme.current
    val shape = RoundedCornerShape(50)
    val fill = when {
        primary -> ZakiPurple.copy(alpha = if (enabled) 0.92f else 0.35f)
        dark -> Color.White.copy(alpha = 0.08f)
        else -> Color.White.copy(alpha = 0.55f)
    }
    val rim = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (dark) 0.35f else 0.95f),
            Color.White.copy(alpha = if (dark) 0.08f else 0.35f)
        )
    )
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, rim, shape)
            .zakiClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}
