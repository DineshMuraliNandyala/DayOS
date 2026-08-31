package com.lifeos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeos.data.db.LifeOSDatabase
import com.lifeos.data.repository.SettingsRepository
import com.lifeos.ui.theme.AccentDomain
import com.lifeos.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Process-scoped ViewModel that drives the top-level theme.
 * Owned by MainActivity and lives as long as the Activity's ViewModelStore.
 */
class MainViewModel(private val repo: SettingsRepository) : ViewModel() {

    val appTheme = repo.settingsFlow
        .map { settings ->
            when (settings?.theme) {
                "amoled" -> AppTheme.AMOLED
                "light" -> AppTheme.LIGHT
                else -> AppTheme.DARK
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.DARK)

    val accentDomain = repo.settingsFlow
        .map { settings ->
            when (settings?.accentDomain) {
                "energy" -> AccentDomain.ENERGY
                "calm" -> AccentDomain.CALM
                else -> AccentDomain.FOCUS
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentDomain.FOCUS)

    class Factory(private val db: LifeOSDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(SettingsRepository(db.settingsDao())) as T
    }
}
