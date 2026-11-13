package com.v2ray.ang.ui.main

import android.os.Bundle
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.ZakiBadge
import com.v2ray.ang.ui.compose.ZakiCard
import com.v2ray.ang.ui.compose.glassMeshBackground
import com.v2ray.ang.ui.compose.ZakiLine
import com.v2ray.ang.ui.compose.ZakiMuted
import com.v2ray.ang.ui.compose.ZakiSectionLabel
import com.v2ray.ang.ui.compose.ZakiTopBar
import com.v2ray.ang.ui.compose.zakiClickable
import com.v2ray.ang.ui.compose.zakiEnter
import java.text.DateFormat
import java.util.Date

/** Read-only technical details of one config, opened from the gear icon on the main screen. */
class ConfigDetailActivity : BaseComponentActivity() {

    private var profile: ProfileItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // BaseComponentActivity sets the content in onCreate, so load the data first.
        val guid = intent.getStringExtra(EXTRA_GUID).orEmpty()
        profile = if (guid.isEmpty()) null else MmkvManager.decodeServerConfig(guid)
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        ConfigDetailScreen(profile = profile, onBack = { finish() })
    }

    companion object {
        const val EXTRA_GUID = "guid"
    }
}

private data class Detail(val label: String, val value: String, val sensitive: Boolean = false)

private data class DetailSection(val title: String, val items: List<Detail>)

private fun detail(label: String, value: String?, sensitive: Boolean = false): Detail? =
    value?.takeIf { it.isNotBlank() }?.let { Detail(label, it, sensitive) }

private fun yesNo(value: Boolean?): String? = value?.let { if (it) "Yes" else "No" }

private fun buildSections(p: ProfileItem): List<DetailSection> {
    val added = runCatching {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(p.addedTime))
    }.getOrNull()
    return listOf(
        DetailSection(
            "General",
            listOfNotNull(
                detail("Name", p.remarks),
                detail("Protocol", p.configType.name.lowercase().replaceFirstChar { it.uppercase() }),
                detail("Address", p.server),
                detail("Port", p.serverPort),
                detail("Added", added)
            )
        ),
        DetailSection(
            "Authentication",
            listOfNotNull(
                detail("ID / Password", p.password, sensitive = true),
                detail("Method", p.method),
                detail("Flow", p.flow),
                detail("Username", p.username)
            )
        ),
        DetailSection(
            "Transport",
            listOfNotNull(
                detail("Network", p.network),
                detail("Header type", p.headerType),
                detail("Host", p.host),
                detail("Path", p.path),
                detail("Service name", p.serviceName),
                detail("Authority", p.authority),
                detail("Mode", p.mode),
                detail("XHTTP mode", p.xhttpMode),
                detail("Seed", p.seed, sensitive = true),
                detail("QUIC security", p.quicSecurity),
                detail("QUIC key", p.quicKey, sensitive = true)
            )
        ),
        DetailSection(
            "Security",
            listOfNotNull(
                detail("Security", p.security),
                detail("SNI", p.sni),
                detail("ALPN", p.alpn),
                detail("Fingerprint", p.fingerPrint),
                detail("Allow insecure", yesNo(p.insecure)),
                detail("Verify peer cert by name", p.verifyPeerCertByName),
                detail("Pinned CA SHA-256", p.pinnedCA256),
                detail("ECH config", p.echConfigList)
            )
        ),
        DetailSection(
            "Reality",
            listOfNotNull(
                detail("Public key", p.publicKey),
                detail("Short ID", p.shortId),
                detail("SpiderX", p.spiderX),
                detail("ML-DSA-65 verify", p.mldsa65Verify)
            )
        ),
        DetailSection(
            "WireGuard",
            listOfNotNull(
                detail("Secret key", p.secretKey, sensitive = true),
                detail("Pre-shared key", p.preSharedKey, sensitive = true),
                detail("Local address", p.localAddress),
                detail("Reserved", p.reserved),
                detail("MTU", p.mtu?.toString()),
                detail("Remote DNS", p.remoteDNS)
            )
        ),
        DetailSection(
            "Hysteria",
            listOfNotNull(
                detail("Obfs", p.obfs),
                detail("Obfs password", p.obfsPassword, sensitive = true),
                detail("Port hopping", p.portHopping),
                detail("Hop interval", p.portHoppingInterval),
                detail("Bandwidth up", p.bandwidthUp),
                detail("Bandwidth down", p.bandwidthDown)
            )
        )
    ).filter { it.items.isNotEmpty() }
}

@Composable
private fun ConfigDetailScreen(profile: ProfileItem?, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassMeshBackground()
    ) {
        ZakiTopBar(title = "Config details", onBack = onBack)
        if (profile == null) {
            Text(
                text = "This config is no longer available.",
                color = ZakiMuted,
                modifier = Modifier.padding(24.dp)
            )
            return@Column
        }
        val sections = remember(profile) { buildSections(profile) }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = profile.remarks,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(start = 6.dp, bottom = 12.dp)
                    .zakiEnter(0)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .padding(start = 6.dp, bottom = 20.dp)
                    .zakiEnter(40)
            ) {
                configBadges(profile).forEach { (text, color) -> ZakiBadge(text, color) }
            }
            sections.forEachIndexed { index, section ->
                Column(Modifier.zakiEnter(delayMs = 80 + index * 70)) {
                    ZakiSectionLabel(section.title)
                    ZakiCard(Modifier.fillMaxWidth()) {
                        Column {
                            section.items.forEachIndexed { i, item ->
                                if (i > 0) {
                                    HorizontalDivider(
                                        color = ZakiLine,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                                DetailRow(item)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(item: Detail) {
    var revealed by remember { mutableStateOf(false) }
    val shown = if (item.sensitive && !revealed) "\u2022".repeat(12) else item.value
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (item.sensitive) Modifier.zakiClickable { revealed = !revealed } else Modifier
            )
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                color = ZakiMuted,
                modifier = Modifier.weight(1f)
            )
            if (item.sensitive) {
                Text(
                    text = if (revealed) "Hide" else "Tap to show",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7C3AED)
                )
            }
        }
        SelectionContainer {
            Text(
                text = shown,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
