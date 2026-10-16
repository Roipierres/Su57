package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.ToneGenerator
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * SoundManager handles real-time audio effects for engine hum,
 * combat explosions, laser fire, and UI keypad clicks using
 * Android Audio synthesis (AudioTrack & ToneGenerator).
 */
class SoundManager(private val context: Context) {

    private var toneGen: ToneGenerator? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var engineHumJob: Job? = null
    private var isEngineHumPlaying = false

    var isSoundEnabled: Boolean = true

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Plays a crisp UI Key Click sound when typing characters in password input.
     */
    fun playUiClickSound() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Plays an explosion sound effect when an enemy jet is destroyed.
     */
    fun playExplosionSound() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                // Synthesize low frequency rumble for explosion
                val sampleRate = 22050
                val numSamples = sampleRate / 3 // ~330ms duration
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val freq = 120.0 - (progress * 80.0) // Frequency drop
                    val sample = (sin(2.0 * Math.PI * i.toDouble() * freq / sampleRate) * Short.MAX_VALUE * (1.0 - progress)).toInt()
                    buffer[i] = sample.toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                delay(350)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Plays laser plasma fire sound effect.
     */
    fun playLaserSound() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                toneGen?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 25)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Starts background Su-57 Jet Engine Hum sound.
     */
    fun startEngineHum() {
        if (isEngineHumPlaying || !isSoundEnabled) return
        isEngineHumPlaying = true

        engineHumJob = scope.launch {
            try {
                val sampleRate = 11025
                val numSamples = sampleRate / 2
                val buffer = ShortArray(numSamples)

                // Engine low pitch hum frequency
                for (i in 0 until numSamples) {
                    val sample = (sin(2.0 * Math.PI * i.toDouble() * 90.0 / sampleRate) * (Short.MAX_VALUE * 0.15)).toInt()
                    buffer[i] = sample.toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.setLoopPoints(0, numSamples - 1, -1)
                audioTrack.play()

                while (isActive && isEngineHumPlaying) {
                    delay(500)
                }

                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Stops Jet Engine Hum sound.
     */
    fun stopEngineHum() {
        isEngineHumPlaying = false
        engineHumJob?.cancel()
        engineHumJob = null
    }

    fun release() {
        stopEngineHum()
        try {
            toneGen?.release()
            toneGen = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
