package com.example.studybuddy

import com.example.studybuddy.util.AmbientSoundPlayer
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientNoiseTest {

    private fun samples(type: AmbientSoundPlayer.Type): ShortArray {
        val buffer = ShortArray(22_050)
        AmbientSoundPlayer.NoiseGenerator(type).fill(buffer)
        return buffer
    }

    @Test
    fun noiseIsAudibleButQuiet() {
        for (type in listOf(AmbientSoundPlayer.Type.RAIN, AmbientSoundPlayer.Type.WHITE, AmbientSoundPlayer.Type.BROWN)) {
            val buf = samples(type)
            val peak = buf.maxOf { kotlin.math.abs(it.toInt()) }
            assertTrue("$type debería sonar", peak > 200)
            // Volumen moderado: nunca satura.
            assertTrue("$type no debería saturar", peak < Short.MAX_VALUE * 0.6)
        }
    }

    @Test
    fun silenceIsSilent() {
        assertTrue(samples(AmbientSoundPlayer.Type.NONE).all { it.toInt() == 0 })
    }
}
