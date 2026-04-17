package com.pollo.scrobbler.ui.screens.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pollo.scrobbler.data.local.AppPreferences
import com.pollo.scrobbler.domain.repository.DiscogsRepository
import com.pollo.scrobbler.domain.repository.LastFmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isDiscogsAuthenticated: Boolean = false,
    val isLastFmAuthenticated: Boolean = false,
    val discogsUsername: String? = null,
    val lastFmUsername: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingLastFmStep: Boolean = false,
    val showTechInfo: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val discogsRepository: DiscogsRepository,
    private val lastFmRepository: LastFmRepository,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferences.showTechInfo.collect { value ->
                _state.value = _state.value.copy(showTechInfo = value)
            }
        }
        checkAuthStatus()
    }

    fun setShowTechInfo(enabled: Boolean) {
        appPreferences.setShowTechInfo(enabled)
    }

    fun checkAuthStatus() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isDiscogsAuthenticated = discogsRepository.isAuthenticated(),
                isLastFmAuthenticated = lastFmRepository.isAuthenticated(),
                discogsUsername = discogsRepository.getCurrentUsername(),
                lastFmUsername = lastFmRepository.getCurrentUsername()
            )
        }
    }

    fun loginWithDiscogs() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val url = discogsRepository.getRequestTokenUrl()
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to start Discogs login: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun handleDiscogsCallback(oauthToken: String, oauthVerifier: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val success = discogsRepository.handleOAuthCallback(oauthToken, oauthVerifier)
            if (success) {
                checkAuthStatus()
                if (!lastFmRepository.isAuthenticated()) {
                    _state.value = _state.value.copy(pendingLastFmStep = true)
                }
            } else {
                _state.value = _state.value.copy(error = "Discogs authentication failed")
            }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun loginWithLastFm() {
        val url = lastFmRepository.getAuthUrl()
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun handleLastFmCallback(token: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val success = lastFmRepository.handleAuthCallback(token)
            if (success) {
                checkAuthStatus()
                _state.value = _state.value.copy(pendingLastFmStep = false)
            } else {
                _state.value = _state.value.copy(error = "Last.fm authentication failed")
            }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun skipLastFm() {
        _state.value = _state.value.copy(pendingLastFmStep = false)
    }

    fun logoutDiscogs() {
        discogsRepository.logout()
        checkAuthStatus()
    }

    fun logoutLastFm() {
        lastFmRepository.logout()
        checkAuthStatus()
    }
}
