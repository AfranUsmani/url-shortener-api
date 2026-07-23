package io.github.afranusmani.urlshortener.dto;

import java.util.List;
import java.util.Map;

/**
 * Aggregated click analytics for a short link.
 *
 * @param shortCode    the code these stats belong to
 * @param totalClicks  all-time redirect count captured as detailed events
 * @param byDevice     click counts keyed by device class (Desktop/Mobile/Tablet/Bot/Unknown)
 * @param byBrowser    click counts keyed by browser family
 * @param byReferrer   click counts keyed by referrer host ("Direct" when none)
 * @param clicksByDay  daily counts for the trailing window, oldest first (zero-filled)
 */
public record AnalyticsResponse(
        String shortCode,
        long totalClicks,
        Map<String, Long> byDevice,
        Map<String, Long> byBrowser,
        Map<String, Long> byReferrer,
        List<DayCount> clicksByDay
) {

    /** A single day bucket in the time series. */
    public record DayCount(String date, long count) {
    }
}
