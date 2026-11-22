package com.example.racingbrothers.audio

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Lightweight zero-asset arcade sound cues using Android ToneGenerator.
 * Fails gracefully in headless/JVM test environments and releases resources cleanly.
 */
class ArcadeSoundManager {
    @Volatile
    var enabled: Boolean = true
    @Volatile
    var volume: Int = 65

    private var toneGenerator: ToneGenerator? = null
    private var activeVolume = -1

    @Synchronized
    private fun getToneGen(): ToneGenerator? {
        val targetVolume = volume.coerceIn(0, 100)
        if (!enabled || targetVolume == 0) return null
        if (activeVolume != targetVolume) release()
        if (toneGenerator == null) {
            toneGenerator = runCatching {
                ToneGenerator(AudioManager.STREAM_MUSIC, targetVolume)
            }.getOrNull()
            activeVolume = targetVolume
        }
        return toneGenerator
    }

    fun playUiClick() {
        runCatching { getToneGen()?.startTone(ToneGenerator.TONE_PROP_BEEP, 45) }
    }

    fun playCountdownTick(isGo: Boolean) {
        val tone = if (isGo) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_ACK
        val duration = if (isGo) 280 else 130
        runCatching { getToneGen()?.startTone(tone, duration) }
    }

    fun playCheckpoint() {
        runCatching { getToneGen()?.startTone(ToneGenerator.TONE_PROP_PROMPT, 100) }
    }

    fun playFinishFanfare() {
        runCatching { getToneGen()?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 380) }
    }

    @Synchronized
    fun release() {
        runCatching { toneGenerator?.release() }
        toneGenerator = null
    }
}
