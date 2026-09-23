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
    val adminPinConfigured: Boolean = false,
    val favoriteProductIds: List<Long> = emptyList(),
    val recentProductIds: List<Long> = emptyList()
)

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val dark = booleanPreferencesKey("dark_mode")
        val remoteUrl = stringPreferencesKey("remote_database_url")
        val aboveBudget = intPreferencesKey("above_budget_percent")
        val pinHash = stringPreferencesKey("admin_pin_hash")
        val favorites = stringPreferencesKey("favorite_product_ids")
        val recents = stringPreferencesKey("recent_product_ids")
    }
    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            prefs[Keys.dark] ?: false,
            prefs[Keys.remoteUrl] ?: "",
            prefs[Keys.aboveBudget] ?: 10,
            prefs[Keys.pinHash] != null,
            decodeIds(prefs[Keys.favorites]),
            decodeIds(prefs[Keys.recents])
        )
    }
    suspend fun setDarkMode(value: Boolean) = context.dataStore.edit { it[Keys.dark] = value }
    suspend fun setRemoteUrl(value: String) {
        val normalized=value.trim()
        require(normalized.isBlank() || normalized.startsWith("https://", ignoreCase=true)) { "The remote manifest must use HTTPS." }
        context.dataStore.edit { it[Keys.remoteUrl] = normalized }
    }
    suspend fun setAboveBudgetPercent(value: Int) = context.dataStore.edit { it[Keys.aboveBudget] = value.coerceIn(0, 25) }
    suspend fun toggleFavorite(productId: Long) = context.dataStore.edit { prefs ->
        val current = decodeIds(prefs[Keys.favorites])
        val updated = if (productId in current) current - productId else (current + productId).takeLast(5)
        prefs[Keys.favorites] = updated.joinToString(",")
    }
    suspend fun recordViewed(productId: Long) = context.dataStore.edit { prefs ->
        prefs[Keys.recents] = (listOf(productId) + decodeIds(prefs[Keys.recents]).filterNot { it == productId }).take(5).joinToString(",")
    }
    suspend fun clearProductHistory() = context.dataStore.edit { prefs -> prefs.remove(Keys.favorites);prefs.remove(Keys.recents) }
    suspend fun setAdminPin(pin: String) { require(pin.length >= 4); context.dataStore.edit { it[Keys.pinHash] = hash(pin) } }
    suspend fun verifyAdminPin(pin: String): Boolean {
        val saved = context.dataStore.data.first()[Keys.pinHash]
        return saved?.let { it == hash(pin) } ?: false
    }
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest((SALT + value).toByteArray()).joinToString("") { "%02x".format(it) }
    private fun decodeIds(value: String?): List<Long> = value.orEmpty().split(',').mapNotNull(String::toLongOrNull)
    companion object {
        private const val SALT = "ji-product-adviser-local-v1:"
    }
}
