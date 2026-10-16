package com.lu4p.fokuslauncher.usage

import android.app.usage.UsageEvents

/**
 * Computes screen-on duration from [UsageEvents.Event.SCREEN_INTERACTIVE] /
 * [UsageEvents.Event.SCREEN_NON_INTERACTIVE] pairs. Unlike summing per-app foreground time,
 * this excludes background playback and other usage while the display is off.
 */
internal fun computeScreenOnTimeMs(
        events: UsageEvents?,
        windowStart: Long,
        windowEnd: Long,
): Long {
    if (events == null || windowEnd <= windowStart) return 0L
    val timeline = buildList {
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            add(event.eventType to event.timeStamp)
        }
    }
    return computeScreenOnTimeMs(timeline, windowStart, windowEnd)
}

internal fun computeScreenOnTimeMs(
        events: List<Pair<Int, Long>>,
        windowStart: Long,
        windowEnd: Long,
): Long {
    if (windowEnd <= windowStart) return 0L
    var screenOnSince: Long? = null
    var screenStateKnown = false
    var totalMs = 0L

    for ((eventType, timeStamp) in events) {
        when (eventType) {
            UsageEvents.Event.SCREEN_INTERACTIVE -> {
                if (screenOnSince == null) {
                    screenOnSince = timeStamp.coerceIn(windowStart, windowEnd)
                }
                screenStateKnown = true
            }
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                // Only the first screen-off event may close a session that began
                // before the window. Later unmatched events must not count it again.
                val start = screenOnSince ?: windowStart.takeUnless { screenStateKnown }
                totalMs += closeOpenScreenOnSegment(start, timeStamp, windowStart, windowEnd)
                screenOnSince = null
                screenStateKnown = true
            }
            UsageEvents.Event.DEVICE_SHUTDOWN -> {
                totalMs += closeOpenScreenOnSegment(screenOnSince, timeStamp, windowStart, windowEnd)
                screenOnSince = null
                screenStateKnown = true
            }
            UsageEvents.Event.DEVICE_STARTUP -> {
                // A restart without a recorded shutdown leaves the closing time unknown.
                screenOnSince = null
                screenStateKnown = true
            }
        }
    }

    screenOnSince?.let {
        totalMs +=
                (windowEnd - it.coerceIn(windowStart, windowEnd)).coerceAtLeast(0L)
    }
    return totalMs.coerceIn(0L, windowEnd - windowStart)
}

private fun closeOpenScreenOnSegment(
        screenOnSince: Long?,
        segmentEnd: Long,
        windowStart: Long,
        windowEnd: Long,
): Long {
    if (screenOnSince == null) return 0L
    val end = segmentEnd.coerceIn(windowStart, windowEnd)
    val start = screenOnSince.coerceIn(windowStart, windowEnd)
    return (end - start).coerceAtLeast(0L)
}
