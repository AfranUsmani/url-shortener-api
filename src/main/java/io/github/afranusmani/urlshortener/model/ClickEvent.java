package io.github.afranusmani.urlshortener.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A single redirect event, captured for analytics.
 *
 * <p>Deliberately privacy-friendly: it stores the referrer <em>host</em> and a
 * coarse device/browser classification derived from the User-Agent — never the
 * raw IP address or full user-agent string.
 */
@Entity
@Table(
        name = "click_event",
        indexes = @Index(name = "idx_click_event_short_code", columnList = "short_code")
)
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "click_event_seq")
    @SequenceGenerator(name = "click_event_seq", sequenceName = "click_event_seq", allocationSize = 50)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 64)
    private String shortCode;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "referrer", length = 255)
    private String referrer;

    @Column(name = "device", length = 16)
    private String device;

    @Column(name = "browser", length = 32)
    private String browser;

    protected ClickEvent() {
        // Required by JPA.
    }

    public ClickEvent(String shortCode, Instant occurredAt, String referrer, String device, String browser) {
        this.shortCode = shortCode;
        this.occurredAt = occurredAt;
        this.referrer = referrer;
        this.device = device;
        this.browser = browser;
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getDevice() {
        return device;
    }

    public String getBrowser() {
        return browser;
    }
}
