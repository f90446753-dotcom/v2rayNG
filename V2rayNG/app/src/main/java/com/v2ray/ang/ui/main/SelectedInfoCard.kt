package com.v2ray.ang.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.dto.ConnectionTestResult
import com.v2ray.ang.extension.toSpeedString
import com.v2ray.ang.extension.toTrafficString
import com.v2ray.ang.ui.compose.GlassCardSurface
import com.v2ray.ang.ui.compose.ZakiBadge
import com.v2ray.ang.ui.compose.ZakiLine
import com.v2ray.ang.ui.compose.ZakiMuted
import com.v2ray.ang.ui.compose.ZakiPurple
import com.v2ray.ang.ui.compose.colorPing
import com.v2ray.ang.ui.compose.zakiClickable
import com.v2ray.ang.ui.compose.zakiEnter

/** Fixed height of the card: about two config cards. */
internal val SelectedInfoCardHeight = 168.dp

@Composable
private fun rememberSelectedRow(
    groupId: String?,
    mainViewModel: MainViewModel,
    selectedGuid: String?
): ServerRowUiModel? {
    if (groupId == null) return null
    val flow = remember(groupId) { mainViewModel.serverGroupState(groupId) }
    val state by flow.collectAsStateWithLifecycle()
    return state.rows.firstOrNull { it.guid == selectedGuid }
}

/**
 * Pinned card above the config list (it never scrolls): the selected config, where the connection
 * exits, the current speed and the data used since connecting.
 */
@Composable
internal fun SelectedInfoCard(
    groupId: String?,
    mainViewModel: MainViewModel,
    selectedGuid: String?,
    isRunning: Boolean,
    connection: ConnectionTestResult?,
    traffic: TrafficSample,
    contentVisible: Boolean,
    onRefreshLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val row = rememberSelectedRow(groupId, mainViewModel, selectedGuid)

    GlassCardSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(SelectedInfoCardHeight)
            .zakiEnter(delayMs = 80, start = contentVisible)
    ) {
        if (row == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No config selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = row.remarks,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (row.statistics.isNotBlank()) {
                    Text(
                        text = row.statistics,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(top = 7.dp)
                ) {
                    configBadges(row.profile).forEach { (text, color) ->
                        ZakiBadge(text = text, color = color)
                    }
                }
                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = ZakiLine)
                Spacer(Modifier.height(9.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    LocationCell(
                        isRunning = isRunning,
                        connection = connection,
                        onClick = onRefreshLocation,
                        modifier = Modifier.weight(1.05f)
                    )
                    Spacer(Modifier.width(8.dp))
                    RateCell(
                        label = "Speed",
                        down = if (isRunning && traffic.supported) traffic.downBytesPerSec.toSpeedString() else null,
                        up = if (isRunning && traffic.supported) traffic.upBytesPerSec.toSpeedString() else null,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    RateCell(
                        label = "Data",
                        down = if (isRunning && traffic.supported) traffic.downTotal.toTrafficString() else null,
                        up = if (isRunning && traffic.supported) traffic.upTotal.toTrafficString() else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CellLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = ZakiMuted,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun LocationCell(
    isRunning: Boolean,
    connection: ConnectionTestResult?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val country = connection?.country?.takeIf { it.isNotBlank() }
    val ip = connection?.ipAddress?.takeIf { it.isNotBlank() }
    Column(modifier = modifier.zakiClickable(enabled = isRunning, onClick = onClick)) {
        CellLabel("Location")
        Spacer(Modifier.height(3.dp))
        val first = when {
            !isRunning -> "\u2014"
            connection == null -> "Locating\u2026"
            country != null -> country
            else -> "Unavailable"
        }
        Text(
            text = first,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isRunning && connection != null && country != null) MaterialTheme.colorScheme.onSurface else ZakiMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (isRunning && ip != null) ip else " ",
            style = MaterialTheme.typography.bodySmall,
            color = ZakiMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RateCell(label: String, down: String?, up: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        CellLabel(label)
        Spacer(Modifier.height(3.dp))
        ArrowLine(arrow = "\u2193", arrowColor = colorPing, value = down)
        ArrowLine(arrow = "\u2191", arrowColor = ZakiPurple, value = up)
    }
}

@Composable
private fun ArrowLine(arrow: String, arrowColor: Color, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = arrow,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (value != null) arrowColor else ZakiMuted
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = value ?: "\u2014",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (value != null) MaterialTheme.colorScheme.onSurface else ZakiMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
