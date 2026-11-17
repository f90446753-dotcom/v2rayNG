package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R

// ---------- Windows 95 palette ----------
private val W95Face = Color(0xFFC0C0C0)
private val W95Light = Color(0xFFDFDFDF)
private val W95White = Color(0xFFFFFFFF)
private val W95Shadow = Color(0xFF808080)
private val W95Dark = Color(0xFF000000)
private val W95Navy = Color(0xFF000080)

private val W95Font = FontFamily(Font(R.font.ms_sans_serif))
private val W95Text = TextStyle(fontFamily = W95Font, fontSize = 16.sp, color = W95Dark)

// ---------- Bevel drawing (no images, no shadows, no animation) ----------
private fun DrawScope.drawFrame(topLeft: Color, bottomRight: Color, inset: Float, t: Float) {
    val w = size.width
    val h = size.height
    drawRect(topLeft, Offset(inset, inset), Size(w - 2 * inset, t))
    drawRect(topLeft, Offset(inset, inset), Size(t, h - 2 * inset))
    drawRect(bottomRight, Offset(inset, h - inset - t), Size(w - 2 * inset, t))
    drawRect(bottomRight, Offset(w - inset - t, inset), Size(t, h - 2 * inset))
}

private fun Modifier.raisedBevel(): Modifier = drawBehind {
    val t = 1.dp.toPx().coerceAtLeast(1f)
    drawRect(W95Face)
    drawFrame(W95White, W95Dark, 0f, t)
    drawFrame(W95Light, W95Shadow, t, t)
}

private fun Modifier.pressedBevel(): Modifier = drawBehind {
    val t = 1.dp.toPx().coerceAtLeast(1f)
    drawRect(W95Face)
    drawFrame(W95Dark, W95Dark, 0f, t)
    drawFrame(W95Shadow, W95Shadow, t, t)
}

private fun Modifier.sunkenBevel(fill: Color): Modifier = drawBehind {
    val t = 1.dp.toPx().coerceAtLeast(1f)
    drawRect(fill)
    drawFrame(W95Shadow, W95White, 0f, t)
    drawFrame(W95Dark, W95Light, t, t)
}

@Composable
private fun W95Label(text: String, modifier: Modifier = Modifier) {
    BasicText(text = text, style = W95Text, modifier = modifier)
}

@Composable
private fun W95Button(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .then(if (pressed) Modifier.pressedBevel() else Modifier.raisedBevel())
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = W95Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offset(
                x = if (pressed) 1.dp else 0.dp,
                y = if (pressed) 1.dp else 0.dp,
            ),
        )
    }
}

/**
 * The whole app: subscription link field, config list, status and the connect button.
 * Everything else (settings, search, menus, drawers, dialogs) is intentionally absent.
 */
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val groups = uiState.groups
    val isRunning = uiState.isRunning
    val selectedGuid = uiState.selectedGuid
    val statusText = mainViewModel.formatStatus(uiState.status)

    val currentGroup = groups.firstOrNull { it.id == uiState.selectedGroupId } ?: groups.firstOrNull()
    val groupId = currentGroup?.id.orEmpty()
    val serversFlow = remember(groupId) { mainViewModel.serversForGroup(groupId) }
    val servers by serversFlow.collectAsStateWithLifecycle()

    var url by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val submit: () -> Unit = {
        val link = url.trim()
        if (link.isNotEmpty()) {
            focusManager.clearFocus()
            val existing = mainViewModel.getSubscriptions().firstOrNull { it.subscription.url == link }
            if (existing != null) {
                onAction(MainAction.UpdateGroupSubscription(existing.guid))
            } else {
                onAction(MainAction.ImportBatchConfig(link))
            }
        }
    }

    // Fixed left-to-right layout so the retro look is identical in every language.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(W95Face)
        ) {
            // Title bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(W95Navy)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                BasicText(
                    text = "v2rayNG",
                    style = W95Text.copy(color = W95White, fontWeight = FontWeight.Bold),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(8.dp)
            ) {
                W95Label("Subscription URL:")
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .sunkenBevel(W95White)
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (url.isEmpty()) {
                            BasicText(
                                text = "https://",
                                style = W95Text.copy(color = W95Shadow),
                            )
                        }
                        BasicTextField(
                            value = url,
                            onValueChange = { url = it },
                            textStyle = W95Text,
                            singleLine = true,
                            cursorBrush = SolidColor(W95Dark),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    W95Button(text = "Add", onClick = submit)
                }

                Spacer(Modifier.height(10.dp))

                if (groups.size > 1 && currentGroup != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        W95Label("Group:")
                        Spacer(Modifier.width(6.dp))
                        W95Button(
                            text = currentGroup.remarks.ifBlank { "-" },
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val index = groups.indexOfFirst { it.id == currentGroup.id }
                                val next = groups[(index + 1).mod(groups.size)]
                                onAction(MainAction.SelectGroup(next.id))
                            },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                W95Label("Configs (${servers.size}):")
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .sunkenBevel(W95White)
                        .padding(3.dp)
                ) {
                    if (servers.isEmpty()) {
                        BasicText(
                            text = "Paste a subscription link above and press Add.",
                            style = W95Text.copy(color = W95Shadow),
                            modifier = Modifier.padding(6.dp),
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(items = servers, key = { it.guid }) { server ->
                                val selected = server.guid == selectedGuid
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (selected) W95Navy else W95White)
                                        .clickable { onAction(MainAction.SelectServer(server.guid)) }
                                        .padding(horizontal = 6.dp, vertical = 9.dp)
                                ) {
                                    BasicText(
                                        text = server.profile.remarks,
                                        style = W95Text.copy(color = if (selected) W95White else W95Dark),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sunkenBevel(W95Face)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        text = statusText,
                        style = W95Text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(8.dp))

                W95Button(
                    text = if (isRunning) "Disconnect" else "Connect",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    onClick = { onAction(MainAction.ToggleService) },
                )
            }
        }
    }
}
