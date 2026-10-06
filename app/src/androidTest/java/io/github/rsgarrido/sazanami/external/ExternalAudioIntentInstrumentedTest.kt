package io.github.rsgarrido.sazanami.external

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.rsgarrido.sazanami.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Intent/manifest/theme boundaries, without launching playback or requiring a provider. */
@RunWith(AndroidJUnit4::class)
class ExternalAudioIntentInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun viewAudioResolvesOnlyToTheDedicatedActivity() {
        listOf("audio/mpeg", "audio/mp4", "audio/opus", "application/ogg").forEach { mime ->
            val intent = viewIntent("content://external.test/audio/42", mime)
            val handlers = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            assertEquals(mime, listOf(ExternalAudioPlayerActivity::class.java.name),
                handlers.map { it.activityInfo.name })
            assertFalse(handlers.any { it.activityInfo.name == MainActivity::class.java.name })
        }
    }

    @Test
    fun chooserDoesNotOfferThisActivityForNetworkFileSendOrBrowsableRequests() {
        val rejected = listOf(
            viewIntent("https://external.test/audio.mp3", "audio/mpeg"),
            viewIntent("http://external.test/audio.mp3", "audio/mpeg"),
            viewIntent("file:///storage/emulated/0/audio.mp3", "audio/mpeg"),
            viewIntent("content://external.test/audio/42", "video/mp4"),
            viewIntent("content://external.test/audio/42", "audio/mpeg").apply {
                action = Intent.ACTION_SEND
            },
            viewIntent("content://external.test/audio/42", "audio/mpeg").apply {
                action = Intent.ACTION_SEND_MULTIPLE
            },
            viewIntent("content://external.test/audio/42", "audio/mpeg").apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
        )
        rejected.forEach { intent ->
            assertNull(intent.toString(), context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY))
        }
    }

    @Test
    fun actualIntentValidationRejectsMissingUriAndUnregisteredExplicitRequests() {
        assertEquals(ExternalAudioFailure.INVALID_REQUEST,
            validateExternalAudioIntent(Intent(Intent.ACTION_VIEW)))
        assertEquals(ExternalAudioFailure.UNSUPPORTED_URI,
            validateExternalAudioIntent(viewIntent("content:/missing-authority", "audio/mpeg")))
        assertEquals(ExternalAudioFailure.UNSUPPORTED_URI,
            validateExternalAudioIntent(viewIntent("https://external.test/audio.mp3", "audio/mpeg")))
        assertNull(validateExternalAudioIntent(viewIntent("content://external.test/audio/42", "application/ogg")))
    }

    @Test
    fun exportedActivityUsesAFloatingDismissibleWindowAndIsExcludedFromRecents() {
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, ExternalAudioPlayerActivity::class.java), 0
        )
        assertTrue(info.exported)
        assertTrue(info.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
        assertTrue(info.taskAffinity.isNullOrEmpty())
        assertEquals(ActivityInfo.LAUNCH_SINGLE_TOP, info.launchMode)
        val attributes = ContextThemeWrapper(context, info.theme).obtainStyledAttributes(
            intArrayOf(android.R.attr.windowIsFloating, android.R.attr.windowCloseOnTouchOutside)
        )
        try {
            assertTrue(attributes.getBoolean(0, false))
            assertTrue(attributes.getBoolean(1, false))
        } finally {
            attributes.recycle()
        }
    }

    private fun viewIntent(uri: String, mime: String): Intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(uri), mime)
        .setPackage(context.packageName)
}
