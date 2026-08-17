package com.unsupportedpastels.hermesandroid.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceInputPolicyTest {
    @Test
    fun selectsFirstNonBlankResultAndTrimsIt() {
        assertEquals(
            "Send the release notes",
            VoiceInputPolicy.bestResult(listOf("   ", "  Send the release notes  ", "ignored")),
        )
        assertNull(VoiceInputPolicy.bestResult(null))
        assertNull(VoiceInputPolicy.bestResult(listOf("", "  ")))
    }

    @Test
    fun boundsUntrustedRecognizerText() {
        val result = VoiceInputPolicy.bestResult(
            listOf("a".repeat(VoiceInputPolicy.MAX_RESULT_CHARS + 32)),
        )

        assertEquals(VoiceInputPolicy.MAX_RESULT_CHARS, result?.length)
    }

    @Test
    fun appendsRecognitionWithoutOverwritingTheDraft() {
        assertEquals("new words", VoiceInputPolicy.mergeDraft("", "new words"))
        assertEquals("existing new words", VoiceInputPolicy.mergeDraft("existing", "new words"))
        assertEquals("existing\nnew words", VoiceInputPolicy.mergeDraft("existing\n", "new words"))
        assertEquals("existing", VoiceInputPolicy.mergeDraft("existing", "   "))
    }
}