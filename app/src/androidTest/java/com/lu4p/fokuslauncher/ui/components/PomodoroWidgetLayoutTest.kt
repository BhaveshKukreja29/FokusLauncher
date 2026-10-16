package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lu4p.fokuslauncher.data.model.PomodoroMode
import com.lu4p.fokuslauncher.ui.theme.FokusLauncherTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PomodoroWidgetLayoutTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun outlinedControls_leaveRoomBetweenCircularBackdrops() {
        composeTestRule.setContent {
            FokusLauncherTheme {
                PomodoroWidget(
                    remainingText = "24:56",
                    isRunning = true,
                    awaitingDismiss = false,
                    mode = PomodoroMode.FOCUS,
                    outlined = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        val density = composeTestRule.density
        val widget = composeTestRule.onNodeWithTag("pomodoro_widget").fetchSemanticsNode().boundsInRoot
        val focus = composeTestRule.onNodeWithTag("pomodoro_mode_focus").fetchSemanticsNode().boundsInRoot
        val decrease = composeTestRule.onNodeWithTag("pomodoro_decrease").fetchSemanticsNode().boundsInRoot
        val playPause = composeTestRule.onNodeWithTag("pomodoro_play_pause").fetchSemanticsNode().boundsInRoot
        val increase = composeTestRule.onNodeWithTag("pomodoro_increase").fetchSemanticsNode().boundsInRoot
        val remaining = composeTestRule.onNodeWithTag("pomodoro_remaining").fetchSemanticsNode().boundsInRoot
        val minimumHorizontalGapPx = with(density) { 28.dp.toPx() }
        val minimumVerticalGapPx = with(density) { 20.dp.toPx() }

        assertTrue("Mode labels should start at the widget's left edge", focus.left - widget.left < with(density) { 1.dp.toPx() })
        assertTrue("Controls should start at the widget's left edge", decrease.left - widget.left < with(density) { 1.dp.toPx() })
        assertTrue("Decrease and pause backdrops need separate visual space", playPause.left - decrease.right >= minimumHorizontalGapPx)
        assertTrue("Pause and increase backdrops need separate visual space", increase.left - playPause.right >= minimumHorizontalGapPx)
        assertTrue("Timer text and control backdrops need separate visual space", playPause.top - remaining.bottom >= minimumVerticalGapPx)
    }
}
