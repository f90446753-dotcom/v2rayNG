package com.v2ray.ang.ui.main

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.material3.MaterialTheme
import com.v2ray.ang.ui.compose.ZakiAccentText
import com.v2ray.ang.ui.compose.ZakiInk
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val SPLASH_NAME = "Hassan Zakizadeh"
private const val HOLD_AFTER_LAST_LETTER_MS = 2500L

private val FadeEasing = CubicBezierEasing(0.3f, 0.6f, 0.4f, 1f)
private val ExpoOut = CubicBezierEasing(0.19f, 1f, 0.22f, 1f)

/**
 * Welcome screen shown once when the app opens: every letter of the name flies in from depth,
 * blurred, and settles on the page; the signature logo does the same underneath. Tap to skip.
 */
@Composable
fun SplashOverlay(onLeaving: () -> Unit, onFinished: () -> Unit) {
    var leaving by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (leaving) 0f else 1f,
        animationSpec = tween(700, easing = LinearOutSlowInEasing),
        label = "splashAlpha",
        finishedListener = { if (leaving) onFinished() }
    )

    // One start time per letter (85-140 ms apart), the first letter starts immediately.
    val starts = remember {
        var t = 0L
        SPLASH_NAME.indices.map { i ->
            if (i > 0) t += 85L + Random.nextInt(55)
            t
        }
    }

    LaunchedEffect(Unit) {
        delay(starts.last() + HOLD_AFTER_LAST_LETTER_MS)
        leaving = true
    }
    LaunchedEffect(leaving) {
        if (leaving) onLeaving()
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val fontSize = (screenWidthDp * 0.08f).coerceIn(30f, 52f).sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { detectTapGestures { leaving = true } },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
            Row {
                SPLASH_NAME.forEachIndexed { index, ch ->
                    SplashFlyIn(
                        startDelayMs = starts[index],
                        zStart = 560f,
                        tiltDegrees = 62f,
                        liftDp = 44f,
                        spreadIndex = index - (SPLASH_NAME.length - 1) / 2f,
                        spreadUnitDp = fontSize.value * 0.52f
                    ) {
                        Text(
                            text = if (ch == ' ') "\u00A0" else ch.toString(),
                            fontSize = fontSize,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Normal,
                            color = ZakiInk
                        )
                    }
                }
            }
            GlowSparkles(
                startMs = starts.last() + HOLD_AFTER_LAST_LETTER_MS - 1000L,
                modifier = Modifier.matchParentSize()
            )
            }
            Spacer(Modifier.height(34.dp))
            // Small signature logo, animated like the text but a beat later.
            SplashFlyIn(
                startDelayMs = 450L,
                zStart = 540f,
                tiltDegrees = 55f,
                liftDp = 40f,
                spreadIndex = 0f,
                spreadUnitDp = 0f
            ) {
                Image(
                    painter = painterResource(R.drawable.splash_signature),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(ZakiAccentText),
                    modifier = Modifier.width(78.dp)
                )
            }
        }
    }
}

/**
 * Letters travel in from depth: scaled up (as if closer to the viewer), tilted, lifted, blurred and
 * transparent, then ease out onto the page (same timings as the original web intro).
 */
