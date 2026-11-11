package com.v2ray.ang.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// V2Zaki palette: white surfaces + a single violet accent.
val ZakiPurple = Color(0xFF7C3AED)
val ZakiPurpleDeep = Color(0xFF5B21B6)
val ZakiPurpleLight = Color(0xFF8B5CF6)
val ZakiPurpleSoft = Color(0xFFF3EEFF)
val ZakiInk = Color(0xFF1C1B22)
val ZakiMuted = Color(0xFF8A86A0)
val ZakiLine = Color(0xFFEDE9F8)

private val LightColor = lightColorScheme(
    primary = ZakiPurple,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = ZakiPurpleSoft,
    onPrimaryContainer = Color(0xFF3B1A8C),
    secondary = ZakiPurple,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = ZakiPurpleSoft,
    onSecondaryContainer = Color(0xFF3B1A8C),
    tertiary = Color(0xFF0E9F6E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDF7EC),
    onTertiaryContainer = Color(0xFF064E3B),
    error = Color(0xFFD92D5B),
    errorContainer = Color(0xFFFFE4EB),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF5F0B22),
    background = Color(0xFFFFFFFF),
    onBackground = ZakiInk,
    surface = Color(0xFFFFFFFF),
    onSurface = ZakiInk,
    surfaceVariant = Color(0xFFF3EEFF),
    onSurfaceVariant = Color(0xFF6B6880),
    outline = Color(0xFFB9B3CC),
    outlineVariant = ZakiLine,
    inverseSurface = Color(0xFF2A2338),
    inverseOnSurface = Color(0xFFF6F3FF),
    inversePrimary = Color(0xFFCDB9FF),
    scrim = Color(0xFF000000),
    surfaceTint = ZakiPurple,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF9FF),
    surfaceContainer = Color(0xFFF7F4FE),
    surfaceContainerHigh = Color(0xFFF1ECFC),
    surfaceContainerHighest = Color(0xFFEBE5FA),
)

// The app is intentionally white + purple only, so the dark scheme mirrors the light one.
private val DarkColor = LightColor

// Semantic Colors
val colorPing = Color(0xFF0E9F6E) // Green
val colorPingRed = Color(0xFFD92D5B) // Red
val colorConfigType = ZakiPurple
val colorFabActive = ZakiPurple
val colorFabInactiveLight = ZakiPurpleLight
val colorFabInactiveDark = ZakiPurpleLight
val dividerColorLight = ZakiLine
val dividerColorDark = ZakiLine

// Toast Colors 85%
val toastNormalBgLight = Color(0xE62A2338) // Deep violet-black
val toastNormalBgDark = Color(0xE62A2338) // Deep violet-black
val toastSuccessBg = Color(0xD9388E3C) // Green
val toastErrorBg = Color(0xD9D50000) // Red
val toastInfoBg = Color(0xE67C3AED) // Violet
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    // V2Zaki is white + purple only; the stored day/night preference is ignored.
    return false
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColor else LightColor
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(10.dp),
                small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(28.dp)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
