package com.example.telephony

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class CallAssistantForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val callerName = intent?.getStringExtra(EXTRA_CALLER_NAME).orEmpty().ifBlank { "Incoming Caller" }
        val phoneNumber = intent?.getStringExtra(EXTRA_PHONE_NUMBER).orEmpty()

        ensureChannels()

        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            101,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            102,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("AI Call Assistant Active")
            .setContentText("$callerName ($phoneNumber)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_NOT_STICKY
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_active),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active AI Call Assistant screening and live call controls"
            }
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "ai_call_assistant_active"
        const val NOTIFICATION_ID = 4001
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
    }
}