@Composable
internal fun SplashFlyIn(
    startDelayMs: Long,
    zStart: Float,
    tiltDegrees: Float,
    liftDp: Float,
    spreadIndex: Float,
    spreadUnitDp: Float,
    start: Boolean = true,
    content: @Composable () -> Unit
) {
    val fade = remember { Animatable(0f) }
    val blur = remember { Animatable(0f) }
    val move = remember { Animatable(0f) }

    LaunchedEffect(start) {
        if (!start) return@LaunchedEffect
        delay(startDelayMs)
        launch { fade.animateTo(1f, tween(1300, easing = FadeEasing)) }
        launch { blur.animateTo(1f, tween(2200, easing = ExpoOut)) }
        launch { move.animateTo(1f, tween(2800, easing = ExpoOut)) }
    }

    val dens = LocalDensity.current.density
    Box(
        modifier = Modifier.graphicsLayer {
            val m = move.value
            val z = zStart * (1f - m)
            val perspective = 800f
            val sc = perspective / (perspective - z)
            alpha = fade.value
            scaleX = sc
            scaleY = sc
            rotationX = tiltDegrees * (1f - m)
            cameraDistance = perspective * dens * 1.5f
            translationY = -liftDp * dens * (1f - m)
            // Letters spread away from the centre of the word while they are "close".
            translationX = spreadIndex * spreadUnitDp * dens * (sc - 1f)
            val b = 22f * dens * (1f - blur.value)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && b > 0.5f) {
                renderEffect = BlurEffect(b, b, TileMode.Decal)
            }
        }
    ) {
        content()
    }
}

private class GlowSpec(val x: Float, val y: Float, val sizeDp: Float, val delayMs: Long)

/**
 * Three or four stars glint beside the name just before the main screen appears. Each one has a
 * bright core, thin rays and a soft halo of light around it.
 */
@Composable
private fun GlowSparkles(startMs: Long, modifier: Modifier = Modifier) {
    val specs = remember {
        listOf(
            GlowSpec(x = 0.09f, y = -0.22f, sizeDp = 9f, delayMs = 0L),
            GlowSpec(x = 0.46f, y = 1.16f, sizeDp = 6.5f, delayMs = 260L),
            GlowSpec(x = 0.79f, y = -0.20f, sizeDp = 8f, delayMs = 520L),
            GlowSpec(x = 1.03f, y = 0.42f, sizeDp = 5.5f, delayMs = 780L)
        )
    }
    val progress = remember { List(specs.size) { Animatable(0f) } }

    LaunchedEffect(Unit) {
        specs.forEachIndexed { i, spec ->
            launch {
                delay(startMs + spec.delayMs)
                // Natural twinkle: swell, dip, flare again, then fade away.
                progress[i].animateTo(1f, tween(480, easing = FastOutSlowInEasing))
                progress[i].animateTo(0.45f, tween(380, easing = FastOutSlowInEasing))
                progress[i].animateTo(1f, tween(380, easing = FastOutSlowInEasing))
                progress[i].animateTo(0f, tween(650, easing = FastOutSlowInEasing))
            }
        }
    }

    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val core = if (dark) Color.White else Color(0xFF7C3AED)
    val halo = if (dark) Color(0xFFB79CFF) else Color(0xFF9F7BFF)

    Canvas(modifier) {
        specs.forEachIndexed { i, spec ->
            val p = progress[i].value
            if (p > 0.01f) {
                val center = Offset(size.width * spec.x, size.height * spec.y)
                val r = spec.sizeDp.dp.toPx() * (0.55f + 0.45f * p)

                // Soft halo of light
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            halo.copy(alpha = 0.55f * p),
                            halo.copy(alpha = 0.18f * p),
                            Color.Transparent
                        ),
                        center = center,
                        radius = r * 3.4f
                    ),
                    radius = r * 3.4f,
                    center = center
                )

                rotate(degrees = 16f * p, pivot = center) {
                    fun ray(length: Float, alpha: Float) {
                        val star = Path().apply {
                            moveTo(center.x, center.y - length)
                            quadraticBezierTo(center.x, center.y, center.x + length, center.y)
                            quadraticBezierTo(center.x, center.y, center.x, center.y + length)
                            quadraticBezierTo(center.x, center.y, center.x - length, center.y)
                            quadraticBezierTo(center.x, center.y, center.x, center.y - length)
                            close()
                        }
                        drawPath(star, color = core.copy(alpha = alpha * p))
                    }
                    ray(r * 1.15f, 1f)
                    rotate(degrees = 45f, pivot = center) { ray(r * 0.62f, 0.7f) }
                }
                // Bright heart of the star
                drawCircle(color = core.copy(alpha = p), radius = r * 0.17f, center = center)
            }
        }
    }
}
