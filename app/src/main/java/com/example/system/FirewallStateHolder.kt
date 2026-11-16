package com.example.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FirewallRuntimeState(
    val isVpnActive: Boolean = false,
    val isShieldMasterEnabled: Boolean = true,
    val activeInternetBlockedCount: Int = 0,
    val activeMemoryBlockedCount: Int = 0,
    val blockedPacketsSession: Long = 0L,
    val blockedBytesSession: Long = 0L,
    val lastBlockedSummary: String = "",
    val lastFreedMb: Long = 0L,
    val totalMemorySweeps: Int = 0
)

object FirewallStateHolder {
    private val _state = MutableStateFlow(FirewallRuntimeState())
    val state: StateFlow<FirewallRuntimeState> = _state.asStateFlow()

    fun setVpnActive(active: Boolean) {
        _state.update { it.copy(isVpnActive = active) }
    }

    fun setMasterEnabled(enabled: Boolean) {
        _state.update { it.copy(isShieldMasterEnabled = enabled) }
    }

    fun updateRuleCounts(internetCount: Int, memoryCount: Int) {
        _state.update {
            it.copy(
                activeInternetBlockedCount = internetCount,
                activeMemoryBlockedCount = memoryCount
            )
        }
    }

    fun recordBlockedPacket(bytes: Int, protocolLabel: String) {
        _state.update {
            it.copy(
                blockedPacketsSession = it.blockedPacketsSession + 1L,
                blockedBytesSession = it.blockedBytesSession + bytes.toLong(),
                lastBlockedSummary = protocolLabel
            )
        }
    }

    fun recordMemorySweep(freedMb: Long) {
        _state.update {
            it.copy(
                lastFreedMb = freedMb,
                totalMemorySweeps = it.totalMemorySweeps + 1
            )
        }
    }
}
