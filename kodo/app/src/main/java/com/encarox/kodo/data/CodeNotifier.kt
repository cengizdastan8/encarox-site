package com.encarox.kodo.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.encarox.kodo.MainActivity
import com.encarox.kodo.R
import com.encarox.kodo.data.model.CodeItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts a rich notification the moment a code is captured, with a one-tap
 * "Copy" action so the user never has to open the app.
 */
@Singleton
class CodeNotifier @Inject constructor(
    private val appContext: Context
) {
    init { ensureChannel() }

    fun notifyCode(item: CodeItem) {
        val nm = appContext.getSystemService<NotificationManager>() ?: return

        val copyIntent = Intent(appContext, CopyCodeReceiver::class.java).apply {
            putExtra(CopyCodeReceiver.EXTRA_CODE, item.code)
        }
        val copyPending = PendingIntent.getBroadcast(
            appContext, item.code.hashCode(), copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openPending = PendingIntent.getActivity(
            appContext, 0, Intent(appContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = buildString {
            append(item.code)
            item.service?.let { append("  •  $it") }
        }
        val sourceLabel = when (item.source.name) {
            "GMAIL" -> item.account ?: "Gmail"
            else -> item.sender
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_kodo_notification)
            .setContentTitle(title)
            .setContentText(appContext.getString(R.string.notif_from, sourceLabel))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(
                R.drawable.ic_copy,
                appContext.getString(R.string.action_copy),
                copyPending
            )
            .build()

        nm.notify(item.code.hashCode(), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = appContext.getSystemService<NotificationManager>() ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.channel_codes),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = appContext.getString(R.string.channel_codes_desc) }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "captured_codes"
    }
}

/** Copies a code to the clipboard directly from the notification action. */
class CopyCodeReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val code = intent.getStringExtra(EXTRA_CODE) ?: return
        val clipboard = context.getSystemService<ClipboardManager>() ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Kodo code", code))
    }

    companion object {
        const val EXTRA_CODE = "extra_code"
    }
}
