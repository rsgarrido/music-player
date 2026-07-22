package com.example.cdplaya.benchmark

import android.Manifest
import android.os.Build
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

internal class CdPlayaBenchmarkActions(
    private val device: UiDevice = UiDevice.getInstance(
        InstrumentationRegistry.getInstrumentation()
    )
) {
    fun MacrobenchmarkScope.launchAndWaitForShell() {
        startActivityAndWait()
        grantMediaPermissionsIfNeeded()
        waitForText("CDPlaya")
    }

    fun selectHome() = clickText("Home")

    fun selectLibrary() = clickText("Library")

    fun selectSearch() = clickText("Search")

    fun openSettings() {
        val settings = device.wait(Until.findObject(By.descContains("Settings")), WAIT_TIMEOUT_MS)
            ?: error("Settings action was not visible")
        settings.click()
        waitForText("Settings")
    }

    fun selectLibrarySection(label: String) = clickText(label)

    fun selectExpandedPlayerTheme(displayName: String) {
        openSettings()
        clickText("Player theme")
        clickText(displayName)
        device.pressBack()
        device.pressBack()
        waitForText("CDPlaya")
    }

    fun expandPlayer() {
        val nowPlaying = device.wait(
            Until.findObject(By.descContains("Expand")),
            WAIT_TIMEOUT_MS
        ) ?: error("Expanded-player trigger was not visible; start playback first")
        nowPlaying.click()
        device.waitForIdle()
    }

    fun collapsePlayer() {
        clickDescription("Collapse player")
        waitForText("CDPlaya")
    }

    fun openQueue() = clickDescription("Open up next queue", "Up Next")

    fun clickNextTrack() = clickDescription("Next track", "Next song", "Next")

    fun clickPlayPause() = clickDescription("Play", "Pause")

    fun swipeExpandedPlayerLeft() {
        val width = device.displayWidth
        val height = device.displayHeight
        device.swipe(
            (width * 0.82f).toInt(),
            (height * 0.46f).toInt(),
            (width * 0.18f).toInt(),
            (height * 0.46f).toInt(),
            20
        )
        device.waitForIdle()
    }

    fun keepAnimatedPlayerVisible() {
        // A bounded five-second observation window is the workload being measured, not
        // synchronization. Continuous animation prevents the window from becoming idle.
        device.waitForIdle(5_000L)
    }

    fun scrollForward(repetitions: Int = 3) {
        val scrollable = device.wait(Until.findObject(By.scrollable(true)), WAIT_TIMEOUT_MS)
            ?: error("No scrollable content was visible")
        repeat(repetitions) {
            scrollable.scroll(Direction.DOWN, 0.75f)
        }
    }

    fun enterSearchQuery(query: String) {
        val input = device.wait(Until.findObject(By.clazz("android.widget.EditText")), WAIT_TIMEOUT_MS)
            ?: error("Search input was not visible")
        input.text = query
        device.waitForIdle()
    }

    fun waitForText(text: String) {
        check(device.wait(Until.hasObject(By.text(text)), WAIT_TIMEOUT_MS)) {
            "Timed out waiting for text: $text"
        }
    }

    private fun clickText(text: String) {
        val target = device.wait(Until.findObject(By.text(text)), WAIT_TIMEOUT_MS)
            ?: error("Timed out waiting for text: $text")
        target.click()
        device.waitForIdle()
    }

    private fun clickDescription(vararg descriptions: String) {
        val target = descriptions.firstNotNullOfOrNull { description ->
            device.wait(Until.findObject(By.descContains(description)), SHORT_WAIT_TIMEOUT_MS)
        } ?: error("Timed out waiting for one of: ${descriptions.joinToString()}")
        target.click()
        device.waitForIdle()
    }

    private fun grantMediaPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.grantRuntimePermission(
                TARGET_PACKAGE,
                Manifest.permission.READ_MEDIA_AUDIO
            )
            instrumentation.uiAutomation.grantRuntimePermission(
                TARGET_PACKAGE,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        }
    }

    private companion object {
        const val WAIT_TIMEOUT_MS = 10_000L
        const val SHORT_WAIT_TIMEOUT_MS = 2_000L
    }
}
