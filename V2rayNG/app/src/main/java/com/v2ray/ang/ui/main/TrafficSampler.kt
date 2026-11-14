package com.v2ray.ang.ui.main

import android.os.Process
import android.os.SystemClock
import android.net.TrafficStats
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Live data usage of the app: current speed and totals since the connection was started. */
internal data class TrafficSample(
    val supported: Boolean = true,
    val downBytesPerSec: Long = 0L,
    val upBytesPerSec: Long = 0L,
    val downTotal: Long = 0L,
    val upTotal: Long = 0L,
)

/**
 * Reads the app's own network counters (android.net.TrafficStats, read-only) once a second while
 * [active] and the screen is visible. Nothing is asked from the proxy core, so it cannot affect
 * the connection. Because the core uses this app's network identity, the numbers are the traffic
 * that really leaves / reaches the phone through the connection (including a little protocol overhead).
 */
@Composable
internal fun rememberTrafficSample(active: Boolean): State<TrafficSample> {
    val state = remember { mutableStateOf(TrafficSample()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(active, lifecycleOwner) {
        state.value = TrafficSample()
        if (!active) return@LaunchedEffect

        val uid = Process.myUid()
        val baseRx = TrafficStats.getUidRxBytes(uid)
        val baseTx = TrafficStats.getUidTxBytes(uid)
        if (baseRx < 0L || baseTx < 0L) {
            state.value = TrafficSample(supported = false)
            return@LaunchedEffect
        }

        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var lastRx = TrafficStats.getUidRxBytes(uid)
            var lastTx = TrafficStats.getUidTxBytes(uid)
            var lastTime = SystemClock.elapsedRealtime()
            while (true) {
                delay(1000)
                val rx = TrafficStats.getUidRxBytes(uid)
                val tx = TrafficStats.getUidTxBytes(uid)
                val now = SystemClock.elapsedRealtime()
                if (rx < 0L || tx < 0L) continue
                val seconds = (now - lastTime).coerceAtLeast(1L) / 1000.0
                state.value = TrafficSample(
                    supported = true,
                    downBytesPerSec = ((rx - lastRx) / seconds).toLong().coerceAtLeast(0L),
                    upBytesPerSec = ((tx - lastTx) / seconds).toLong().coerceAtLeast(0L),
                    downTotal = (rx - baseRx).coerceAtLeast(0L),
                    upTotal = (tx - baseTx).coerceAtLeast(0L)
                )
                lastRx = rx
                lastTx = tx
                lastTime = now
            }
        }
    }
    return state
}
