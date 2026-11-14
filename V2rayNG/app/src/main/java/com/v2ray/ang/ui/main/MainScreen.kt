package com.v2ray.ang.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import com.v2ray.ang.ui.compose.ZakiBrandFontBold
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.v2ray.ang.ui.compose.GlassConnectButton
import com.v2ray.ang.ui.compose.GlassRoundButton
import com.v2ray.ang.ui.compose.GlassStatusPill
import com.v2ray.ang.ui.compose.glass
import com.v2ray.ang.ui.compose.glassMeshBackground
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.ZakiAccentText
import com.v2ray.ang.ui.compose.ZakiActionPill
import com.v2ray.ang.ui.compose.ZakiBrandFont
import com.v2ray.ang.ui.compose.ZakiIconButton
import com.v2ray.ang.ui.compose.ZakiMuted
import com.v2ray.ang.ui.compose.ZakiPurple
import com.v2ray.ang.ui.compose.ZakiPurpleSoft
import com.v2ray.ang.ui.compose.ZakiTextDialog
import com.v2ray.ang.ui.compose.zakiClickable
import com.v2ray.ang.ui.compose.zakiEnter
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.launch

/**
 * V2Zaki main screen: a quiet list of configs, a menu button at the top left and one row of
 * controls at the bottom (add subscription link, connect / disconnect, real delay).
 */
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
    contentVisible: Boolean = true,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSubDialog by rememberSaveable { mutableStateOf(false) }
    var controlsHeightPx by remember { mutableIntStateOf(0) }

    // Only one subscription exists; its configs are the whole list.
    val groups = uiState.groups
    val groupId = (groups.firstOrNull { it.id.isNotEmpty() && it.id == uiState.selectedGroupId }
        ?: groups.firstOrNull { it.id.isNotEmpty() })?.id
    LaunchedEffect(groupId) {
        if (groupId != null && groupId != uiState.selectedGroupId) {
            onAction(MainAction.SelectGroup(groupId))
        }
    }

    val caption = when (val status = uiState.status) {
        MainStatus.Disconnected -> "Not connected"
        MainStatus.Connected -> "Connected"
        MainStatus.Testing -> "Testing delay\u2026"
        is MainStatus.TestProgress -> "Testing \u00B7 ${status.progress} left"
        is MainStatus.ConnectionTest ->
            if (status.result.delayMillis >= 0) "Connected \u00B7 ${status.result.delayMillis} ms"
            else "Connected \u00B7 delay test failed"
    }

    val backdrop = rememberLayerBackdrop()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // The design is English / left-to-right in every language, so the menu stays at the top left.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !showSubDialog,
            drawerContent = {
                MainDrawerContent(
                    drawerState = drawerState,
                    backdrop = backdrop,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigate(route)
                    }
                )
            }
        ) {
            Box(Modifier.fillMaxSize()) {
                // Everything the glass can see lives in this box: the ambient background and the list.
                Box(
                    Modifier
                        .fillMaxSize()
                        .layerBackdrop(backdrop)
                        .glassMeshBackground()
                ) {
                    if (groupId != null) {
                        ConfigListHost(
                            groupId = groupId,
                            mainViewModel = mainViewModel,
                            selectedGuid = uiState.selectedGuid,
                            topPadding = topInset + 56.dp + 22.dp,
                            bottomPadding = with(LocalDensity.current) { controlsHeightPx.toDp() } + 12.dp,
                            contentVisible = contentVisible,
                            onSelect = { guid -> onAction(MainAction.SelectServer(guid)) },
                            onDetails = { guid -> onAction(MainAction.ShowConfigDetails(guid)) }
                        )
                    } else {
                        EmptyHint(contentVisible = contentVisible)
                    }
                }

                MainHeader(
                    backdrop = backdrop,
                    isLoading = isLoading,
                    contentVisible = contentVisible,
                    onMenu = { scope.launch { drawerState.open() } },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                MainControls(
                    backdrop = backdrop,
                    isRunning = uiState.isRunning,
                    isTesting = uiState.isTesting,
                    caption = caption,
                    contentVisible = contentVisible,
                    onAddSubscription = { showSubDialog = true },
                    onToggle = { onAction(MainAction.ToggleService) },
                    onRealDelay = {
                        if (uiState.isTesting) onAction(MainAction.CancelTesting)
                        else onAction(MainAction.TestRealAllServers)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { controlsHeightPx = it.height }
                )

                val currentUrl = remember(groupId, showSubDialog) {
                    if (showSubDialog) {
                        mainViewModel.getSubscriptions()
                            .firstOrNull { it.guid == groupId }?.subscription?.url.orEmpty()
                    } else ""
                }
                SubscriptionSheet(
                    visible = showSubDialog,
                    backdrop = backdrop,
                    initialUrl = currentUrl,
                    hasCurrent = currentUrl.isNotEmpty(),
                    onDismiss = { showSubDialog = false },
                    onSave = { url ->
                        showSubDialog = false
                        if (url.isNotBlank()) onAction(MainAction.SetSubscription(url))
                    },
                    onUpdate = {
                        showSubDialog = false
                        onAction(MainAction.UpdateSubscriptions)
                    }
                )
            }
        }
    }

}

@Composable
private fun MainHeader(
    backdrop: Backdrop,
    isLoading: Boolean,
    contentVisible: Boolean,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .zakiEnter(delayMs = 0, start = contentVisible)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .glass(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(28.dp),
                    blurRadius = 3.dp,
                    refractionHeight = 24.dp,
                    refractionAmount = 48.dp,
                    liquid = true
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZakiIconButton(
                iconRes = R.drawable.ic_zaki_menu,
                contentDescription = "Open menu",
                onClick = onMenu,
                tint = ZakiAccentText,
                size = 46.dp,
                modifier = Modifier.padding(start = 3.dp)
            )
            BrandName(start = contentVisible, modifier = Modifier.offset(x = (-8).dp))
        }
        AnimatedVisibility(visible = isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .padding(top = 8.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth()
                    .height(2.dp),
                color = ZakiPurple,
                trackColor = Color.Transparent
            )
        }
    }
}

