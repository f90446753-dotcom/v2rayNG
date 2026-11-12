package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.ZakiLogo
import com.v2ray.ang.ui.compose.ZakiMuted
import com.v2ray.ang.ui.compose.ZakiPurple
import com.v2ray.ang.ui.compose.ZakiPurpleSoft
import com.v2ray.ang.ui.compose.ZakiSurface
import com.v2ray.ang.ui.compose.zakiClickable

/**
 * Destinations the main screen can navigate to. Only Settings and PerAppProxy are shown in the
 * menu of this app; the rest are kept so existing navigation code keeps compiling.
 */
enum class MainDestination(@DrawableRes val iconRes: Int, @StringRes val labelRes: Int) {
    Subscriptions(R.drawable.ic_subscriptions_24dp, R.string.title_sub_setting),
    PerAppProxy(R.drawable.ic_per_apps_24dp, R.string.per_app_proxy_settings),
    Routing(R.drawable.ic_routing_24dp, R.string.routing_settings_title),
    UserAssets(R.drawable.ic_file_24dp, R.string.title_user_asset_setting),
    Settings(R.drawable.ic_settings_24dp, R.string.title_settings),
    Promotion(R.drawable.ic_promotion_24dp, R.string.title_pref_promotion),
    Logcat(R.drawable.ic_logcat_24dp, R.string.title_logcat),
    CheckUpdate(R.drawable.ic_check_update_24dp, R.string.update_check_for_update),
    BackupRestore(R.drawable.ic_restore_24dp, R.string.title_configuration_backup_restore),
    About(R.drawable.ic_about_24dp, R.string.title_about)
}

private data class DrawerEntry(
    val destination: MainDestination,
    @DrawableRes val iconRes: Int,
    val label: String
)

private val drawerEntries = listOf(
    DrawerEntry(MainDestination.Settings, R.drawable.ic_settings_24dp, "Settings"),
    DrawerEntry(MainDestination.PerAppProxy, R.drawable.ic_per_apps_24dp, "Per-App settings")
)

@Composable
fun MainDrawerContent(drawerState: DrawerState, onNavigate: (MainDestination) -> Unit) {
    val open = drawerState.targetValue == DrawerValue.Open

    ModalDrawerSheet(
        drawerState = drawerState,
        modifier = Modifier.fillMaxWidth(0.76f),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(36.dp))
            val headerProgress by animateFloatAsState(
                targetValue = if (open) 1f else 0f,
                animationSpec = tween(450),
                label = "drawerHeader"
            )
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = headerProgress
                    translationY = (1f - headerProgress) * 16.dp.toPx()
                }
            ) {
                ZakiLogo(size = 64.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "V2Zaki",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(32.dp))
            drawerEntries.forEachIndexed { index, entry ->
                val progress by animateFloatAsState(
                    targetValue = if (open) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 420,
                        delayMillis = if (open) 80 + index * 70 else 0
                    ),
                    label = "drawerItem$index"
                )
                DrawerItem(
                    entry = entry,
                    modifier = Modifier.graphicsLayer {
                        alpha = progress
                        translationX = (1f - progress) * -24.dp.toPx()
                    },
                    onClick = { onNavigate(entry.destination) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun DrawerItem(entry: DrawerEntry, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ZakiPurpleSoft.copy(alpha = 0.55f))
            .zakiClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(ZakiSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(entry.iconRes),
                contentDescription = null,
                tint = ZakiPurple,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = entry.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
