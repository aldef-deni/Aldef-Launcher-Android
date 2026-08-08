package com.aldef.launcher.ai

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lapisan AI daring memakai SDK resmi Anthropic.
 *
 * Catatan keamanan: API key disimpan di perangkat. Untuk pemakaian pribadi ini
 * wajar, tapi jika aplikasi disebarkan ke publik, key sebaiknya diletakkan di
 * server perantara milik Anda sendiri — bukan di dalam APK.
 */
class ClaudeClient(private val apiKey: String) {

    private val client: AnthropicClient by lazy {
        AnthropicOkHttpClient.builder().apiKey(apiKey).build()
    }

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    suspend fun ask(systemPrompt: String, userPrompt: String): String = withContext(Dispatchers.IO) {
        val params = MessageCreateParams.builder()
            .model(MODEL)
            .maxTokens(600L)
            .system(systemPrompt)
            // Asisten suara: jawaban pendek dan cepat lebih penting daripada
            // penalaran mendalam, jadi effort ditahan di level rendah.
            .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
            .addUserMessage(userPrompt)
            .build()

        val message = client.messages().create(params)

        message.content()
            .mapNotNull { block -> block.text().orElse(null) }
            .joinToString(" ") { it.text() }
            .trim()
    }

    companion object {
        const val MODEL = "claude-opus-5"
    }
}
