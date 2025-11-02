package shortener;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class Link {
    private final String shortCode;
    private final String originalUrl;
    private final String ownerUuid;
    private final Instant createdAt;
    private Instant expiresAt;
    private int maxClicks;
    private final AtomicInteger clickCount = new AtomicInteger(0);

    public Link(String shortCode, String originalUrl, String ownerUuid, Instant createdAt, Instant expiresAt, int maxClicks) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.ownerUuid = ownerUuid;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.maxClicks = maxClicks;
    }

    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public String getOwnerUuid() { return ownerUuid; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getMaxClicks() { return maxClicks; }
    public int getClickCount() { return clickCount.get(); }

    public int incrementClicks() { return clickCount.incrementAndGet(); }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isExhausted() {
        return getClickCount() >= maxClicks;
    }

    public String getStatus() {
        if (isExpired()) return "EXPIRED";
        if (isExhausted()) return "EXHAUSTED";
        return "ACTIVE";
    }

    public synchronized void updateMaxClicks(int newMax) {
        if (newMax < clickCount.get())
            throw new IllegalArgumentException("Новый лимит меньше уже совершённых кликов");
        if (newMax <= 0)
            throw new IllegalArgumentException("Лимит должен быть положительным");
        this.maxClicks = newMax;
    }

    public synchronized void updateTTL(long newTTLSeconds) {
        if (newTTLSeconds <= 0)
            throw new IllegalArgumentException("TTL должен быть положительным");
        this.expiresAt = Instant.now().plusSeconds(newTTLSeconds);
    }

}