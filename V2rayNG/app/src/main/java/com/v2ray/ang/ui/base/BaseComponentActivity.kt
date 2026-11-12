package com.v2ray.ang.ui.base

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.ui.compose.AppTheme
import com.v2ray.ang.ui.compose.ThemeManager

abstract class BaseComponentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLocaleManager.onActivityCreated(this)
        enableEdgeToEdge()
        // Avoid a white flash behind the dark theme while the first frame is being drawn.
        if (!keepWhiteWindow && ThemeManager.themeMode.value != "1") {
            window.setBackgroundDrawable(ColorDrawable(Color.parseColor("#0F0C18")))
        }
        setContent {
            AppTheme {
                ScreenContent()
            }
        }
    }

    /** Reserved: screens that must keep the default (white) window background. */
    protected open val keepWhiteWindow: Boolean get() = false

    @Composable
    protected abstract fun ScreenContent()
}