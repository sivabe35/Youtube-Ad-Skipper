package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sharedPreferences = context.getSharedPreferences("test_ad_skip_prefs", Context.MODE_PRIVATE)
    
    // Seed test data
    sharedPreferences.edit()
        .putInt(YoutubeAdSkipService.KEY_SKIP_COUNT, 24)
        .putLong(YoutubeAdSkipService.KEY_LAST_SKIPPED_TIME, 1716301402000L) // Fixed timestamp for reproducible screenshots
        .commit()

    composeTestRule.setContent {
      MyApplicationTheme {
        DashboardScreen(sharedPreferences = sharedPreferences)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
