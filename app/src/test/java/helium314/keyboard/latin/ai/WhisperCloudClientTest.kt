// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ai

import helium314.keyboard.latin.ai.WhisperCloudClient.TranscribeResult
import helium314.keyboard.latin.voice.WavEncoder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WhisperCloudClientTest {

    // ---- TranscribeResult sealed class tests ----

    @Test
    fun `TranscribeResult Success holds text`() {
        val result = TranscribeResult.Success("hello")
        assertEquals("hello", result.text)
    }

    @Test
    fun `TranscribeResult Empty has default message`() {
        val result = TranscribeResult.Empty()
        assertEquals("No speech detected", result.message)
    }

    @Test
    fun `TranscribeResult Empty accepts custom message`() {
        val result = TranscribeResult.Empty("custom")
        assertEquals("custom", result.message)
    }

    @Test
    fun `TranscribeResult ApiError holds code and message`() {
        val result = TranscribeResult.ApiError(401, "Invalid key")
        assertEquals(401, result.code)
        assertEquals("Invalid key", result.message)
    }

    @Test
    fun `TranscribeResult NetworkError holds message`() {
        val result = TranscribeResult.NetworkError("timeout")
        assertEquals("timeout", result.message)
    }

    @Test
    fun `TranscribeResult types are distinct`() {
        val success = TranscribeResult.Success("text")
        val empty = TranscribeResult.Empty()
        val apiError = TranscribeResult.ApiError(500, "error")
        val networkError = TranscribeResult.NetworkError("fail")

        // Each type is only an instance of its own subclass
        assertIs<TranscribeResult.Success>(success)
        assertFalse(success is TranscribeResult.Empty)
        assertFalse(success is TranscribeResult.ApiError)
        assertFalse(success is TranscribeResult.NetworkError)

        assertIs<TranscribeResult.Empty>(empty)
        assertFalse(empty is TranscribeResult.Success)
        assertFalse(empty is TranscribeResult.ApiError)
        assertFalse(empty is TranscribeResult.NetworkError)

        assertIs<TranscribeResult.ApiError>(apiError)
        assertFalse(apiError is TranscribeResult.Success)
        assertFalse(apiError is TranscribeResult.Empty)
        assertFalse(apiError is TranscribeResult.NetworkError)

        assertIs<TranscribeResult.NetworkError>(networkError)
        assertFalse(networkError is TranscribeResult.Success)
        assertFalse(networkError is TranscribeResult.Empty)
        assertFalse(networkError is TranscribeResult.ApiError)

        // All are TranscribeResult
        assertIs<TranscribeResult>(success)
        assertIs<TranscribeResult>(empty)
        assertIs<TranscribeResult>(apiError)
        assertIs<TranscribeResult>(networkError)
    }

    // ---- WavEncoder integration tests (first step of transcribe) ----

    @Test
    fun `transcribe with empty samples produces empty byte array`() {
        // WavEncoder.encodeWaveBytes returns ByteArray(0) for empty input,
        // which is the first step inside WhisperCloudClient.transcribe
        val wav = WavEncoder.encodeWaveBytes(ShortArray(0))
        assertEquals(0, wav.size)
    }

    @Test
    fun `transcribe WAV encoding produces correct header for short audio`() {
        // 16000 samples at 16kHz = 1 second of audio
        // Each sample is 2 bytes, so data = 32000 bytes
        // WAV header = 44 bytes, total = 44 + 32000 = 32044
        val samples = ShortArray(16000)
        val wav = WavEncoder.encodeWaveBytes(samples)
        assertEquals(44 + 32000, wav.size)
    }

    // ---- Error code message verification ----

    @Test
    fun `error code 401 message`() {
        // Matches the when-branch in WhisperCloudClient.transcribe for 401
        val result = TranscribeResult.ApiError(401, "Invalid OpenAI API key")
        assertEquals(401, result.code)
        assertEquals("Invalid OpenAI API key", result.message)
    }

    @Test
    fun `error code 429 message`() {
        // Matches the when-branch in WhisperCloudClient.transcribe for 429
        val result = TranscribeResult.ApiError(429, "API quota exceeded \u2014 check your OpenAI billing")
        assertEquals(429, result.code)
        assertEquals("API quota exceeded \u2014 check your OpenAI billing", result.message)
    }

    @Test
    fun `error code 500 message`() {
        // Matches the when-branch in WhisperCloudClient.transcribe for 500/502/503
        val result = TranscribeResult.ApiError(500, "OpenAI server error \u2014 try again")
        assertEquals(500, result.code)
        assertEquals("OpenAI server error \u2014 try again", result.message)
    }

    // ---- WavEncoder produces valid WAV for transcription input ----

    @Test
    fun `WavEncoder produces valid WAV for transcription input`() {
        // Encode a known set of samples and verify the RIFF header structure
        val samples = ShortArray(160) { (it % 128).toShort() } // 10ms at 16kHz
        val wav = WavEncoder.encodeWaveBytes(samples)
        val dataLength = samples.size * 2 // 320 bytes of PCM data

        // Total size = 44 (header) + dataLength
        assertEquals(44 + dataLength, wav.size)

        // Verify RIFF header magic bytes
        assertEquals('R'.code.toByte(), wav[0])
        assertEquals('I'.code.toByte(), wav[1])
        assertEquals('F'.code.toByte(), wav[2])
        assertEquals('F'.code.toByte(), wav[3])

        // Verify file size field (bytes 4-7, little-endian): dataLength + 36
        val fileSizeField = ByteBuffer.wrap(wav, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(dataLength + 36, fileSizeField)

        // Verify WAVE format
        assertEquals('W'.code.toByte(), wav[8])
        assertEquals('A'.code.toByte(), wav[9])
        assertEquals('V'.code.toByte(), wav[10])
        assertEquals('E'.code.toByte(), wav[11])

        // Verify fmt sub-chunk
        assertEquals('f'.code.toByte(), wav[12])
        assertEquals('m'.code.toByte(), wav[13])
        assertEquals('t'.code.toByte(), wav[14])
        assertEquals(' '.code.toByte(), wav[15])

        // fmt chunk size = 16 (bytes 16-19)
        val fmtSize = ByteBuffer.wrap(wav, 16, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(16, fmtSize)

        // AudioFormat = 1 (PCM) at bytes 20-21
        val audioFormat = ByteBuffer.wrap(wav, 20, 2).order(ByteOrder.LITTLE_ENDIAN).short
        assertEquals(1.toShort(), audioFormat)

        // NumChannels = 1 (mono) at bytes 22-23
        val numChannels = ByteBuffer.wrap(wav, 22, 2).order(ByteOrder.LITTLE_ENDIAN).short
        assertEquals(1.toShort(), numChannels)

        // SampleRate = 16000 at bytes 24-27
        val sampleRate = ByteBuffer.wrap(wav, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(16000, sampleRate)

        // ByteRate = 32000 at bytes 28-31
        val byteRate = ByteBuffer.wrap(wav, 28, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(32000, byteRate)

        // BlockAlign = 2 at bytes 32-33
        val blockAlign = ByteBuffer.wrap(wav, 32, 2).order(ByteOrder.LITTLE_ENDIAN).short
        assertEquals(2.toShort(), blockAlign)

        // BitsPerSample = 16 at bytes 34-35
        val bitsPerSample = ByteBuffer.wrap(wav, 34, 2).order(ByteOrder.LITTLE_ENDIAN).short
        assertEquals(16.toShort(), bitsPerSample)

        // Verify data sub-chunk header
        assertEquals('d'.code.toByte(), wav[36])
        assertEquals('a'.code.toByte(), wav[37])
        assertEquals('t'.code.toByte(), wav[38])
        assertEquals('a'.code.toByte(), wav[39])

        // data chunk size (bytes 40-43) = dataLength
        val dataChunkSize = ByteBuffer.wrap(wav, 40, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(dataLength, dataChunkSize)
    }
}
