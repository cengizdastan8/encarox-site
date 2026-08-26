package com.encarox.kodo.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.encarox.kodo.data.CodeNotifier
import com.encarox.kodo.data.gmail.GmailClient
import com.encarox.kodo.data.model.CodeSource
import com.encarox.kodo.data.prefs.SettingsStore
import com.encarox.kodo.data.repository.CodeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Periodically scans every enabled Gmail account for new verification emails,
 * stores any codes found and notifies the user. Runs fully on-device.
 */
@HiltWorker
class GmailSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val gmailClient: GmailClient,
    private val repository: CodeRepository,
    private val settings: SettingsStore,
    private val notifier: CodeNotifier
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val accounts = settings.accounts.first().filter { it.enabled }
        val notify = settings.notificationsEnabled.first()

        var anyError = false
        for (account in accounts) {
            try {
                val messages = gmailClient.fetchRecent(account.email)
                for (msg in messages) {
                    val stored = repository.ingest(
                        rawMessage = msg.body,
                        source = CodeSource.GMAIL,
                        sender = msg.from,
                        account = account.email,
                        receivedAt = msg.receivedAt,
                        externalId = msg.id
                    )
                    if (stored != null && notify) notifier.notifyCode(stored)
                }
            } catch (t: Throwable) {
                anyError = true
            }
        }

        // Retention cleanup.
        runCatching { repository.purgeOlderThan(settings.retentionDays.first()) }

        if (anyError && runAttemptCount < 3) Result.retry() else Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "kodo_gmail_sync"

        /** Schedules (or reschedules) the periodic sync. */
        fun schedule(context: Context, intervalMinutes: Int) {
            val request = PeriodicWorkRequestBuilder<GmailSyncWorker>(
                intervalMinutes.toLong().coerceAtLeast(15), TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
