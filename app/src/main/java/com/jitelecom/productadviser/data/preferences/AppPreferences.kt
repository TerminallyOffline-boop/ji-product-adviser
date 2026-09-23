package com.jitelecom.productadviser.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("app_preferences")

data class AppSettings(
    val darkMode: Boolean = false,
    val remoteDatabaseUrl: String = "",
    val allowAboveBudgetPercent: Int = 10,
    val adminPinConfigured: Boolean = false
)

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val dark = booleanPreferencesKey("dark_mode")
        val remoteUrl = stringPreferencesKey("remote_database_url")
        val aboveBudget = intPreferencesKey("above_budget_percent")
        val pinHash = stringPreferencesKey("admin_pin_hash")
    }
    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(prefs[Keys.dark] ?: false, prefs[Keys.remoteUrl] ?: "", prefs[Keys.aboveBudget] ?: 10, prefs[Keys.pinHash] != null)
    }
    suspend fun setDarkMode(value: Boolean) = context.dataStore.edit { it[Keys.dark] = value }
    suspend fun setRemoteUrl(value: String) = context.dataStore.edit { it[Keys.remoteUrl] = value.trim() }
    suspend fun setAboveBudgetPercent(value: Int) = context.dataStore.edit { it[Keys.aboveBudget] = value.coerceIn(0, 25) }
    suspend fun setAdminPin(pin: String) { require(pin.length >= 4); context.dataStore.edit { it[Keys.pinHash] = hash(pin) } }
    suspend fun verifyAdminPin(pin: String): Boolean {
        val saved = context.dataStore.data.first()[Keys.pinHash]
        return saved?.let { it == hash(pin) } ?: (pin == DEFAULT_DEMO_PIN)
    }
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest((SALT + value).toByteArray()).joinToString("") { "%02x".format(it) }
    companion object {
        const val DEFAULT_DEMO_PIN = "2468"
        private const val SALT = "ji-product-adviser-local-v1:"
    }
}
