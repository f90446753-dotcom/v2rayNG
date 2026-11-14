package com.v2ray.ang.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.v2ray.ang.R

/**
 * Soft violet / pink / blue glows. Glass only looks like glass when there is something colourful
 * behind it to blur and bend, so every screen sits on this ambient background.
 */
@Composable
fun Modifier.glassMeshBackground(): Modifier {
    val dark = LocalDarkTheme.current
    val base = if (dark) Color(0xFF0B0814) else Color(0xFFF6F3FF)
    val a = if (dark) Color(0xFF6D28D9) else Color(0xFFA78BFA)
    val b = if (dark) Color(0xFFA21CAF) else Color(0xFFF0ABFC)
    val c = if (dark) Color(0xFF4338CA) else Color(0xFF93C5FD)
    val aAlpha = if (dark) 0.50f else 0.50f
    val bAlpha = if (dark) 0.28f else 0.42f
    val cAlpha = if (dark) 0.38f else 0.40f
    return this.drawBehind {
        drawRect(base)
        val w = size.width
        val h = size.height
        fun blob(color: Color, alpha: Float, cx: Float, cy: Float, radius: Float) {
            val center = Offset(cx, cy)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
        blob(a, aAlpha, w * 0.15f, h * 0.12f, w * 0.90f)
        blob(b, bAlpha, w * 0.98f, h * 0.46f, w * 0.80f)
        blob(c, cAlpha, w * 0.08f, h * 0.86f, w * 0.95f)
    }
}

/**
 * Real liquid glass: blurs, saturates and bends whatever [backdrop] holds behind this element,
 * then adds a rim highlight and a soft shadow. Needs a [CornerBasedShape][androidx.compose.foundation.shape.CornerBasedShape]
 * (rounded corners). On Android 12 only the blur is applied; below that it is a plain translucent surface.
 */
@Composable
fun Modifier.glass(
    backdrop: Backdrop,
    shape: Shape,
    tint: Color = Color.Unspecified,
    blurRadius: Dp = 6.dp,
    refraction: Boolean = true,
    refractionHeight: Dp = 10.dp,
    refractionAmount: Dp = 20.dp,
    /** Clearer, more liquid look: wider light bending, colour fringes, brighter rim, deeper shadow. */
    liquid: Boolean = false,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null
): Modifier {
    val dark = LocalDarkTheme.current
    val surface = when {
        liquid && dark -> Color.White.copy(alpha = 0.035f)
        liquid -> Color.White.copy(alpha = 0.14f)
        dark -> Color.White.copy(alpha = 0.07f)
        else -> Color.White.copy(alpha = 0.36f)
    }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurRadius.toPx())
            if (refraction) {
                lens(
                    refractionHeight.toPx(),
                    refractionAmount.toPx(),
                    depthEffect = liquid,
                    chromaticAberration = liquid
                )
            }
        },
        highlight = {
            if (liquid) {
                Highlight(width = 1.6f.dp)
            } else {
                Highlight.Default
            }
        },
        shadow = {
            if (liquid) {
                Shadow(
                    radius = 22f.dp,
                    color = Color.Black.copy(alpha = if (dark) 0.35f else 0.16f)
                )
            } else {
                Shadow.Default
            }
        },
        layerBlock = layerBlock,
        onDrawSurface = {
            if (tint.isSpecified && tint.alpha > 0f) {
                drawRect(tint, blendMode = BlendMode.Hue)
                drawRect(tint.copy(alpha = 0.75f * tint.alpha))
            }
            drawRect(surface)
        }
    )
}

@Composable
private fun rememberGlassPress(): Pair<MutableInteractionSource, State<Float>> {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "glassPress"
    )
    return source to scale
}

