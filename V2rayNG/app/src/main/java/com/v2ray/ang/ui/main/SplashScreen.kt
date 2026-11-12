package com.v2ray.ang.ui.main

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
private val SplashInk = Color(0xFF1C1E21)

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

    // The splash is always white, so system bar icons must be dark while it is on screen.
    val view = LocalView.current
    DisposableEffect(Unit) {
        var ctx: Context? = view.context
        while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
        val window = (ctx as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevStatus = controller?.isAppearanceLightStatusBars
        val prevNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
        onDispose {
            if (controller != null) {
                if (prevStatus != null) controller.isAppearanceLightStatusBars = prevStatus
                if (prevNav != null) controller.isAppearanceLightNavigationBars = prevNav
            }
        }
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val fontSize = (screenWidthDp * 0.08f).coerceIn(30f, 52f).sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(Color.White)
            .pointerInput(Unit) { detectTapGestures { leaving = true } },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            color = SplashInk
                        )
                    }
                }
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
private fun SplashFlyIn(
    startDelayMs: Long,
    zStart: Float,
    tiltDegrees: Float,
    liftDp: Float,
    spreadIndex: Float,
    spreadUnitDp: Float,
    content: @Composable () -> Unit
) {
    val fade = remember { Animatable(0f) }
    val blur = remember { Animatable(0f) }
    val move = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
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
