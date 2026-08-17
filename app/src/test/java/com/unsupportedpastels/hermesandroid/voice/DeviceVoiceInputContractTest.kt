package com.unsupportedpastels.hermesandroid.voice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.speech.RecognizerIntent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeviceVoiceInputContractTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contract = DeviceVoiceInputContract()

    @Test
    fun createsGenericFreeFormRecognitionIntent() {
        val intent = contract.createIntent(context, Unit)

        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, intent.action)
        assertEquals(
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL),
        )
        assertEquals(3, intent.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 0))
        assertNull(intent.component)
        assertNull(intent.`package`)
    }

    @Test
    fun returnsTextOnlyForSuccessfulRecognition() {
        val result = Intent().putStringArrayListExtra(
            RecognizerIntent.EXTRA_RESULTS,
            arrayListOf("  draft from speech  "),
        )

        assertEquals("draft from speech", contract.parseResult(Activity.RESULT_OK, result))
        assertNull(contract.parseResult(Activity.RESULT_CANCELED, result))
        assertNull(contract.parseResult(Activity.RESULT_OK, null))
    }

    @Test
    fun reportsAvailabilityOnlyWhenADeviceHandlerExists() {
        assertFalse(DeviceVoiceInputContract.isAvailable(context))

        shadowOf(context.packageManager).addResolveInfoForIntent(
            contract.createIntent(context, Unit),
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = "test.speech.service"
                    name = "SpeechActivity"
                }
            },
        )

        assertTrue(DeviceVoiceInputContract.isAvailable(context))
    }

    @Test
    fun appDoesNotRequestMicrophonePermission() {
        @Suppress("DEPRECATION")
        val permissions = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        ).requestedPermissions.orEmpty()

        assertFalse(Manifest.permission.RECORD_AUDIO in permissions)
    }
}