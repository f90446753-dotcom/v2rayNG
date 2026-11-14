package com.v2ray.ang.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.ui.compose.ZakiBadge
import com.v2ray.ang.ui.compose.ZakiCard
import com.v2ray.ang.ui.compose.ZakiCheckDot
import com.v2ray.ang.ui.compose.ZakiIconButton
import com.v2ray.ang.ui.compose.ZakiLogo
import com.v2ray.ang.ui.compose.ZakiMuted
import com.v2ray.ang.ui.compose.ZakiPurple
import com.v2ray.ang.ui.compose.colorPing
import com.v2ray.ang.ui.compose.colorPingRed
import com.v2ray.ang.ui.compose.zakiClickable
import com.v2ray.ang.ui.compose.zakiEnter

private val BadgeBlue = Color(0xFF2563EB)
private val BadgeGreen = Color(0xFF0E9F6E)
private val BadgeAmber = Color(0xFFD97706)
private val BadgeGray = Color(0xFF8A86A0)

/** (Vless) (xhttp) (tls) style badges: protocol, network and certificate/security state. */
internal fun configBadges(profile: ProfileItem): List<Pair<String, Color>> {
    val type = profile.configType
    val badges = mutableListOf<Pair<String, Color>>()
    badges += type.name.lowercase().replaceFirstChar { it.uppercase() } to ZakiPurple
    if (type.isComplexType() || type == EConfigType.CUSTOM) return badges

    val usesTransport = type == EConfigType.VMESS || type == EConfigType.VLESS || type == EConfigType.TROJAN
    val network = profile.network?.takeIf { it.isNotBlank() } ?: if (usesTransport) "tcp" else null
    if (network != null) badges += network to BadgeBlue

    val security = profile.security?.takeIf { it.isNotBlank() }
    when {
        security == null -> if (usesTransport) badges += "none" to BadgeGray
        profile.insecure == true && security.equals("tls", ignoreCase = true) ->
            badges += "$security \u00B7 insecure" to BadgeAmber

        else -> badges += security to BadgeGreen
    }
    return badges
}

@Composable
internal fun ConfigListHost(
    groupId: String,
    mainViewModel: MainViewModel,
    selectedGuid: String?,
    topPadding: Dp,
    bottomPadding: Dp,
    contentVisible: Boolean,
    onSelect: (String) -> Unit,
    onDetails: (String) -> Unit,
) {
    val groupFlow = remember(groupId) { mainViewModel.serverGroupState(groupId) }
    val groupState by groupFlow.collectAsStateWithLifecycle()
    val rows = groupState.rows

    if (rows.isEmpty()) {
        NoConfigsHint(contentVisible = contentVisible)
        return
    }

    val listState = rememberLazyListState()
    // Cards that have already played their entrance animation do not replay it while scrolling.
    val seen = remember { HashSet<String>() }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPadding, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items = rows, key = { _, row -> row.guid }) { index, row ->
            val firstTime = remember(row.guid) { seen.add(row.guid) }
            ConfigCard(
                modifier = Modifier.animateItem(),
                row = row,
                selected = row.guid == selectedGuid,
                animateIn = firstTime && index < 12,
                enterDelayMs = index * 55,
                contentVisible = contentVisible,
                onSelect = { onSelect(row.guid) },
                onDetails = { onDetails(row.guid) }
            )
        }
    }
}

@Composable
private fun ConfigCard(
    modifier: Modifier = Modifier,
    row: ServerRowUiModel,
    selected: Boolean,
    animateIn: Boolean,
    enterDelayMs: Int,
    contentVisible: Boolean,
    onSelect: () -> Unit,
    onDetails: () -> Unit,
) {
    ZakiCard(
        modifier = modifier
            .fillMaxWidth()
            .zakiEnter(delayMs = enterDelayMs, enabled = animateIn, start = contentVisible)
            .zakiClickable(onClick = onSelect),
        selected = selected
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZakiCheckDot(checked = selected, size = 20.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.remarks,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (row.testDelayMillis != 0L) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (row.testDelayMillis > 0) "${row.testDelayMillis} ms" else "timeout",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (row.testDelayMillis > 0) colorPing else colorPingRed,
                            maxLines = 1
                        )
                    }
                }
                if (row.statistics.isNotBlank()) {
                    Text(
                        text = row.statistics,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZakiMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    configBadges(row.profile).forEach { (text, color) ->
                        ZakiBadge(text = text, color = color)
                    }
                }
            }
            ZakiIconButton(
                iconRes = R.drawable.ic_settings_24dp,
                contentDescription = "Config details",
                onClick = onDetails,
                size = 36.dp,
                iconSize = 19.dp,
                tint = ZakiMuted
            )
        }
    }
}

@Composable
private fun NoConfigsHint(contentVisible: Boolean) {
    JavidEmptyState(contentVisible = contentVisible)
}
