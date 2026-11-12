package com.v2ray.ang.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.v2ray.ang.R
import com.v2ray.ang.util.AppIconFetcher
import kotlinx.coroutines.delay

/** Elegant italic serif (Lora Italic, SIL OFL) used for the author name next to the menu. */
val ZakiBrandFont = FontFamily(Font(R.font.lora_italic))

/** Violet that stays readable on both the white and the dark surface. */
val ZakiAccentText: Color @Composable get() = if (LocalDarkTheme.current) Color(0xFFB79CFF) else ZakiPurple

/** Subtle "press in" feedback for anything tappable. */
fun Modifier.zakiClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "zakiPress"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Fade + slide-up entrance. Pass enabled = false to show the content immediately, and start = false
 * to hold it hidden until something else (e.g. the splash screen) is out of the way.
 */
@Composable
fun Modifier.zakiEnter(
    delayMs: Int = 0,
    offsetY: Dp = 18.dp,
    enabled: Boolean = true,
    start: Boolean = true
): Modifier {
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(start) {
        if (enabled && start) {
            delay(delayMs.toLong())
            progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * offsetY.toPx()
    }
}

@Composable
fun ZakiIconButton(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    background: Color = Color.Transparent,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .zakiClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** Minimal top bar for secondary screens. */
@Composable
fun ZakiTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ZakiIconButton(
            iconRes = R.drawable.ic_zaki_back,
            contentDescription = "Back",
            onClick = onBack,
            background = ZakiPurpleSoft,
            tint = ZakiPurple
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        )
        actions()
    }
}

@Composable
fun ZakiCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val borderColor by animateColorAsState(
        if (selected) ZakiPurple else ZakiLine, tween(250), label = "cardBorder"
    )
    val bg by animateColorAsState(
        if (selected) ZakiPurpleSoft else ZakiSurface, tween(250), label = "cardBg"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor), shape)
    ) {
        content()
    }
}

@Composable
fun ZakiSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = ZakiMuted,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(start = 6.dp, bottom = 8.dp, top = 4.dp)
    )
}

@Composable
fun ZakiBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.32f), shape)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

@Composable
fun zakiSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = ZakiPurple,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = if (LocalDarkTheme.current) Color(0xFF3A3354) else Color(0xFFD9D4EA),
    uncheckedBorderColor = Color.Transparent
)

@Composable
fun ZakiSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .zakiClickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            if (!summary.isNullOrEmpty()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = ZakiMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = zakiSwitchColors()
        )
    }
}

/** Tap to edit a text value in a small dialog. */
@Composable
fun ZakiValueRow(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Not set",
    keyboardNumber: Boolean = false,
    isPassword: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }
    val shown = when {
        value.isEmpty() -> placeholder
        isPassword -> "\u2022".repeat(value.length.coerceAtMost(12))
        else -> value
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .zakiClickable { showDialog = true }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = shown,
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isEmpty()) ZakiMuted.copy(alpha = 0.7f) else ZakiPurple,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
    if (showDialog) {
        ZakiTextDialog(
            title = title,
            initial = value,
            keyboardNumber = keyboardNumber,
            isPassword = isPassword,
            onDismiss = { showDialog = false },
            onConfirm = {
                showDialog = false
                onValueChange(it)
            }
        )
    }
}

@Composable
fun ZakiTextDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    keyboardNumber: Boolean = false,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    supportingText: String? = null,
    extraAction: (@Composable (setText: (String) -> Unit) -> Unit)? = null
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZakiSurface,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = singleLine,
                    maxLines = if (singleLine) 1 else 4,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when {
                            keyboardNumber -> KeyboardType.Number
                            isPassword -> KeyboardType.Password
                            else -> KeyboardType.Text
                        }
                    ),
                    visualTransformation = if (isPassword) PasswordVisualTransformation()
                    else VisualTransformation.None,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZakiPurple,
                        unfocusedBorderColor = ZakiLine,
                        cursorColor = ZakiPurple
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!supportingText.isNullOrEmpty()) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZakiMuted,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )
                }
                extraAction?.invoke { text = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) {
                Text("Save", color = ZakiPurple, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = ZakiMuted) }
        }
    )
}

/** Pill-shaped segmented selector. */
@Composable
fun ZakiSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ZakiPurpleSoft)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                if (selected) ZakiPurple else Color.Transparent, tween(250), label = "segBg"
            )
            val fg by animateColorAsState(
                if (selected) Color.White else MaterialTheme.colorScheme.onPrimaryContainer, tween(250), label = "segFg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .zakiClickable { onSelect(index) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = fg,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** App row used by the per-app screen: icon, name, package and a round check. */
@Composable
fun ZakiAppRow(
    appName: String,
    packageName: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val model = remember(packageName) {
        ImageRequest.Builder(context)
            .data("appicon:$packageName")
            .fetcherFactory(AppIconFetcher.Factory(context))
            .build()
    }
    val bg by animateColorAsState(
        if (checked) ZakiPurpleSoft else Color.Transparent, tween(220), label = "appRowBg"
    )
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .zakiClickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit,
            error = painterResource(R.drawable.ic_image_24dp),
            fallback = painterResource(R.drawable.ic_image_24dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = ZakiMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        ZakiCheckDot(checked)
    }
}

@Composable
fun ZakiCheckDot(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val fill by animateColorAsState(
        if (checked) ZakiPurple else Color.Transparent, tween(220), label = "dotFill"
    )
    val stroke by animateColorAsState(
        if (checked) ZakiPurple else MaterialTheme.colorScheme.outline, tween(220), label = "dotStroke"
    )
    val iconScale by animateFloatAsState(
        if (checked) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "dotIcon"
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, stroke, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_zaki_check),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(size * 0.66f)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    alpha = iconScale.coerceIn(0f, 1f)
                }
        )
    }
}

@Composable
fun ZakiLogo(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_v2zaki),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.26f))
    )
}

/** Wide, soft pill button with an icon, used for secondary actions inside dialogs and cards. */
@Composable
fun ZakiActionPill(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ZakiPurpleSoft)
            .border(1.dp, ZakiPurple.copy(alpha = 0.28f), shape)
            .zakiClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = ZakiPurple,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = ZakiPurple,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
