package com.example.cdplaya.ui.statistics

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cdplaya.data.AnalyticsRangePreset
import com.example.cdplaya.data.AnalyticsRangeSelection
import com.example.cdplaya.data.ListeningOverview
import com.example.cdplaya.data.ListeningPlayCountBreakdown
import com.example.cdplaya.data.ListeningTimeBreakdown
import com.example.cdplaya.ui.state.ListeningAnalyticsError
import com.example.cdplaya.ui.state.ListeningAnalyticsErrorKind
import com.example.cdplaya.ui.state.ListeningAnalyticsUiState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StatisticsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun overviewFormatsMetricsAndDefaultRangeSemantics() {
        setStatisticsContent(
            state = ListeningAnalyticsUiState(overview = overview())
        )

        composeRule.onNodeWithText("Last 30 days").assertIsSelected()
        composeRule.onNodeWithText("3 hr 18 min").assertExists()
        composeRule.onNodeWithText("1,234").assertExists()
        composeRule.onNodeWithText("52").assertExists()
        composeRule.onNodeWithText("9").assertExists()
        composeRule.onNodeWithContentDescription(
            "Not counted, 9. Attempts below the play threshold"
        ).assertExists()
    }

    @Test
    fun presetAndCustomIntentsAreNarrowAndInclusive() {
        var preset: AnalyticsRangePreset? = null
        var custom: Pair<LocalDate, LocalDate>? = null
        setStatisticsContent(
            state = ListeningAnalyticsUiState(overview = overview()),
            onPresetSelected = { preset = it },
            onCustomRangeSelected = { start, end -> custom = start to end }
        )

        composeRule.onNodeWithText("Today").performClick()
        composeRule.runOnIdle { assertEquals(AnalyticsRangePreset.TODAY, preset) }
        composeRule.onNodeWithText("Custom").performScrollTo().performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.runOnIdle { assertEquals(null, custom) }
    }

    @Test
    fun loadingErrorsRefreshAndEmptyStatesRemainExplicit() {
        val screenState = mutableStateOf(ListeningAnalyticsUiState(isInitialLoading = true))
        var retried = false
        composeRule.setContent {
            MaterialTheme {
                StatisticsScreen(
                    state = screenState.value,
                    onBackClick = {},
                    onPresetSelected = {},
                    onCustomRangeSelected = { _, _ -> },
                    onRetry = { retried = true },
                    listState = remember { LazyListState() }
                )
            }
        }
        composeRule.onNodeWithText("Loading listening statistics…").assertExists()

        composeRule.runOnIdle {
            screenState.value = ListeningAnalyticsUiState(
                error = ListeningAnalyticsError(
                    ListeningAnalyticsErrorKind.SNAPSHOT_LOAD,
                    retryable = true,
                    cause = IllegalStateException("not shown")
                )
            )
        }
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.runOnIdle { assertTrue(retried) }

        composeRule.runOnIdle {
            screenState.value = ListeningAnalyticsUiState(overview = overview(), isRefreshing = true)
        }
        composeRule.onNodeWithTag("statistics_refresh_indicator").assertExists()
        composeRule.onNodeWithText("Refreshing this range.", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("1,234").assertExists()

        composeRule.runOnIdle {
            screenState.value = ListeningAnalyticsUiState(
                selectedRange = AnalyticsRangeSelection.Preset(AnalyticsRangePreset.ALL_TIME),
                overview = overview(plays = 0L, detailedEvents = 0L)
            )
        }
        composeRule.onNodeWithText("Your listening activity will appear here as you play music.")
            .assertExists()

        composeRule.runOnIdle {
            screenState.value = ListeningAnalyticsUiState(
                overview = overview(plays = 0L, detailedEvents = 0L)
            )
        }
        composeRule.onNodeWithText("No listening activity in this range.").assertExists()
    }

    @Test
    fun refreshUsesReservedSlotWithoutMovingMetrics() {
        val screenState = mutableStateOf(ListeningAnalyticsUiState(overview = overview()))
        composeRule.setContent {
            MaterialTheme {
                StatisticsScreen(
                    state = screenState.value,
                    onBackClick = {},
                    onPresetSelected = {},
                    onCustomRangeSelected = { _, _ -> },
                    onRetry = {},
                    listState = remember { LazyListState() }
                )
            }
        }

        composeRule.onNodeWithTag("statistics_refresh_slot").assertExists()
        composeRule.onNodeWithTag("statistics_refresh_indicator").assertDoesNotExist()
        composeRule.onNodeWithText("Last 30 days").assertIsSelected()
        val metric = composeRule.onNodeWithContentDescription(
            "Recorded listening, 3 hours, 18 minutes. Detailed history only"
        )
        val idleTop = metric.fetchSemanticsNode().boundsInRoot.top

        composeRule.runOnIdle {
            screenState.value = screenState.value.copy(isRefreshing = true)
        }

        composeRule.onNodeWithTag("statistics_refresh_indicator").assertExists()
        composeRule.onNodeWithText("Refreshing this range.", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("1,234").assertExists()
        val refreshingTop = metric.fetchSemanticsNode().boundsInRoot.top
        assertEquals(idleTop, refreshingTop, 0.5f)
    }

    @Test
    fun historyDialogDoesNotReplaceRememberedListState() {
        lateinit var listState: LazyListState
        composeRule.setContent {
            MaterialTheme {
                listState = remember { LazyListState() }
                StatisticsScreen(
                    state = ListeningAnalyticsUiState(overview = overview()),
                    onBackClick = {},
                    onPresetSelected = {},
                    onCustomRangeSelected = { _, _ -> },
                    onRetry = {},
                    listState = listState,
                    modifier = Modifier.height(300.dp)
                )
            }
        }
        composeRule.runOnIdle { listState.requestScrollToItem(3) }
        composeRule.waitForIdle()
        val indexBeforeDialog = listState.firstVisibleItemIndex
        val offsetBeforeDialog = listState.firstVisibleItemScrollOffset
        assertTrue(indexBeforeDialog > 0 || offsetBeforeDialog > 0)
        composeRule.onNodeWithText("Learn more").performClick()
        composeRule.onNodeWithText("About your listening history").assertExists()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.runOnIdle {
            assertEquals(indexBeforeDialog, listState.firstVisibleItemIndex)
            assertEquals(offsetBeforeDialog, listState.firstVisibleItemScrollOffset)
        }
    }

    @Test
    fun incompletePickerCannotConfirm() {
        composeRule.setContent {
            MaterialTheme {
                StatisticsDateRangeDialog(
                    initialStartDate = null,
                    initialEndDateInclusive = null,
                    onDismiss = {},
                    onConfirm = { _, _ -> }
                )
            }
        }
        composeRule.onNodeWithText("Confirm").assertIsNotEnabled()
    }

    private fun setStatisticsContent(
        state: ListeningAnalyticsUiState,
        onPresetSelected: (AnalyticsRangePreset) -> Unit = {},
        onCustomRangeSelected: (LocalDate, LocalDate) -> Unit = { _, _ -> },
        onRetry: () -> Unit = {}
    ) {
        composeRule.setContent {
            MaterialTheme {
                StatisticsScreen(
                    state = state,
                    onBackClick = {},
                    onPresetSelected = onPresetSelected,
                    onCustomRangeSelected = onCustomRangeSelected,
                    onRetry = onRetry,
                    listState = remember { LazyListState() }
                )
            }
        }
    }

    private fun overview(
        plays: Long = 1_234L,
        detailedEvents: Long = 70L
    ) = ListeningOverview(
        playCounts = ListeningPlayCountBreakdown(plays, 100L.coerceAtMost(plays), (plays - 100L).coerceAtLeast(0L)),
        listeningTime = ListeningTimeBreakdown(
            confirmedDetailedListeningMs = (3L * 60L + 18L) * 60_000L,
            legacyPlayCountWithoutKnownDuration = 100L.coerceAtMost(plays)
        ),
        qualifiedDetailedPlayCount = (plays - 100L).coerceAtLeast(0L),
        naturalCompletionCount = 52L,
        nonQualifiedAttemptCount = 9L,
        detailedEventCount = detailedEvents,
        firstDetailedEventAt = null,
        latestDetailedEventAt = null,
        firstKnownPlayAt = null,
        latestKnownPlayAt = null,
        hasLegacyBaseline = plays > 0L
    )
}
