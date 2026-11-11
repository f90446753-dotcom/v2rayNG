package com.v2ray.ang.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.VPN
import com.v2ray.ang.R
import com.v2ray.ang.handler.MmkvManager.rememberMmkvBool
import com.v2ray.ang.handler.MmkvManager.rememberMmkvString
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.ZakiCard
import com.v2ray.ang.ui.compose.ZakiLine
import com.v2ray.ang.ui.compose.ZakiSectionLabel
import com.v2ray.ang.ui.compose.ZakiSegmented
import com.v2ray.ang.ui.compose.ZakiSwitchRow
import com.v2ray.ang.ui.compose.ZakiTopBar
import com.v2ray.ang.ui.compose.ZakiValueRow
import com.v2ray.ang.ui.compose.zakiEnter

class SettingsActivity : BaseComponentActivity() {

    @Composable
    override fun ScreenContent() {
        SettingsScreen(onBackClick = { finish() })
    }
}

/** V2Zaki settings: only the essentials, in three quiet cards. */
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    var speedEnabled by rememberMmkvBool(AppConfig.PREF_SPEED_ENABLED, false)
    var socksPort by rememberMmkvString(AppConfig.PREF_SOCKS_PORT, "")
    var proxySharing by rememberMmkvBool(AppConfig.PREF_PROXY_SHARING, false)
    var socksUsername by rememberMmkvString(AppConfig.PREF_SOCKS_USERNAME, "")
    var socksPassword by rememberMmkvString(AppConfig.PREF_SOCKS_PASSWORD, "")
    var mode by rememberMmkvString(AppConfig.PREF_MODE, VPN)

    val modeEntries = stringArrayResource(R.array.mode_entries).toList()
    val modeValues = stringArrayResource(R.array.mode_value).toList()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ZakiTopBar(title = "Settings", onBack = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Column(Modifier.zakiEnter(delayMs = 0)) {
                ZakiSectionLabel("Display")
                ZakiCard(Modifier.fillMaxWidth()) {
                    ZakiSwitchRow(
                        title = "Enable speed display",
                        checked = speedEnabled,
                        onCheckedChange = { speedEnabled = it }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Column(Modifier.zakiEnter(delayMs = 90)) {
                ZakiSectionLabel("Local proxy")
                ZakiCard(Modifier.fillMaxWidth()) {
                    Column {
                        ZakiValueRow(
                            title = "Local proxy port",
                            value = socksPort,
                            placeholder = "Default",
                            keyboardNumber = true,
                            onValueChange = { socksPort = it }
                        )
                        RowDivider()
                        ZakiSwitchRow(
                            title = "Allow connections from the Lan",
                            checked = proxySharing,
                            onCheckedChange = { proxySharing = it }
                        )
                        RowDivider()
                        ZakiValueRow(
                            title = "Local proxy username",
                            value = socksUsername,
                            onValueChange = { socksUsername = it }
                        )
                        RowDivider()
                        ZakiValueRow(
                            title = "Local proxy password",
                            value = socksPassword,
                            isPassword = true,
                            onValueChange = { socksPassword = it }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Column(Modifier.zakiEnter(delayMs = 180)) {
                ZakiSectionLabel("Mode")
                ZakiSegmented(
                    options = modeEntries,
                    selectedIndex = modeValues.indexOf(mode).coerceAtLeast(0),
                    onSelect = { index -> modeValues.getOrNull(index)?.let { mode = it } }
                )
            }

            Spacer(Modifier.navigationBarsPadding().height(32.dp))
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = ZakiLine, modifier = Modifier.padding(horizontal = 18.dp))
}

private fun Modifier.fillMaxWidth(): Modifier =
    this.then(Modifier.fillMaxWidth())
