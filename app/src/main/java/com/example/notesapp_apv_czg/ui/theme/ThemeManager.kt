package com.example.notesapp_apv_czg.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore("settings")
private val KEY_THEME_NAME = stringPreferencesKey("theme_name")

object ThemeManager {
    private val currentScheme = mutableStateOf(predefinedSchemes[0])
    
    fun getCurrentScheme(): ColorSchemeOption = currentScheme.value

    suspend fun load(context: Context) {
        val prefs = context.themeDataStore.data.first()
        val savedName = prefs[KEY_THEME_NAME]
        val scheme = predefinedSchemes.find { it.name == savedName } ?: predefinedSchemes[0]
        currentScheme.value = scheme
    }
    
    suspend fun setColorScheme(context: Context, scheme: ColorSchemeOption) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_THEME_NAME] = scheme.name
        }
        currentScheme.value = scheme
    }
    
    @Composable
    fun getColorScheme(darkTheme: Boolean): ColorScheme {
        return if (darkTheme) {
            darkColorScheme(
                primary = DarkPrimary,
                onPrimary = DarkOnPrimary,
                secondary = DarkSecondary,
                onSecondary = DarkOnSecondary,
                background = DarkBackground,
                onBackground = DarkOnBackground,
                surface = DarkSurface,
                onSurface = DarkOnSurface,
                error = DarkError,
                onError = DarkOnError
            )
        } else {
            lightColorScheme(
                primary = LightPrimary,
                onPrimary = LightOnPrimary,
                secondary = LightSecondary,
                onSecondary = LightOnSecondary,
                background = LightBackground,
                onBackground = LightOnBackground,
                surface = LightSurface,
                onSurface = LightOnSurface,
                error = LightError,
                onError = LightOnError
            )
        }
    }
}

// Local composition para acceder al ThemeManager desde cualquier parte de la app
val LocalThemeManager = staticCompositionLocalOf { ThemeManager }
