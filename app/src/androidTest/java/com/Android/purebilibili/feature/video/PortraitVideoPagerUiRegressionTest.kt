package com.Android.purebilibili.feature.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.video.ui.pager.PortraitVideoViewportContainer
import com.android.purebilibili.feature.video.ui.pager.resolvePortraitInitialVideoAspectRatio
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortraitVideoPagerUiRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun targetVideoNotReady_usesPortraitFallbackViewportInsteadOfStaleLandscapeFrame() {
        composeTestRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .size(width = 360.dp, height = 640.dp)
                        .background(Color.Black)
                ) {
                    PortraitVideoViewportContainer(
                        currentVideoAspect = resolvePortraitInitialVideoAspectRatio(
                            itemBvid = "BV_NEXT",
                            currentPlayingBvid = "BV_PREV",
                            playerVideoWidth = 1920,
                            playerVideoHeight = 1080
                        ),
                        modifier = Modifier.fillMaxSize(),
                        viewportModifier = Modifier
                            .testTag("portrait_viewport")
                            .background(Color.Red)
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithTag("portrait_viewport")
            .assertWidthIsEqualTo(360.dp)
            .assertHeightIsEqualTo(640.dp)
    }
}
