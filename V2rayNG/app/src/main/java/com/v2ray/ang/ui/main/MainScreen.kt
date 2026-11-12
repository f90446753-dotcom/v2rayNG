package com.v2ray.ang.ui.main

import androidx.compose.animation.AnimatedVisibility
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
import com.v2ray.ang.ui.compose.ZakiHandwriting
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

    // The design is English / left-to-right in every language, so the menu stays at the top left.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                MainDrawerContent(
                    drawerState = drawerState,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        onNavigate(route)
                    }
                )
            }
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(Modifier.fillMaxSize()) {
                    MainHeader(
                        isLoading = isLoading,
                        contentVisible = contentVisible,
                        onMenu = { scope.launch { drawerState.open() } }
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (groupId != null) {
                            ConfigListHost(
                                groupId = groupId,
                                mainViewModel = mainViewModel,
                                selectedGuid = uiState.selectedGuid,
                                bottomPadding = with(LocalDensity.current) { controlsHeightPx.toDp() } + 12.dp,
                                contentVisible = contentVisible,
                                onSelect = { guid -> onAction(MainAction.SelectServer(guid)) },
                                onDetails = { guid -> onAction(MainAction.ShowConfigDetails(guid)) }
                            )
                        } else {
                            EmptyHint(contentVisible = contentVisible)
                        }
                    }
                }

                MainControls(
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
            }
        }
    }

    if (showSubDialog) {
        val context = LocalContext.current
        val currentUrl = remember(groupId) {
            mainViewModel.getSubscriptions()
                .firstOrNull { it.guid == groupId }?.subscription?.url.orEmpty()
        }
        ZakiTextDialog(
            title = "Subscription link",
            initial = currentUrl,
            singleLine = false,
            supportingText = "Paste your subscription link. Saving it replaces the current one.",
            onDismiss = { showSubDialog = false },
            onConfirm = { url ->
                showSubDialog = false
                if (url.isNotBlank()) onAction(MainAction.SetSubscription(url))
            },
            extraAction = { setText ->
                Column {
                    TextButton(onClick = { setText(Utils.getClipboard(context)) }) {
                        Text("Paste from clipboard", color = ZakiPurple)
                    }
                    if (currentUrl.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        ZakiActionPill(
                            iconRes = R.drawable.ic_zaki_refresh,
                            label = "Update current subscription",
                            onClick = {
                                showSubDialog = false
                                onAction(MainAction.UpdateSubscriptions)
                            }
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun MainHeader(isLoading: Boolean, contentVisible: Boolean, onMenu: () -> Unit) {
    Column(Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZakiIconButton(
                iconRes = R.drawable.ic_zaki_menu,
                contentDescription = "Open menu",
                onClick = onMenu,
                background = ZakiPurpleSoft,
                tint = ZakiPurple,
                size = 46.dp,
                modifier = Modifier.zakiEnter(delayMs = 0, start = contentVisible)
            )
            Spacer(Modifier.width(12.dp))
            BrandName(start = contentVisible)
        }
        AnimatedVisibility(visible = isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = ZakiPurple,
                trackColor = Color.Transparent
            )
        }
    }
}

/** Author name in handwriting; its letters fly in like the splash screen when the app opens. */
@Composable
private fun BrandName(start: Boolean) {
    val name = "Hassan Zakizadeh"
    val fontSize = 24.sp
    Row(verticalAlignment = Alignment.CenterVertically) {
        name.forEachIndexed { index, ch ->
            SplashFlyIn(
                startDelayMs = 200L + index * 65L,
                zStart = 560f,
                tiltDegrees = 62f,
                liftDp = 30f,
                spreadIndex = index - (name.length - 1) / 2f,
                spreadUnitDp = fontSize.value * 0.5f,
                start = start
            ) {
                Text(
                    text = if (ch == ' ') "\u00A0" else ch.toString(),
                    fontFamily = ZakiHandwriting,
                    fontSize = fontSize,
                    color = ZakiAccentText
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(contentVisible: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.zakiEnter(delayMs = 150, start = contentVisible)
        ) {
            com.v2ray.ang.ui.compose.ZakiLogo(size = 72.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Add your subscription",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tap the link button below and paste your subscription link. Your configs will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = ZakiMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun MainControls(
    isRunning: Boolean,
    isTesting: Boolean,
    caption: String,
    contentVisible: Boolean,
    onAddSubscription: () -> Unit,
    onToggle: () -> Unit,
    onRealDelay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageColor = MaterialTheme.colorScheme.background
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, pageColor.copy(alpha = 0.94f), pageColor)
                )
            )
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 18.dp)
            .zakiEnter(delayMs = 120, offsetY = 40.dp, start = contentVisible),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = caption,
                animationSpec = tween(300),
                modifier = Modifier.fillMaxWidth(),
                label = "caption"
            ) { text ->
                Text(
                    text = text,
                    fontSize = 13.sp,
                    color = if (isRunning) ZakiPurple else ZakiMuted,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundAction(
                iconRes = R.drawable.ic_zaki_link,
                description = "Add subscription link",
                onClick = onAddSubscription
            )
            Spacer(Modifier.width(14.dp))
            ConnectButton(isRunning = isRunning, onClick = onToggle, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(14.dp))
            RoundAction(
                iconRes = R.drawable.ic_zaki_bolt,
                description = "Real delay",
                onClick = onRealDelay,
                pulsing = isTesting
            )
        }
    }
}

@Composable
private fun RoundAction(
    iconRes: Int,
    description: String,
    onClick: () -> Unit,
    pulsing: Boolean = false,
) {
    // The infinite animation only exists while a test is running, so an idle screen stays idle.
    val pulse: State<Float> = if (pulsing) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.82f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulseValue"
        )
    } else {
        remember { mutableStateOf(1f) }
    }
    val bg by animateColorAsState(
        if (pulsing) ZakiPurple else ZakiPurpleSoft, tween(300), label = "roundBg"
    )
    val tint by animateColorAsState(
        if (pulsing) Color.White else ZakiPurple, tween(300), label = "roundTint"
    )
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(bg)
            .zakiClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = tint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    val s = if (pulsing) pulse.value else 1f
                    scaleX = s
                    scaleY = s
                }
        )
    }
}

