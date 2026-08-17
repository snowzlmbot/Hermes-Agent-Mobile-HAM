package com.unsupportedpastels.hermesandroid.voice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContract

internal class DeviceVoiceInputContract : ActivityResultContract<Unit, String?>() {
    override fun createIntent(context: Context, input: Unit): Intent = recognitionIntent()

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode != Activity.RESULT_OK) return null
        return VoiceInputPolicy.bestResult(
            intent?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS),
        )
    }

    companion object {
        fun isAvailable(context: Context): Boolean =
            recognitionIntent().resolveActivity(context.packageManager) != null

        private fun recognitionIntent(): Intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
    }
}