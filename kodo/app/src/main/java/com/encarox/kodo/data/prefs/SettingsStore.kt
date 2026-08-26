package com.encarox.kodo.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.encarox.kodo.data.model.GmailAccountInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kodo_settings")

/**
 * User settings + the list of connected Gmail accounts, backed by DataStore.
 */
@Singleton
class SettingsStore @Inject constructor(
    private val appContext: Context
) {
    private object Keys {
        val ACCOUNTS = stringSetPreferencesKey("gmail_accounts")
        val DISABLED_ACCOUNTS = stringSetPreferencesKey("disabled_gmail_accounts")
        val AUTO_COPY = booleanPreferencesKey("auto_copy_latest")
        val SYNC_MINUTES = intPreferencesKey("sync_interval_minutes")
        val RETENTION_DAYS = intPreferencesKey("retention_days")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NOTIFY = booleanPreferencesKey("notifications_enabled")
    }

    val accounts: Flow<List<GmailAccountInfo>> = appContext.dataStore.data.map { prefs ->
        val disabled = prefs[Keys.DISABLED_ACCOUNTS] ?: emptySet()
        (prefs[Keys.ACCOUNTS] ?: emptySet()).sorted().map { email ->
            GmailAccountInfo(email = email, enabled = email !in disabled)
        }
    }

    val syncIntervalMinutes: Flow<Int> =
        appContext.dataStore.data.map { it[Keys.SYNC_MINUTES] ?: DEFAULT_SYNC_MINUTES }

    val retentionDays: Flow<Int> =
        appContext.dataStore.data.map { it[Keys.RETENTION_DAYS] ?: DEFAULT_RETENTION_DAYS }

    val autoCopyLatest: Flow<Boolean> =
        appContext.dataStore.data.map { it[Keys.AUTO_COPY] ?: true }

    val notificationsEnabled: Flow<Boolean> =
        appContext.dataStore.data.map { it[Keys.NOTIFY] ?: true }

    val onboarded: Flow<Boolean> =
        appContext.dataStore.data.map { it[Keys.ONBOARDED] ?: false }

    suspend fun addAccount(email: String) = appContext.dataStore.edit { prefs ->
        prefs[Keys.ACCOUNTS] = (prefs[Keys.ACCOUNTS] ?: emptySet()) + email
    }

    suspend fun removeAccount(email: String) = appContext.dataStore.edit { prefs ->
        prefs[Keys.ACCOUNTS] = (prefs[Keys.ACCOUNTS] ?: emptySet()) - email
        prefs[Keys.DISABLED_ACCOUNTS] = (prefs[Keys.DISABLED_ACCOUNTS] ?: emptySet()) - email
    }

    suspend fun setAccountEnabled(email: String, enabled: Boolean) =
        appContext.dataStore.edit { prefs ->
            val disabled = prefs[Keys.DISABLED_ACCOUNTS] ?: emptySet()
            prefs[Keys.DISABLED_ACCOUNTS] = if (enabled) disabled - email else disabled + email
        }

    suspend fun setSyncInterval(minutes: Int) = appContext.dataStore.edit {
        it[Keys.SYNC_MINUTES] = minutes.coerceIn(15, 720)
    }

    suspend fun setRetentionDays(days: Int) = appContext.dataStore.edit {
        it[Keys.RETENTION_DAYS] = days.coerceIn(1, 90)
    }

    suspend fun setAutoCopy(enabled: Boolean) = appContext.dataStore.edit {
        it[Keys.AUTO_COPY] = enabled
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) = appContext.dataStore.edit {
        it[Keys.NOTIFY] = enabled
    }

    suspend fun setOnboarded(value: Boolean) = appContext.dataStore.edit {
        it[Keys.ONBOARDED] = value
    }

    companion object {
        const val DEFAULT_SYNC_MINUTES = 15
        const val DEFAULT_RETENTION_DAYS = 14
    }
}
