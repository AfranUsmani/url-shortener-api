package io.github.afranusmani.urlshortener.service;

import java.net.URI;

/**
 * Tiny, dependency-free heuristics for turning request metadata into the coarse
 * dimensions we store for analytics. Intentionally approximate — good enough for
 * "which device / browser / source" breakdowns without shipping a UA database.
 */
public final class UserAgents {

    /** Coarse classification of a single click. */
    public record Classification(String device, String browser) {
    }

    private UserAgents() {
    }

    public static Classification classify(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new Classification("Unknown", "Unknown");
        }
        return new Classification(device(userAgent), browser(userAgent));
    }

    private static String device(String ua) {
        String s = ua.toLowerCase();
        if (s.contains("bot") || s.contains("crawl") || s.contains("spider")
                || s.contains("curl") || s.contains("wget") || s.contains("httpie")
                || s.contains("python") || s.contains("java/") || s.contains("okhttp")
                || s.contains("postman") || s.contains("insomnia")) {
            return "Bot";
        }
        if (s.contains("ipad") || s.contains("tablet")) {
            return "Tablet";
        }
        if (s.contains("mobi") || s.contains("iphone") || s.contains("ipod") || s.contains("android")) {
            return "Mobile";
        }
        return "Desktop";
    }

    private static String browser(String ua) {
        String s = ua.toLowerCase();
        if (s.contains("edg")) return "Edge";
        if (s.contains("opr") || s.contains("opera")) return "Opera";
        if (s.contains("samsungbrowser")) return "Samsung";
        if (s.contains("firefox") || s.contains("fxios")) return "Firefox";
        if (s.contains("chrome") || s.contains("crios")) return "Chrome";
        if (s.contains("safari")) return "Safari";
        if (s.contains("curl")) return "curl";
        return "Other";
    }

    /**
     * Reduces a Referer header to its host ("Direct" when absent/unparseable),
     * stripping a leading {@code www.} so sources group cleanly.
     */
    public static String referrerHost(String referer) {
        if (referer == null || referer.isBlank()) {
            return "Direct";
        }
        try {
            String host = URI.create(referer.trim()).getHost();
            if (host == null || host.isBlank()) {
                return "Direct";
            }
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (IllegalArgumentException e) {
            return "Other";
        }
    }
}
