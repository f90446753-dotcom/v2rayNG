package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.ServersCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// ---------- Plain console look: black background, light-gray monospace text ----------
private val TermBg = Color(0xFF000000)
private val TermFg = Color(0xFFCCCCCC)
private val TermWhite = Color(0xFFFFFFFF)
private val TermGreen = Color(0xFF16C60C)
private val TermRed = Color(0xFFE74856)
private val TermYellow = Color(0xFFF9F1A5)
private val TermStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = TermFg)

private data class TermLine(val text: String, val color: Color = TermFg)

private enum class TermMode { COMMAND, ENTER_URL, PICK_CONFIG }

private const val MAX_LINES = 400

/**
 * The whole app is a text console. No buttons, menus or lists:
 *   1 = connect, 0 = disconnect, S = change subscription link, C = choose config.
 * Only ONE subscription exists; entering a new link replaces the previous one.
 */
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    val lines = remember { mutableStateListOf<TermLine>() }
    var mode by remember { mutableStateOf(TermMode.COMMAND) }
    var input by remember { mutableStateOf("") }
    var pickList by remember { mutableStateOf(emptyList<ServersCache>()) }
    var prevRunning by remember { mutableStateOf<Boolean?>(null) }

    fun say(text: String, color: Color = TermFg) {
        lines.add(TermLine(text, color))
        while (lines.size > MAX_LINES) lines.removeAt(0)
    }

    // The built-in "default" group and the "All" group are hidden: only the user's subscription counts.
    fun realGroup(): GroupMapItem? = uiState.groups.firstOrNull {
        it.id.isNotEmpty() && it.id != AppConfig.DEFAULT_SUBSCRIPTION_ID
    }

    fun serversOf(groupId: String): List<ServersCache> =
        mainViewModel.serverGroupState(groupId).value.servers

    fun selectedName(): String {
        val group = realGroup() ?: return "-"
        val guid = uiState.selectedGuid
        return serversOf(group.id).firstOrNull { it.guid == guid }?.profile?.remarks ?: "-"
    }

    fun sayHelp() {
        say("Commands:")
        say("  1   connect")
        say("  0   disconnect")
        say("  S   change subscription link")
        say("  C   choose config")
        say("  ?   help")
    }

    fun askForLink() {
        say("Enter subscription link:", TermYellow)
        mode = TermMode.ENTER_URL
    }

    suspend fun showConfigsOf(groupId: String) {
        if (uiState.selectedGroupId != groupId) onAction(MainAction.SelectGroup(groupId))
        delay(500)
        val list = serversOf(groupId)
        if (list.isEmpty()) {
            say("[FAIL] No configs found in this subscription.", TermRed)
            say("Press S to enter another link.")
            return
        }
        val currentGuid = uiState.selectedGuid
        val chosen = list.firstOrNull { it.guid == currentGuid } ?: list.first()
        if (chosen.guid != currentGuid) onAction(MainAction.SelectServer(chosen.guid))
        say("Configs (${list.size}):")
        list.forEach { say("  ${if (it.guid == chosen.guid) "*" else " "} ${it.profile.remarks}") }
        say("Selected: ${chosen.profile.remarks}", TermGreen)
        say("")
        sayHelp()
    }

    suspend fun waitForLoading() {
        withTimeoutOrNull(3_000) { mainViewModel.isLoading.first { it } }
        withTimeoutOrNull(90_000) { mainViewModel.isLoading.first { !it } }
        delay(300)
    }

    fun submitLink(link: String) {
        scope.launch {
            say("Loading subscription...")
            val before = realGroup()?.id
            val existing = mainViewModel.getSubscriptions().firstOrNull { it.subscription.url == link }
            if (existing != null) {
                onAction(MainAction.UpdateGroupSubscription(existing.guid))
            } else {
                onAction(MainAction.ReplaceSubscription(link))
            }
            waitForLoading()
            val sub = realGroup()
            val replacedOk = existing != null || (sub != null && sub.id != before)
            if (sub == null || !replacedOk) {
                say("[FAIL] Could not load this link.", TermRed)
                if (sub != null) say("The previous subscription was kept.")
                askForLink()
            } else {
                say("[ OK ] Subscription loaded.", TermGreen)
                showConfigsOf(sub.id)
            }
        }
    }

    fun handleInput(raw: String) {
        val text = raw.trim()
        val prompt = when (mode) {
            TermMode.COMMAND -> ">"
            TermMode.ENTER_URL -> "link>"
            TermMode.PICK_CONFIG -> "number>"
        }
        say("$prompt $text", TermWhite)

        when (mode) {
            TermMode.ENTER_URL -> {
                if (text.isEmpty()) {
                    if (realGroup() != null) {
                        say("Cancelled.")
                        mode = TermMode.COMMAND
                    } else {
                        say("Enter subscription link:", TermYellow)
                    }
                } else if (!text.startsWith("http://", true) && !text.startsWith("https://", true)) {
                    say("[FAIL] The link must start with http:// or https://", TermRed)
                } else {
                    mode = TermMode.COMMAND
                    submitLink(text)
                }
            }

            TermMode.PICK_CONFIG -> {
                val number = text.toIntOrNull()
                if (text.isEmpty()) {
                    say("Cancelled.")
                    mode = TermMode.COMMAND
                } else if (number == null || number < 1 || number > pickList.size) {
                    say("[FAIL] Enter a number from 1 to ${pickList.size}, or press Enter to cancel.", TermRed)
                } else {
                    val server = pickList[number - 1]
                    onAction(MainAction.SelectServer(server.guid))
                    say("[ OK ] Selected: ${server.profile.remarks}", TermGreen)
                    mode = TermMode.COMMAND
                }
            }

            TermMode.COMMAND -> when (text.lowercase()) {
                "" -> Unit
                "1" -> {
                    if (uiState.isRunning) {
                        say("Already connected.")
                    } else if (uiState.selectedGuid.isNullOrEmpty()) {
                        say("[FAIL] No config selected. Press S to enter a subscription link.", TermRed)
                    } else {
                        say("Connecting...")
                        onAction(MainAction.ToggleService)
                    }
                }

                "0" -> {
                    if (uiState.isRunning) {
                        say("Disconnecting...")
                        onAction(MainAction.ToggleService)
                    } else {
                        say("Already disconnected.")
                    }
                }

                "s" -> askForLink()

                "c" -> {
                    val group = realGroup()
                    val list = if (group != null) serversOf(group.id) else emptyList()
                    if (list.isEmpty()) {
                        say("[FAIL] No configs. Press S to enter a subscription link.", TermRed)
                    } else {
                        pickList = list
                        list.forEachIndexed { index, server ->
                            val mark = if (server.guid == uiState.selectedGuid) "*" else " "
                            say("  $mark ${index + 1}. ${server.profile.remarks}")
                        }
                        say("Enter config number (Enter = cancel):", TermYellow)
                        mode = TermMode.PICK_CONFIG
                    }
                }

                "?", "h", "help" -> sayHelp()

                else -> {
                    say("Unknown command: $text", TermRed)
                    sayHelp()
                }
            }
        }
    }

    // Banner, printed every time the app opens.
    LaunchedEffect(Unit) {
        say("Design and Development:")
        say("Hassan Zakizadeh")
        say("")
        runCatching { focusRequester.requestFocus() }
    }

    // First start: wait for the app data to load, then either ask for a link or show the configs.
    val initialized = uiState.groups.isNotEmpty()
    LaunchedEffect(initialized) {
        if (!initialized) return@LaunchedEffect
        delay(300)
        val sub = realGroup()
        if (sub == null) {
            say("No subscription found.")
            askForLink()
        } else {
            showConfigsOf(sub.id)
        }
    }

    // Connection state changes.
    LaunchedEffect(uiState.isRunning) {
        val previous = prevRunning
        prevRunning = uiState.isRunning
        if (uiState.isRunning) {
            say("[ OK ] Connected: ${selectedName()}", TermGreen)
        } else if (previous == true) {
            say("[ -- ] Disconnected.", TermYellow)
        }
    }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    val promptText = when (mode) {
        TermMode.COMMAND -> "> "
        TermMode.ENTER_URL -> "link> "
        TermMode.PICK_CONFIG -> "number> "
    }

    // Fixed left-to-right layout, like a real console.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TermBg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 8.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                items(count = lines.size) { index ->
                    val line = lines.getOrNull(index)
                    if (line != null) {
                        BasicText(text = line.text, style = TermStyle.copy(color = line.color))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(text = promptText, style = TermStyle.copy(color = TermWhite))
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    textStyle = TermStyle.copy(color = TermWhite),
                    singleLine = true,
                    cursorBrush = SolidColor(TermWhite),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = if (mode == TermMode.ENTER_URL) KeyboardType.Uri else KeyboardType.Text,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val text = input
                            input = ""
                            handleInput(text)
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                )
            }
        }
    }
}
