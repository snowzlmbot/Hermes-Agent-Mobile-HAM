package com.unsupportedpastels.hermesandroid.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.speech.RecognizerIntent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.unsupportedpastels.hermesandroid.app.DurableSessionId
import com.unsupportedpastels.hermesandroid.app.SessionSummary
import com.unsupportedpastels.hermesandroid.connection.ServerOrigin
import com.unsupportedpastels.hermesandroid.connection.ServerSettingsState
import com.unsupportedpastels.hermesandroid.gateway.AuthenticationState
import com.unsupportedpastels.hermesandroid.gateway.ConnectionState
import com.unsupportedpastels.hermesandroid.gateway.HermesGatewaySnapshot
import com.unsupportedpastels.hermesandroid.navigation.SessionDetailRoute
import com.unsupportedpastels.hermesandroid.theme.HermesAndroidTheme
import com.unsupportedpastels.hermesandroid.voice.DeviceVoiceInputContract
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class VoiceInputUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun staleProfileResultIsDiscardedWhileCurrentResultUpdatesDraft() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context.packageManager).addResolveInfoForIntent(
            DeviceVoiceInputContract().createIntent(context, Unit),
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = "test.speech.service"
                    name = "SpeechActivity"
                }
            },
        )
        val registry = RecordingActivityResultRegistry()
        val registryOwner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
        val sessionId = DurableSessionId("voice-session")
        var snapshot by mutableStateOf(
            HermesGatewaySnapshot(
                connectionState = ConnectionState.Connected,
                authenticationState = AuthenticationState.Authenticated,
                durableSessions = listOf(SessionSummary(sessionId, "Voice session")),
                selectedProfile = "default",
            ),
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                HermesAndroidTheme {
                    HermesApp(
                        snapshot = snapshot,
                        initialRoute = SessionDetailRoute(sessionId),
                        serverSettingsState = ServerSettingsState.Ready(
                            ServerOrigin.parse("https://hermes.example"),
                        ),
                    )
                }
            }
        }

        launchVoiceInput()
        composeRule.runOnIdle {
            snapshot = snapshot.copy(selectedProfile = "work")
        }
        composeRule.runOnIdle {
            registry.dispatchRecognition("stale default-profile text")
        }
        assertComposerText("")

        launchVoiceInput()
        composeRule.runOnIdle {
            registry.dispatchRecognition("current work-profile text")
        }
        assertComposerText("current work-profile text")
    }

    private fun launchVoiceInput() {
        composeRule.onNodeWithContentDescription("Attach files").performClick()
        composeRule.onNodeWithText("Voice input").performClick()
    }

    private fun assertComposerText(expected: String) {
        val composer = hasAnyAncestor(hasTestTag("Message composer"))
        val text = composeRule.onNode(hasSetTextAction().and(composer))
            .fetchSemanticsNode()
            .config[SemanticsProperties.InputText]
            .text
        assertEquals(expected, text)
    }

    private class RecordingActivityResultRegistry : ActivityResultRegistry() {
        private var requestCode: Int? = null

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            this.requestCode = requestCode
        }

        fun dispatchRecognition(text: String) {
            dispatchResult(
                checkNotNull(requestCode) { "Voice input was not launched" },
                Activity.RESULT_OK,
                Intent().putStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS,
                    arrayListOf(text),
                ),
            )
        }
    }
}