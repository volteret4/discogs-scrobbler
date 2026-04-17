package com.pollo.scrobbler.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _showTechInfo = MutableStateFlow(prefs.getBoolean("show_tech_info", false))
    val showTechInfo: StateFlow<Boolean> = _showTechInfo.asStateFlow()

    fun setShowTechInfo(value: Boolean) {
        prefs.edit().putBoolean("show_tech_info", value).apply()
        _showTechInfo.value = value
    }
}
