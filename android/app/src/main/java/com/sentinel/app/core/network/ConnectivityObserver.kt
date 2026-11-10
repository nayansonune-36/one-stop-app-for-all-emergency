package com.sentinel.app.core.network

import kotlinx.coroutines.flow.StateFlow

interface ConnectivityObserver {
    /**
     * Emits true when an active internet connection is detected, false when offline.
     */
    val isOnline: StateFlow<Boolean>
}
