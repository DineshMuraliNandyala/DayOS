package com.lifeos

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.lifeos.ui.navigation.NavGraph
import com.lifeos.ui.screens.lock.BiometricHelper
import com.lifeos.ui.screens.lock.LockScreen
import com.lifeos.ui.theme.LifeOSTheme

/**
 * Single Activity that hosts the entire Compose UI.
 *
 * Extends [FragmentActivity] — required by [androidx.biometric.BiometricPrompt],
 * which needs a FragmentActivity to host its internal DialogFragment.
 *
 * Security contract:
 *  • FLAG_SECURE is set BEFORE setContent — protects the first frame AND the
 *    Recent Apps thumbnail capture.
 *  • [isUnlocked] starts false on every cold start.
 *  • On [onStop] (app leaves foreground) we record the timestamp.
 *  • On [onResume], if the app was backgrounded longer than [LOCK_GRACE_PERIOD_MS]
 *    (30 s), we re-lock and force a new biometric challenge.
 *  • If the device has no enrolled biometric/credential, the lock is skipped
 *    (checked via [BiometricHelper.canAuthenticate]).
 */
class MainActivity : FragmentActivity() {

    /**
     * Whether the user has successfully authenticated in this session.
     * Backed by Compose snapshot state so recomposition is triggered automatically
     * when it flips from false → true (unlock) or true → false (re-lock).
     */
    private var isUnlocked by mutableStateOf(false)

    /** Epoch millis when the app last moved to background (onStop). */
    private var backgroundedAtMs: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── FLAG_SECURE ──────────────────────────────────────────────────────────
        // Prevents screenshots, screen recording, and hides content in the
        // Recent Apps thumbnail. MUST be set before setContent.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        enableEdgeToEdge()

        val vm = ViewModelProvider(
            this,
            MainViewModel.Factory((application as LifeOSApp).database),
        )[MainViewModel::class.java]

        setContent {
            val theme by vm.appTheme.collectAsState()
            val accent by vm.accentDomain.collectAsState()

            LifeOSTheme(appTheme = theme, accentDomain = accent) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isUnlocked || !BiometricHelper.canAuthenticate(this@MainActivity)) {
                        // ── Authenticated (or device has no enrolled credential) ──
                        NavGraph()
                    } else {
                        // ── Locked ────────────────────────────────────────────────
                        // LockScreen auto-triggers the biometric prompt on first
                        // composition. The "Unlock" button re-triggers it if the
                        // user dismissed or the system imposed a retry delay.
                        LockScreen(
                            onUnlockRequested = {
                                BiometricHelper.showPrompt(
                                    activity = this@MainActivity,
                                    onSuccess = { isUnlocked = true },
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    /**
     * Record the time the app goes to background so we can measure elapsed
     * time on the next [onResume] and decide whether to re-lock.
     */
    override fun onStop() {
        super.onStop()
        backgroundedAtMs = System.currentTimeMillis()
    }

    /**
     * Re-lock if the app was backgrounded longer than [LOCK_GRACE_PERIOD_MS].
     *
     * The grace period prevents re-auth on brief task-switches (e.g. opening the
     * camera for a photo attachment). 30 s is generous enough for UX but tight
     * enough to block someone who picks up an unattended phone.
     */
    override fun onResume() {
        super.onResume()
        val elapsed = backgroundedAtMs?.let { System.currentTimeMillis() - it }
        val shouldRelock = elapsed == null || elapsed > LOCK_GRACE_PERIOD_MS
        if (shouldRelock && BiometricHelper.canAuthenticate(this)) {
            isUnlocked = false
        }
        backgroundedAtMs = null
    }

    companion object {
        /**
         * How long (ms) the app can be backgrounded before re-locking.
         * 30 s = brief task-switch OK; leaving phone unattended → locks.
         */
        private const val LOCK_GRACE_PERIOD_MS = 30_000L
    }
}
