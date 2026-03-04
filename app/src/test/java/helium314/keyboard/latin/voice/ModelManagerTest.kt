// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModelManagerTest {

    @Test
    fun `available models list is not empty`() {
        assertTrue(ModelManager.availableModels.isNotEmpty())
    }

    @Test
    fun `default model is tiny english`() {
        assertEquals("ggml-tiny.en-q5_1.bin", ModelManager.defaultModel.fileName)
        assertEquals("Tiny", ModelManager.defaultModel.tier)
    }

    @Test
    fun `all models have valid URLs`() {
        for (model in ModelManager.availableModels) {
            assertTrue(model.url.startsWith("https://huggingface.co/"), "Invalid URL for ${model.name}: ${model.url}")
            assertTrue(model.url.endsWith(".bin"), "URL should end with .bin for ${model.name}")
        }
    }

    @Test
    fun `all models have unique filenames`() {
        val fileNames = ModelManager.availableModels.map { it.fileName }
        assertEquals(fileNames.size, fileNames.toSet().size, "Duplicate filenames found")
    }

    @Test
    fun `all models have positive size`() {
        for (model in ModelManager.availableModels) {
            assertTrue(model.sizeMb > 0, "${model.name} has invalid size: ${model.sizeMb}")
        }
    }

    @Test
    fun `large v3 turbo model is available`() {
        val turbo = ModelManager.availableModels.find { it.fileName.contains("large-v3-turbo") }
        assertNotNull(turbo, "Large v3 turbo model should be available")
        assertEquals("Large", turbo.tier)
        assertTrue(turbo.sizeMb > 500, "Turbo model should be >500MB")
    }

    @Test
    fun `models are sorted by size within tiers`() {
        // English models should go Tiny < Base < Small < Medium < Large
        val english = ModelManager.availableModels.filter { it.fileName.contains(".en") || it.fileName.contains("large") }
        for (i in 0 until english.size - 1) {
            assertTrue(
                english[i].sizeMb <= english[i + 1].sizeMb,
                "English models not sorted by size: ${english[i].name} (${english[i].sizeMb}MB) > ${english[i + 1].name} (${english[i + 1].sizeMb}MB)"
            )
        }
    }

    @Test
    fun `download progress defaults are correct`() {
        val progress = DownloadProgress()
        assertEquals(null, progress.model)
        assertEquals(0L, progress.bytesDownloaded)
        assertEquals(0L, progress.totalBytes)
        assertEquals(false, progress.isDownloading)
    }
}
