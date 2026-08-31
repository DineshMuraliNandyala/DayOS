package com.lifeos.ui.screens.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lifeos.R

/**
 * Utility for biometric / device-credential authentication.
 *
 * Uses BIOMETRIC_STRONG | DEVICE_CREDENTIAL so fingerprint, face, iris,
 * and PIN/pattern/password are all valid unlock methods. No custom in-app
 * PIN — we rely entirely on the system credential store (matches the security
 * architecture in the project brief: FBE + system credential, not a second
 * password the user might lose).
 */
object BiometricHelper {

    private val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /**
     * Returns true if this device has an enrolled biometric or device credential
     * that can be used to unlock LifeOS. If false, the app skips the lock screen
     * entirely (no point showing a lock the user cannot pass).
     */
    fun canAuthenticate(context: Context): Boolean =
        BiometricManager.from(context)
            .canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Shows the system biometric / credential prompt.
     *
     * @param activity  Must be a [FragmentActivity] — BiometricPrompt requirement.
     *                  [MainActivity] extends [FragmentActivity] for this reason.
     * @param onSuccess Called on the main thread after successful authentication.
     *                  The caller is responsible for updating app unlock state.
     */
    fun showPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    // User explicitly cancelled (or pressed Back) — exit the app.
                    // There is no guest mode: data is inaccessible without auth.
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        activity.finishAffinity()
                    }
                    // All other errors (hardware failures, lockout, etc.) are
                    // handled by the system UI. We let the user retry via the
                    // "Unlock" button on LockScreen.
                }

                override fun onAuthenticationFailed() {
                    // System handles retry count and temporary lockout automatically.
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_prompt_title))
            .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
            // DEVICE_CREDENTIAL is set here, so setNegativeButtonText must NOT be called.
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        prompt.authenticate(promptInfo)
    }
}
