package com.example.notesapp_apv_czg.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class VaultState {
    object Locked : VaultState()
    object Unlocked : VaultState()
}

class AppLockManager(
    private val timeoutMillis: Long = 60_000L,
    private val requireAuthOnLaunch: Boolean = true
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _vaultState = MutableStateFlow<VaultState>(
        if (requireAuthOnLaunch) VaultState.Locked else VaultState.Unlocked
    )
    val vaultState: StateFlow<VaultState> = _vaultState.asStateFlow()

    private var timeoutJob: Job? = null
    private var appInForeground = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        appInForeground = true
        if (_vaultState.value is VaultState.Unlocked) {
            scheduleTimeout()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        appInForeground = false
        if (requireAuthOnLaunch) {
            lockNow()
        }
        cancelTimeout()
    }

    fun onAuthenticated() {
        _vaultState.value = VaultState.Unlocked
        if (appInForeground) {
            scheduleTimeout()
        }
    }

    fun lockNow() {
        _vaultState.value = VaultState.Locked
        cancelTimeout()
    }

    private fun scheduleTimeout() {
        cancelTimeout()
        if (timeoutMillis <= 0L) return
        timeoutJob = scope.launch {
            delay(timeoutMillis)
            if (appInForeground) {
                lockNow()
            }
        }
    }

    private fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    fun dispose() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        scope.cancel()
    }
}