/** Author name in handwriting; its letters fly in like the splash screen, then a few tiny stars glint on it. */
@Composable
private fun BrandName(start: Boolean, modifier: Modifier = Modifier) {
    val name = "Hassan Zakizadeh"
    val fontSize = 13.sp
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            name.forEachIndexed { index, ch ->
                SplashFlyIn(
                    startDelayMs = 200L + index * 65L,
                    zStart = 560f,
                    tiltDegrees = 62f,
                    liftDp = 16f,
                    spreadIndex = index - (name.length - 1) / 2f,
                    spreadUnitDp = fontSize.value * 0.5f,
                    start = start
                ) {
                    Text(
                        text = if (ch == ' ') "\u00A0" else ch.toString(),
                        fontFamily = ZakiBrandFontBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        // Matte, softly blurred ink (the blur is a no-op below Android 12).
                        color = ZakiAccentText.copy(alpha = 0.85f),
                        modifier = Modifier.blur(0.7.dp, BlurredEdgeTreatment.Unbounded)
                    )
                }
            }
        }
        SparkleOverlay(start = start, modifier = Modifier.matchParentSize())
    }
}

private class SparkleSpec(val x: Float, val y: Float, val sizeDp: Float, val delayMs: Long)

/**
 * Many tiny four-point stars twinkle over the name while it animates in. A big burst appears at
 * the start and the number of new stars then falls off along a parabola until the last one fades.
 */
@Composable
private fun SparkleOverlay(start: Boolean, modifier: Modifier = Modifier) {
    val specs = remember {
        val rnd = kotlin.random.Random(23)
        val count = 7
        val spanMs = 3600f
        List(count) { i ->
            // Evenly spread quantiles of a linearly falling density: remaining stars ~ (1 - t)^2.
            val u = (i + 0.5f) / count
            val t = 1f - kotlin.math.sqrt(1f - u)
            SparkleSpec(
                x = rnd.nextFloat(),
                y = -0.35f + 1.7f * rnd.nextFloat(),
                sizeDp = 1.8f + 2.4f * rnd.nextFloat(),
                delayMs = 250L + (t * spanMs).toLong() + rnd.nextInt(70)
            )
        }
    }
    val progress = remember { List(specs.size) { Animatable(0f) } }

    LaunchedEffect(start) {
        if (!start) return@LaunchedEffect
        specs.forEachIndexed { i, spec ->
            launch {
                delay(spec.delayMs)
                progress[i].animateTo(1f, tween(320, easing = FastOutSlowInEasing))
                progress[i].animateTo(0f, tween(520, easing = FastOutSlowInEasing))
            }
        }
    }

    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val color = if (dark) Color.White else Color(0xFFA78BFA)

    Canvas(modifier) {
        specs.forEachIndexed { i, spec ->
            val p = progress[i].value
            if (p > 0.01f) {
                val cx = size.width * spec.x
                val cy = size.height * spec.y
                val r = spec.sizeDp.dp.toPx() * (0.45f + 0.55f * p)
                drawCircle(color = color.copy(alpha = 0.22f * p), radius = r * 1.15f, center = Offset(cx, cy))
                val star = Path().apply {
                    moveTo(cx, cy - r)
                    quadraticBezierTo(cx, cy, cx + r, cy)
                    quadraticBezierTo(cx, cy, cx, cy + r)
                    quadraticBezierTo(cx, cy, cx - r, cy)
                    quadraticBezierTo(cx, cy, cx, cy - r)
                    close()
                }
                drawPath(star, color = color.copy(alpha = p))
            }
        }
    }
}

@Composable
private fun EmptyHint(contentVisible: Boolean) {
    JavidEmptyState(contentVisible = contentVisible)
}

@Composable
private fun MainControls(
    backdrop: Backdrop,
    isRunning: Boolean,
    isTesting: Boolean,
    caption: String,
    contentVisible: Boolean,
    onAddSubscription: () -> Unit,
    onToggle: () -> Unit,
    onRealDelay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The pulse only exists while a test runs, so an idle screen stays idle.
    val pulse: State<Float> = if (isTesting) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.82f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulseValue"
        )
    } else {
        remember { mutableStateOf(1f) }
    }
    val testTint by animateColorAsState(
        if (isTesting) ZakiPurple else ZakiPurple.copy(alpha = 0f), tween(300), label = "testTint"
    )
    val testIcon by animateColorAsState(
        if (isTesting) Color.White else ZakiAccentText, tween(300), label = "testIcon"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 18.dp)
            .zakiEnter(delayMs = 120, offsetY = 40.dp, start = contentVisible),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassStatusPill(backdrop = backdrop, text = caption, accent = isRunning)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassRoundButton(
                backdrop = backdrop,
                iconRes = R.drawable.ic_zaki_link,
                description = "Add subscription link",
                onClick = onAddSubscription
            )
            Spacer(Modifier.width(14.dp))
            GlassConnectButton(
                backdrop = backdrop,
                isRunning = isRunning,
                onClick = onToggle,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(14.dp))
            GlassRoundButton(
                backdrop = backdrop,
                iconRes = R.drawable.ic_zaki_bolt,
                description = "Real delay",
                onClick = onRealDelay,
                tint = testTint,
                iconTint = testIcon,
                iconScale = { if (isTesting) pulse.value else 1f }
            )
        }
    }
}
