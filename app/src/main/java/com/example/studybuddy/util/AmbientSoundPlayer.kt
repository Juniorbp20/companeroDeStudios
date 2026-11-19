package com.example.studybuddy.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sonidos ambientales para el modo enfoque, sintetizados en tiempo real
 * (sin archivos de audio): lluvia (ruido rosa modulado), ruido blanco y ruido marrón.
 */
object AmbientSoundPlayer {

    enum class Type { NONE, RAIN, WHITE, BROWN }

    private const val PREFS = "focus_prefs"
    private const val KEY_AMBIENT = "ambient_sound"
    private const val SAMPLE_RATE = 22_050
    private const val VOLUME = 0.22f

    @Volatile private var running = false
    private var thread: Thread? = null

    fun getType(context: Context): Type =
        runCatching {
            Type.valueOf(
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_AMBIENT, Type.NONE.name) ?: Type.NONE.name
            )
        }.getOrDefault(Type.NONE)

    fun setType(context: Context, type: Type) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_AMBIENT, type.name).apply()
    }

    val isPlaying: Boolean get() = running

    @Synchronized
    fun start(type: Type) {
        stop()
        if (type == Type.NONE) return
        running = true
        thread = Thread({ loop(type) }, "ambient-sound").apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }

    private fun loop(type: Type) {
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            running = false
            return
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuffer * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        try {
            track.play()
            val buffer = ShortArray(minBuffer)
            val generator = NoiseGenerator(type)
            while (running) {
                generator.fill(buffer)
                track.write(buffer, 0, buffer.size)
            }
        } catch (_: Exception) {
            running = false
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    /** Generador de muestras; separado para poder probarlo sin audio real. */
    internal class NoiseGenerator(private val type: Type, seed: Int = 7) {
        private val random = Random(seed)
        private var b0 = 0.0
        private var b1 = 0.0
        private var b2 = 0.0
        private var brown = 0.0
        private var phase = 0.0

        fun next(): Double {
            val white = random.nextDouble(-1.0, 1.0)
            return when (type) {
                Type.WHITE -> white * 0.5
                Type.BROWN -> {
                    brown = (brown + 0.02 * white) / 1.02
                    brown * 3.5
                }
                Type.RAIN -> {
                    // Ruido rosa (filtro de Paul Kellet) con un leve vaivén de intensidad.
                    b0 = 0.99765 * b0 + white * 0.0990460
                    b1 = 0.96300 * b1 + white * 0.2965164
                    b2 = 0.57000 * b2 + white * 1.0526913
                    val pink = (b0 + b1 + b2 + white * 0.1848) * 0.18
                    phase += 2 * Math.PI * 0.15 / SAMPLE_RATE
                    pink * (0.8 + 0.2 * abs(sin(phase)))
                }
                Type.NONE -> 0.0
            }
        }

        fun fill(buffer: ShortArray) {
            for (i in buffer.indices) {
                val v = (next() * VOLUME).coerceIn(-1.0, 1.0)
                buffer[i] = (v * Short.MAX_VALUE).toInt().toShort()
            }
        }
    }
}
