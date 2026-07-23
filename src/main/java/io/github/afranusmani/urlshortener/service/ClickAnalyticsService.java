package io.github.afranusmani.urlshortener.service;

import io.github.afranusmani.urlshortener.dto.AnalyticsResponse;
import io.github.afranusmani.urlshortener.model.ClickEvent;
import io.github.afranusmani.urlshortener.repository.ClickEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Captures and aggregates per-click analytics.
 *
 * <p>Capture runs on the {@code clickExecutor} pool ({@link Async}) so recording
 * the detailed event never adds latency to the redirect; failures are logged and
 * swallowed rather than breaking the user-facing 302.
 */
@Service
public class ClickAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(ClickAnalyticsService.class);

    /** Trailing window used for breakdowns and the daily time series. */
    private static final int WINDOW_DAYS = 30;
    private static final int TIMELINE_DAYS = 14;

    private final ClickEventRepository repository;

    public ClickAnalyticsService(ClickEventRepository repository) {
        this.repository = repository;
    }

    @Async("clickExecutor")
    @Transactional
    public void record(String shortCode, String referer, String userAgent) {
        try {
            UserAgents.Classification ua = UserAgents.classify(userAgent);
            String referrer = UserAgents.referrerHost(referer);
            repository.save(new ClickEvent(shortCode, Instant.now(), referrer, ua.device(), ua.browser()));
        } catch (Exception e) {
            log.warn("Failed to record click for '{}': {}", shortCode, e.toString());
        }
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse summarize(String shortCode) {
        Instant since = Instant.now().minus(WINDOW_DAYS, ChronoUnit.DAYS);
        List<ClickEvent> events = repository.findByShortCodeAndOccurredAtGreaterThanEqual(shortCode, since);

        long total = repository.countByShortCode(shortCode);

        return new AnalyticsResponse(
                shortCode,
                total,
                countBy(events, ClickEvent::getDevice),
                countBy(events, ClickEvent::getBrowser),
                countBy(events, ClickEvent::getReferrer),
                dailyTimeline(events)
        );
    }

    /** Counts events by a dimension, ordered by descending count for a clean UI. */
    private static Map<String, Long> countBy(List<ClickEvent> events, Function<ClickEvent, String> dimension) {
        return events.stream()
                .collect(Collectors.groupingBy(
                        e -> {
                            String v = dimension.apply(e);
                            return (v == null || v.isBlank()) ? "Unknown" : v;
                        },
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    /** Zero-filled daily counts for the trailing {@link #TIMELINE_DAYS} days, oldest first. */
    private static List<AnalyticsResponse.DayCount> dailyTimeline(List<ClickEvent> events) {
        Map<LocalDate, Long> perDay = events.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate(),
                        Collectors.counting()));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<AnalyticsResponse.DayCount> timeline = new ArrayList<>(TIMELINE_DAYS);
        for (int i = TIMELINE_DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            timeline.add(new AnalyticsResponse.DayCount(day.toString(), perDay.getOrDefault(day, 0L)));
        }
        return timeline;
    }
}
