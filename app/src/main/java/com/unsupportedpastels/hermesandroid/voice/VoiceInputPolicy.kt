package com.unsupportedpastels.hermesandroid.voice

internal object VoiceInputPolicy {
    const val MAX_RESULT_CHARS = 4_096

    fun bestResult(results: List<String>?): String? = null

    fun mergeDraft(current: String, recognized: String): String = current
}