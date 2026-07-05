package net.atom.dpibypass.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.atom.dpibypass.data.ConnectionState
import net.atom.dpibypass.data.ActiveProfile

/**
 * Süreç-içi global bağlantı durumu. Servis günceller; UI (Activity/Compose) ve
 * Quick Settings Tile aynı süreçte olduğundan doğrudan gözlemler.
 */
object VpnState {
    private val _state = MutableStateFlow(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _profile = MutableStateFlow<ActiveProfile?>(null)
    val profile: StateFlow<ActiveProfile?> = _profile.asStateFlow()

    fun update(state: ConnectionState) {
        _state.value = state
    }

    fun update(state: ConnectionState, profile: ActiveProfile?) {
        _state.value = state
        _profile.value = profile
    }

    fun isRunning(): Boolean =
        _state.value == ConnectionState.Connected ||
            _state.value == ConnectionState.Connecting ||
            _state.value == ConnectionState.Testing
}
