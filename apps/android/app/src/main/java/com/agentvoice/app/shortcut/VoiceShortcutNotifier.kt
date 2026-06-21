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

class VoiceShortcutNotifier(private val context: Context) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "AgentVoice shortcuts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "User-triggered AgentVoice shortcuts"
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun showDrivingShortcut() {
        ensureChannel()

        val openIntent = shortcutIntent(ACTION_OPEN_DRIVING)
        val talkIntent = shortcutIntent(ACTION_START_LISTENING)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_agentvoice_notification)
            .setContentTitle("Talk to AgentVoice")
            .setContentText("Starts a visible hands-free driving session.")
            .setContentIntent(pendingActivity(openIntent, REQUEST_OPEN))
            .addAction(
                R.drawable.ic_agentvoice_notification,
                "Talk",
                pendingActivity(talkIntent, REQUEST_TALK)
            )
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun hideDrivingShortcut() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun shortcutIntent(action: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

    private fun pendingActivity(intent: Intent, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val ACTION_OPEN_DRIVING = "com.agentvoice.app.action.OPEN_DRIVING"
        const val ACTION_START_LISTENING = "com.agentvoice.app.action.START_LISTENING"

        private const val CHANNEL_ID = "agentvoice_shortcuts"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN = 2001
        private const val REQUEST_TALK = 2002
    }
}
