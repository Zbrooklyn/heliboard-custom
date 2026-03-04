// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WavEncoderTest {

    @Test
    fun `empty audio produces valid WAV header`() {
        val wav = WavEncoder.encodeWaveBytes(ShortArray(0))
        // WAV header is 44 bytes
        assertEquals(44, wav.size)
        // RIFF header
        assertEquals('R'.code.toByte(), wav[0])
        assertEquals('I'.code.toByte(), wav[1])
        assertEquals('F'.code.toByte(), wav[2])
        assertEquals('F'.code.toByte(), wav[3])
        // WAVE format
        assertEquals('W'.code.toByte(), wav[8])
        assertEquals('A'.code.toByte(), wav[9])
        assertEquals('V'.code.toByte(), wav[10])
        assertEquals('E'.code.toByte(), wav[11])
    }

    @Test
    fun `wav output size matches input`() {
        val samples = ShortArray(16000) // 1 second at 16kHz
        val wav = WavEncoder.encodeWaveBytes(samples)
        // 44 byte header + 2 bytes per sample
        assertEquals(44 + 16000 * 2, wav.size)
    }

    @Test
    fun `wav header has correct sample rate`() {
        val wav = WavEncoder.encodeWaveBytes(ShortArray(100))
        // Sample rate at bytes 24-27 (little-endian), should be 16000 = 0x3E80
        val sampleRate = (wav[24].toInt() and 0xFF) or
            ((wav[25].toInt() and 0xFF) shl 8) or
            ((wav[26].toInt() and 0xFF) shl 16) or
            ((wav[27].toInt() and 0xFF) shl 24)
        assertEquals(16000, sampleRate)
    }

    @Test
    fun `wav header has mono channel`() {
        val wav = WavEncoder.encodeWaveBytes(ShortArray(100))
        // Channels at bytes 22-23 (little-endian), should be 1
        val channels = (wav[22].toInt() and 0xFF) or ((wav[23].toInt() and 0xFF) shl 8)
        assertEquals(1, channels)
    }

    @Test
    fun `wav header has 16-bit samples`() {
        val wav = WavEncoder.encodeWaveBytes(ShortArray(100))
        // Bits per sample at bytes 34-35 (little-endian), should be 16
        val bitsPerSample = (wav[34].toInt() and 0xFF) or ((wav[35].toInt() and 0xFF) shl 8)
        assertEquals(16, bitsPerSample)
    }

    @Test
    fun `wav data chunk size is correct`() {
        val numSamples = 8000
        val wav = WavEncoder.encodeWaveBytes(ShortArray(numSamples))
        // Data chunk size at bytes 40-43 (little-endian)
        val dataSize = (wav[40].toInt() and 0xFF) or
            ((wav[41].toInt() and 0xFF) shl 8) or
            ((wav[42].toInt() and 0xFF) shl 16) or
            ((wav[43].toInt() and 0xFF) shl 24)
        assertEquals(numSamples * 2, dataSize)
    }

    @Test
    fun `wav preserves sample data`() {
        val samples = shortArrayOf(0, 100, -100, Short.MAX_VALUE, Short.MIN_VALUE)
        val wav = WavEncoder.encodeWaveBytes(samples)
        // Check first sample (bytes 44-45) is 0
        val s0 = (wav[44].toInt() and 0xFF) or ((wav[45].toInt() and 0xFF) shl 8)
        assertEquals(0, s0.toShort().toInt())
        // Check second sample (bytes 46-47) is 100
        val s1 = (wav[46].toInt() and 0xFF) or ((wav[47].toInt() and 0xFF) shl 8)
        assertEquals(100, s1.toShort().toInt())
    }
}
