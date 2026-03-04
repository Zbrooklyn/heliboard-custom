// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encodes raw 16-bit PCM audio samples into WAV format.
 * Assumes mono, 16 kHz, 16-bit PCM.
 */
object WavEncoder {

    /**
     * Convert a ShortArray of PCM samples to a complete WAV byte array.
     */
    @JvmStatic
    fun encodeWaveBytes(data: ShortArray): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        val dataLength = data.size * 2
        val header = headerBytes(dataLength)
        val buffer = ByteBuffer.allocate(dataLength)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.asShortBuffer().put(data)
        val bytes = ByteArray(buffer.limit())
        buffer.get(bytes)
        return header + bytes
    }

    private fun headerBytes(dataLength: Int): ByteArray {
        require(dataLength > 0) { "Audio data must not be empty" }
        val buffer = ByteBuffer.allocate(44)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        // RIFF header
        buffer.put('R'.code.toByte()); buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte()); buffer.put('F'.code.toByte())
        buffer.putInt(dataLength + 36)       // file size minus RIFF header (8 bytes)
        buffer.put('W'.code.toByte()); buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte()); buffer.put('E'.code.toByte())
        // fmt sub-chunk
        buffer.put('f'.code.toByte()); buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte()); buffer.put(' '.code.toByte())
        buffer.putInt(16)                    // PCM sub-chunk size
        buffer.putShort(1.toShort())         // AudioFormat: PCM
        buffer.putShort(1.toShort())         // NumChannels: 1 (mono)
        buffer.putInt(16000)                 // SampleRate: 16 kHz
        buffer.putInt(32000)                 // ByteRate: 16000 * 1 * 2
        buffer.putShort(2.toShort())         // BlockAlign: 2
        buffer.putShort(16.toShort())        // BitsPerSample: 16
        // data sub-chunk
        buffer.put('d'.code.toByte()); buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte()); buffer.put('a'.code.toByte())
        buffer.putInt(dataLength)
        buffer.position(0)
        val bytes = ByteArray(buffer.limit())
        buffer.get(bytes)
        return bytes
    }
}