@Composable
private fun ConnectButton(isRunning: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(29.dp)
    val bg by animateColorAsState(
        if (isRunning) ZakiPurpleSoft else ZakiPurple, tween(420, easing = FastOutSlowInEasing), label = "connectBg"
    )
    val fg by animateColorAsState(
        if (isRunning) ZakiPurple else Color.White, tween(420, easing = FastOutSlowInEasing), label = "connectFg"
    )
    val borderColor by animateColorAsState(
        if (isRunning) ZakiPurple else Color.Transparent, tween(420, easing = FastOutSlowInEasing), label = "connectBorder"
    )
    val iconRotation by animateFloatAsState(
        if (isRunning) 180f else 0f, tween(520, easing = FastOutSlowInEasing), label = "connectIcon"
    )
    // Both labels are always laid out (so the button never changes its layout); only their
    // opacity and a small vertical slide are animated.
    val runningProgress by animateFloatAsState(
        if (isRunning) 1f else 0f, tween(380, easing = FastOutSlowInEasing), label = "connectLabel"
    )
    val dotPulse: State<Float> = if (isRunning) {
        rememberInfiniteTransition(label = "dot").animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
            label = "dotAlpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Box(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(bg)
            .border(1.5.dp, borderColor, shape)
            .zakiClickable(onClick = onClick),
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
        // Status dot sits at the end of the button, so it never pushes the label around.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .size(7.dp)
                .graphicsLayer { alpha = runningProgress * dotPulse.value }
                .background(ZakiPurple, CircleShape)
        )
    }
}
