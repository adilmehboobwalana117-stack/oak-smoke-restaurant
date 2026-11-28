package com.example.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Continuous ringing alarm controller for incoming unaccepted restaurant orders.
 * Loops a loud ringtone/alarm audio stream + subtle vibration pulse until explicitly stopped
 * (when Admin/Staff accepts or rejects the order, mutes the alert, or the 3-minute timer expires).
 */
object IncomingOrderAlarmController {
    const val DEFAULT_ALARM_DURATION_SECONDS = 180 // 3 minutes

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeRingtone: Ringtone? = null
    private var toneGenerator: ToneGenerator? = null
    private var activeVibrator: Vibrator? = null
    private var isRinging = false
    private var watchdogRunnable: Runnable? = null

    fun startContinuousAlarm(context: Context) {
        val appContext = context.applicationContext
        mainHandler.post {
            if (isRinging) return@post
            isRinging = true
            startRingtoneOrTone(appContext)
            scheduleWatchdog(appContext)
        }
    }

    fun stopAlarm() {
        mainHandler.post {
            isRinging = false
            watchdogRunnable?.let { mainHandler.removeCallbacks(it) }
            watchdogRunnable = null
            stopAudioAndVibrationInternal()
        }
    }

    private fun startRingtoneOrTone(appContext: Context) {
        stopAudioAndVibrationInternal()
        var startedRingtone = false
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(appContext, uri)
                if (ringtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ringtone.audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone.isLooping = true
                    }
                    ringtone.play()
                    activeRingtone = ringtone
                    startedRingtone = true
                }
            }
        } catch (_: Exception) {
            startedRingtone = false
        }

        if (!startedRingtone) {
            playFallbackAlarmTone()
        }
        triggerVibrationPulse(appContext)
    }

    private fun scheduleWatchdog(appContext: Context) {
        watchdogRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = object : Runnable {
            override fun run() {
                if (!isRinging) return
                val ringtone = activeRingtone
                if (ringtone != null) {
                    try {
                        if (!ringtone.isPlaying) {
                            ringtone.play()
                        }
                    } catch (_: Exception) {
                        playFallbackAlarmTone()
                    }
                } else {
                    playFallbackAlarmTone()
                }
                triggerVibrationPulse(appContext)
                mainHandler.postDelayed(this, 2200L)
            }
        }
        watchdogRunnable = runnable
        mainHandler.postDelayed(runnable, 2200L)
    }

    private fun playFallbackAlarmTone() {
        try {
            val tg = toneGenerator ?: ToneGenerator(AudioManager.STREAM_ALARM, 100).also {
                toneGenerator = it
            }
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900)
        } catch (_: Exception) {
            // Audio stream unavailable on device
        }
    }

    @Suppress("DEPRECATION")
    private fun triggerVibrationPulse(appContext: Context) {
        try {
            val vibrator = activeVibrator ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }.also { activeVibrator = it }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0L, 400L, 200L, 400L), -1)
                    )
                } else {
                    vibrator.vibrate(longArrayOf(0L, 400L, 200L, 400L), -1)
                }
            }
        } catch (_: Exception) {
            // Vibrator unavailable or restricted
        }
    }

    private fun stopAudioAndVibrationInternal() {
        try {
            activeRingtone?.let { r ->
                if (r.isPlaying) r.stop()
            }
        } catch (_: Exception) {
        }
        activeRingtone = null

        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (_: Exception) {
        }
        toneGenerator = null

        try {
            activeVibrator?.cancel()
        } catch (_: Exception) {
        }
    }

    fun formatCountdown(seconds: Int): String {
        val clamped = seconds.coerceAtLeast(0)
        val mins = clamped / 60
        val secs = clamped % 60
        return "%02d:%02d".format(mins, secs)
    }
}
