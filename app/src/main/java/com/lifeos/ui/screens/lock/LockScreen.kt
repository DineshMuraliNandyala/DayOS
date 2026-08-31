package com.lifeos.ui.screens.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Full-screen lock overlay rendered until the user successfully authenticates.
 *
 * Behaviour:
 *  - [LaunchedEffect] auto-triggers [onUnlockRequested] on first composition so
 *    the biometric prompt appears immediately without any user tap.
 *  - The "Unlock" button lets the user re-trigger the prompt after a failed
 *    attempt, a temporary lockout expiry, or if they cancelled the system sheet.
 *  - This composable never shows sensitive data — it renders on top of (and
 *    hides) all content until [isUnlocked] flips in [MainActivity].
 *
 * FLAG_SECURE is set in [MainActivity.onCreate], so this screen's Recent Apps
 * thumbnail and any screenshot attempt will be blocked at the window level.
 */
@Composable
fun LockScreen(onUnlockRequested: () -> Unit) {

    // Auto-trigger the system biometric prompt on first composition.
    LaunchedEffect(Unit) {
        onUnlockRequested()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "LifeOS",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = "Authenticate to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(onClick = onUnlockRequested) {
                Text(text = "Unlock")
            }
        }
    }
}
