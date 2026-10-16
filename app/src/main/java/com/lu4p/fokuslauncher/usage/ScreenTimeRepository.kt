package com.lu4p.fokuslauncher.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenTimeRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    /** Rolling 24-hour screen-on total in milliseconds, or null when usage access is missing. */
    fun queryLast24HoursTotalMs(): Long? {
        if (!UsageStatsHelper.hasUsageAccess(context)) return null
        val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - TWENTY_FOUR_HOURS_MS

        // Individual screen events are available from Android 9. Aggregated event stats
        // can expand the requested range to whole days, so they cannot give a rolling total.
        return computeScreenOnTimeMs(usageStatsManager.queryEvents(start, end), start, end)
    }

    private companion object {
        private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L
    }
}
