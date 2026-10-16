package com.lu4p.fokuslauncher

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class WidgetConfigurationLaunchTest {
    @Test
    @Config(sdk = [33])
    fun `Android 13 keeps the legacy configuration launch options`() {
        assertNull(widgetConfigurationActivityOptions())
    }

    @Test
    @Config(sdk = [34, 35])
    @Suppress("DEPRECATION") // This is the supported opt-in on these Android versions.
    fun `Android 14 and 15 grant activity start privileges to widget configuration`() {
        val options = requireNotNull(widgetConfigurationActivityOptions())

        assertEquals(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                options.pendingIntentBackgroundActivityStartMode,
        )
    }

    @Test
    @Config(sdk = [36])
    fun `Android 16 grants activity start privileges only while visible`() {
        val options = requireNotNull(widgetConfigurationActivityOptions())

        assertEquals(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE,
                options.pendingIntentBackgroundActivityStartMode,
        )
    }

    @Test
    fun `missing provider configuration is reported as launch failure`() {
        assertEquals(
                WidgetConfigurationLaunchResult.MISSING_ACTIVITY,
                launchWidgetConfiguration { throw ActivityNotFoundException() },
        )
    }

    @Test
    fun `inaccessible provider configuration is reported as launch failure`() {
        assertEquals(
                WidgetConfigurationLaunchResult.ACCESS_DENIED,
                launchWidgetConfiguration { throw SecurityException("not exported") },
        )
    }

    @Test
    fun `stale bound widget is reported as launch failure`() {
        assertEquals(
                WidgetConfigurationLaunchResult.STALE_BINDING,
                launchWidgetConfiguration { throw IllegalArgumentException("Widget not bound") },
        )
    }

    @Test
    fun `successful launch is reported as started`() {
        assertEquals(WidgetConfigurationLaunchResult.STARTED, launchWidgetConfiguration {})
    }
}
