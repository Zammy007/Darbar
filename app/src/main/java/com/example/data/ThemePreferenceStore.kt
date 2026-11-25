package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "darbar_settings")

class ThemePreferenceStore(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")

    fun observeDarkMode(systemDefault: Boolean): Flow<Boolean> =
        context.themeDataStore.data.map { prefs ->
            prefs[darkModeKey] ?: systemDefault
        }

    suspend fun setDarkMode(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[darkModeKey] = enabled
        }
    }
}
