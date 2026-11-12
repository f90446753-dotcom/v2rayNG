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

// V2Zaki palette: one violet accent on white (light) or deep violet-black (dark) surfaces.
val ZakiPurple = Color(0xFF7C3AED)
val ZakiPurpleDeep = Color(0xFF5B21B6)
val ZakiPurpleLight = Color(0xFF8B5CF6)

// Colours that change between light and dark. They are read inside composables only.
val ZakiPurpleSoft: Color @Composable get() = if (LocalDarkTheme.current) Color(0xFF262040) else Color(0xFFF3EEFF)
val ZakiInk: Color @Composable get() = if (LocalDarkTheme.current) Color(0xFFF3F0FA) else Color(0xFF1C1B22)
val ZakiMuted: Color @Composable get() = if (LocalDarkTheme.current) Color(0xFF9B96B3) else Color(0xFF8A86A0)
val ZakiLine: Color @Composable get() = if (LocalDarkTheme.current) Color(0xFF2B2440) else Color(0xFFEDE9F8)
val ZakiSurface: Color @Composable get() = if (LocalDarkTheme.current) Color(0xFF191426) else Color(0xFFFFFFFF)

private val LightColor = lightColorScheme(
    primary = ZakiPurple,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3EEFF),
    onPrimaryContainer = Color(0xFF3B1A8C),
    secondary = ZakiPurple,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3EEFF),
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
    onBackground = Color(0xFF1C1B22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B22),
    surfaceVariant = Color(0xFFF3EEFF),
    onSurfaceVariant = Color(0xFF6B6880),
    outline = Color(0xFFB9B3CC),
    outlineVariant = Color(0xFFEDE9F8),
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

private val DarkColor = darkColorScheme(
    primary = ZakiPurpleLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2A2145),
    onPrimaryContainer = Color(0xFFE4D9FF),
    secondary = ZakiPurpleLight,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2A2145),
    onSecondaryContainer = Color(0xFFE4D9FF),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF04281C),
    tertiaryContainer = Color(0xFF0B3B2A),
    onTertiaryContainer = Color(0xFFB7F5DC),
    error = Color(0xFFF87191),
    errorContainer = Color(0xFF4A1224),
    onError = Color(0xFF3A0A19),
    onErrorContainer = Color(0xFFFFD9E2),
    background = Color(0xFF0F0C18),
    onBackground = Color(0xFFF3F0FA),
    surface = Color(0xFF0F0C18),
    onSurface = Color(0xFFF3F0FA),
    surfaceVariant = Color(0xFF262040),
    onSurfaceVariant = Color(0xFFB3AEC9),
    outline = Color(0xFF6F6987),
    outlineVariant = Color(0xFF2B2440),
    inverseSurface = Color(0xFFE9E5F5),
    inverseOnSurface = Color(0xFF1B1730),
    inversePrimary = Color(0xFF6D3BD6),
    scrim = Color(0xFF000000),
    surfaceTint = ZakiPurpleLight,
    surfaceContainerLowest = Color(0xFF0C0914),
    surfaceContainerLow = Color(0xFF141020),
    surfaceContainer = Color(0xFF191426),
    surfaceContainerHigh = Color(0xFF211B33),
    surfaceContainerHighest = Color(0xFF2A233F),
)

// Semantic Colors
val colorPing = Color(0xFF0E9F6E) // Green
val colorPingRed = Color(0xFFD92D5B) // Red
val colorConfigType = ZakiPurple
val colorFabActive = ZakiPurple
val colorFabInactiveLight = ZakiPurpleLight
val colorFabInactiveDark = ZakiPurpleLight
val dividerColorLight = Color(0xFFEDE9F8)
val dividerColorDark = Color(0xFF2B2440)

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
    // "1" = light, "2" = dark. V2Zaki starts in light (white + purple) until the user picks dark.
    val mode by ThemeManager.themeMode.collectAsState()
    return mode == "2"
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