/** Round glass button with an icon. */
@Composable
fun GlassRoundButton(
    backdrop: Backdrop,
    iconRes: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    iconSize: Dp = 22.dp,
    tint: Color = Color.Unspecified,
    iconTint: Color = ZakiAccentText,
    iconScale: () -> Float = { 1f }
) {
    val (source, pressScale) = rememberGlassPress()
    Box(
        modifier = modifier
            .size(size)
            .glass(
                backdrop = backdrop,
                shape = CircleShape,
                tint = tint,
                blurRadius = 2.dp,
                refractionHeight = 22.dp,
                refractionAmount = 44.dp,
                liquid = true,
                layerBlock = {
                    scaleX = pressScale.value
                    scaleY = pressScale.value
                }
            )
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = iconTint,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    val s = iconScale()
                    scaleX = s
                    scaleY = s
                }
        )
    }
}

/** The big connect / disconnect glass capsule. Both labels are always laid out so nothing jumps. */
@Composable
fun GlassConnectButton(
    backdrop: Backdrop,
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val runningProgress by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "connectProgress"
    )
    val idleInk = MaterialTheme.colorScheme.onSurface
    val activeInk = ZakiAccentText
    val fg by animateColorAsState(
        targetValue = if (isRunning) activeInk else idleInk,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "connectFg"
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (isRunning) 180f else 0f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "connectIcon"
    )
    val (source, pressScale) = rememberGlassPress()

    Box(
        modifier = modifier
            .height(58.dp)
            .glass(
                backdrop = backdrop,
                shape = RoundedCornerShape(50),
                blurRadius = 2.dp,
                refractionHeight = 27.dp,
                refractionAmount = 56.dp,
                liquid = true,
                layerBlock = {
                    scaleX = pressScale.value
                    scaleY = pressScale.value
                }
            )
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_zaki_power),
                contentDescription = null,
                tint = fg,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = iconRotation }
            )
            Spacer(Modifier.width(10.dp))
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Connect",
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer {
                        alpha = 1f - runningProgress
                        translationY = -8.dp.toPx() * runningProgress
                    }
                )
                Text(
                    text = "Disconnect",
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer {
                        alpha = runningProgress
                        translationY = 8.dp.toPx() * (1f - runningProgress)
                    }
                )
            }
        }
        // Status dot sits at the end so it never pushes the label around.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .size(7.dp)
                .graphicsLayer { alpha = runningProgress }
                .background(ZakiPurple, CircleShape)
        )
    }
}

/** Small glass capsule with a status text that cross-fades and resizes smoothly. */
@Composable
fun GlassStatusPill(backdrop: Backdrop, text: String, accent: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .glass(
                backdrop = backdrop,
                shape = RoundedCornerShape(50),
                blurRadius = 4.dp,
                refractionHeight = 12.dp,
                refractionAmount = 24.dp,
                liquid = true
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) },
            contentAlignment = Alignment.Center,
            label = "statusText"
        ) { value ->
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (accent) ZakiAccentText else ZakiMuted,
                maxLines = 1
            )
        }
    }
}

/**
 * Frosted card without a per-card shader: translucent fill, a bright rim and a soft top sheen.
 * It sits on the ambient background, which is where the glass look comes from.
 */
@Composable
fun GlassCardSurface(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    shape: Shape = RoundedCornerShape(22.dp),
    content: @Composable () -> Unit
) {
    val dark = LocalDarkTheme.current
    val fill by animateColorAsState(
        targetValue = when {
            selected -> ZakiPurple.copy(alpha = if (dark) 0.24f else 0.16f)
            dark -> Color.White.copy(alpha = 0.06f)
            else -> Color.White.copy(alpha = 0.62f)
        },
        animationSpec = tween(250),
        label = "glassCardFill"
    )
    val rim = if (selected) {
        SolidColor(ZakiPurple.copy(alpha = 0.85f))
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = if (dark) 0.30f else 0.95f),
                Color.White.copy(alpha = if (dark) 0.05f else 0.30f),
                Color.White.copy(alpha = if (dark) 0.14f else 0.60f)
            )
        )
    }
    val sheen = Color.White.copy(alpha = if (dark) 0.07f else 0.34f)
    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(sheen, Color.Transparent),
                        endY = size.height * 0.55f
                    )
                )
            }
            .border(if (selected) 1.5.dp else 1.dp, rim, shape)
    ) {
        content()
    }
}
