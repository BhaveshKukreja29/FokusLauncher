package com.lu4p.fokuslauncher

import android.app.Activity
import android.app.ActivityOptions
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.lu4p.fokuslauncher.data.widget.WidgetHostManager

internal enum class WidgetConfigurationLaunchResult {
    STARTED,
    MISSING_ACTIVITY,
    ACCESS_DENIED,
    STALE_BINDING,
}

internal fun launchWidgetConfiguration(launch: () -> Unit): WidgetConfigurationLaunchResult =
        try {
            launch()
            WidgetConfigurationLaunchResult.STARTED
        } catch (_: ActivityNotFoundException) {
            WidgetConfigurationLaunchResult.MISSING_ACTIVITY
        } catch (_: SecurityException) {
            WidgetConfigurationLaunchResult.ACCESS_DENIED
        } catch (_: IllegalArgumentException) {
            WidgetConfigurationLaunchResult.STALE_BINDING
        }

internal fun widgetConfigurationActivityOptions(): ActivityOptions? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null

    // AppWidgetHost uses a system-created IntentSender. Android 14+ requires the sender
    // to opt in to lend its activity-start privileges to this user-requested launch.
    val mode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
            } else {
                @Suppress("DEPRECATION") // Required on Android 14/15; the visible-only mode is API 36+.
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
    return ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(mode)
}

/**
 * Starts widget configuration through [AppWidgetHost], then forwards the provider's result to the
 * widget page.
 *
 * Providers do not have to export their configuration activity. Calling the activity directly can
 * therefore throw a [SecurityException]. AppWidgetHost asks the system to launch it on our behalf.
 */
class WidgetConfigurationActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId =
                intent.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID,
                )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finishWithResult(RESULT_CANCELED, null)
            return
        }

        if (savedInstanceState == null) {
            val launchResult = launchWidgetConfiguration {
                AppWidgetHost(applicationContext, WidgetHostManager.HOST_ID)
                        .startAppWidgetConfigureActivityForResult(
                                this,
                                appWidgetId,
                                0,
                                REQUEST_CONFIGURE,
                                widgetConfigurationActivityOptions()?.toBundle(),
                        )
            }
            Log.i(TAG, "Widget configuration launch: $launchResult, id=$appWidgetId")
            if (launchResult != WidgetConfigurationLaunchResult.STARTED) {
                finishWithResult(RESULT_CANCELED, null)
            }
        }
    }

    @Deprecated("Deprecated in the framework, but required by AppWidgetHost's configuration API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE) {
            Log.i(TAG, "Widget provider configuration result: code=$resultCode, id=$appWidgetId")
            finishWithResult(resultCode, data)
        }
    }

    private fun finishWithResult(resultCode: Int, data: Intent?) {
        val result =
                data ?: Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(resultCode, result)
        finish()
    }

    companion object {
        private const val TAG = "WidgetConfiguration"
        private const val REQUEST_CONFIGURE = 1

        fun createIntent(context: Context, appWidgetId: Int): Intent =
                Intent(context, WidgetConfigurationActivity::class.java).putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        appWidgetId,
                )
    }
}
