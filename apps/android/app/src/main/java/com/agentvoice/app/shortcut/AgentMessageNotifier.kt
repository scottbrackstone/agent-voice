package com.agentvoice.app.shortcut

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.agentvoice.app.MainActivity
import com.agentvoice.app.R
import kotlin.math.absoluteValue

class AgentMessageNotifier(private val context: Context) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Solara messages",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Messages delivered to AgentVoice from Solara and Hermes"
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun showMessage(title: String, text: String) {
        ensureChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_agentvoice_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(text), notification)
    }

    private fun notificationId(text: String): Int =
        (NOTIFICATION_ID_BASE + text.hashCode().absoluteValue).coerceAtLeast(NOTIFICATION_ID_BASE)

    companion object {
        private const val CHANNEL_ID = "agentvoice_messages"
        private const val REQUEST_OPEN = 2101
        private const val NOTIFICATION_ID_BASE = 3000
    }
}
