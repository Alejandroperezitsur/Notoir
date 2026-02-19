package com.example.notesapp_apv_czg.security

import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.crypto.Cipher
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class BiometricAuthenticator(
    private val keyManager: SecureKeyManager
) {
    sealed class AuthResult {
        data class Success(val cipher: Cipher) : AuthResult()
        object Failed : AuthResult()
        object Cancelled : AuthResult()
        object LockedOut : AuthResult()
    }

    private var failureCount = 0
    private var lockUntilElapsedRealtime = 0L

    suspend fun authenticateForEncryption(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): AuthResult {
        val cipher = try {
            keyManager.createEncryptCipher()
        } catch (e: Exception) {
            return AuthResult.Failed
        }
        return authenticateInternal(activity, title, subtitle, cipher)
    }

    suspend fun authenticateForDecryption(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        iv: ByteArray
    ): AuthResult {
        val cipher = try {
            keyManager.createDecryptCipher(iv)
        } catch (e: Exception) {
            return AuthResult.Failed
        }
        return authenticateInternal(activity, title, subtitle, cipher)
    }

    private suspend fun authenticateInternal(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cipher: Cipher
    ): AuthResult = suspendCancellableCoroutine { cont ->
        val now = SystemClock.elapsedRealtime()
        if (now < lockUntilElapsedRealtime) {
            cont.resume(AuthResult.LockedOut)
            return@suspendCancellableCoroutine
        }

        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val biometricManager = BiometricManager.from(activity)
        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            cont.resume(AuthResult.Failed)
            return@suspendCancellableCoroutine
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        var finished = false
        val lifecycle = activity.lifecycle
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                if (!finished && cont.isActive) {
                    cont.resume(AuthResult.Cancelled)
                }
                lifecycle.removeObserver(this)
            }
        }
        lifecycle.addObserver(observer)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authCipher = result.cryptoObject?.cipher
                    if (!finished && cont.isActive) {
                        finished = true
                        if (authCipher != null) {
                            failureCount = 0
                            cont.resume(AuthResult.Success(authCipher))
                        } else {
                            cont.resume(AuthResult.Failed)
                        }
                    }
                    lifecycle.removeObserver(observer)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (!finished && cont.isActive) {
                        finished = true
                        val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        if (cancelled) {
                            cont.resume(AuthResult.Cancelled)
                        } else {
                            failureCount += 1
                            if (failureCount >= 3) {
                                lockUntilElapsedRealtime = SystemClock.elapsedRealtime() + 10_000L
                                cont.resume(AuthResult.LockedOut)
                            } else {
                                cont.resume(AuthResult.Failed)
                            }
                        }
                    }
                    lifecycle.removeObserver(observer)
                }
            }
        )

        cont.invokeOnCancellation {
            prompt.cancelAuthentication()
            lifecycle.removeObserver(observer)
        }

        try {
            val cryptoObject = BiometricPrompt.CryptoObject(cipher)
            prompt.authenticate(promptInfo, cryptoObject)
        } catch (e: Exception) {
            if (!finished && cont.isActive) {
                finished = true
                cont.resumeWithException(e)
            }
            lifecycle.removeObserver(observer)
        }
    }
}
