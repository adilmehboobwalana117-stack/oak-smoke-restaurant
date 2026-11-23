package com.example.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

object LocalNotificationHelper {
    private const val CHANNEL_UPDATES_ID = "oak_smoke_updates"
    private const val CHANNEL_UPDATES_NAME = "Oak & Smoke Live Updates"

    private const val CHANNEL_ORDER_READY_SOUND_ID = "oak_smoke_order_ready_sound"
    private const val CHANNEL_ORDER_READY_SOUND_NAME = "Order Ready & Rider Sound Alerts"

    private val VIBRATION_PATTERN = longArrayOf(0L, 350L, 180L, 350L)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES_ID,
                CHANNEL_UPDATES_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time COD order status, reservation updates, daily specials, and staff alerts."
                setSound(soundUri, audioAttrs)
                enableVibration(true)
            }

            val readySoundChannel = NotificationChannel(
                CHANNEL_ORDER_READY_SOUND_ID,
                CHANNEL_ORDER_READY_SOUND_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Audible sound & vibration alerts sent to Riders and Customers when an order is Ready."
                val alertAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(soundUri, alertAttrs)
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
            }

            manager.createNotificationChannel(updatesChannel)
            manager.createNotificationChannel(readySoundChannel)
        }
    }

    fun playAlertSound(context: Context, isOrderReady: Boolean = true) {
        try {
            val uri = RingtoneManager.getDefaultUri(
                if (isOrderReady) RingtoneManager.TYPE_NOTIFICATION else RingtoneManager.TYPE_NOTIFICATION
            ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            if (ringtone != null) {
                ringtone.play()
                if (isOrderReady) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            if (ringtone.isPlaying) ringtone.stop()
                        } catch (_: Exception) {
                        }
                    }, 3000L)
                }
                return
            }
        } catch (_: Exception) {
            // Fall through to ToneGenerator fallback
        }
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
            tone.startTone(
                if (isOrderReady) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP,
                450
            )
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    tone.release()
                } catch (_: Exception) {
                }
            }, 600L)
        } catch (_: Exception) {
            // Device audio unavailable
        }
    }

    fun showNotification(
        context: Context,
        id: Int,
        title: String,
        message: String,
        withSoundAlert: Boolean = false
    ) {
        ensureChannel(context)

        // Always play immediate audible alert in foreground when Order Ready / Rider Dispatch arrives
        if (withSoundAlert) {
            playAlertSound(context, isOrderReady = true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val channelId = if (withSoundAlert) CHANNEL_ORDER_READY_SOUND_ID else CHANNEL_UPDATES_ID
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_flame)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (withSoundAlert) NotificationCompat.CATEGORY_EVENT else NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setSound(soundUri)

        if (withSoundAlert) {
            builder.setVibrate(VIBRATION_PATTERN)
        }

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }
}
