package shortener;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LinkStore — хранилище ссылок, управляет добавлением, удалением и очисткой.
 */

public class LinkStore {
    private final ConcurrentHashMap<String, Link> byCode = new ConcurrentHashMap<>();

    // Добавляем поля конфигурации
    private final long defaultTtlSeconds;
    private final int defaultMaxClicks;

    // Конструктор с параметрами из конфига
    public LinkStore(long defaultTtlSeconds, int defaultMaxClicks) {
        this.defaultTtlSeconds = defaultTtlSeconds;
        this.defaultMaxClicks = defaultMaxClicks;
    }

    // Конструктор по умолчанию
    public LinkStore() {
        this(86400, 10);
    }

    public boolean existsCode(String code) {
        return byCode.containsKey(code);
    }

    public void add(Link link) {
        byCode.put(link.getShortCode(), link);
    }

    public Link getByCode(String code) {
        return byCode.get(code);
    }

    public List<Link> getByOwner(String ownerUuid) {
        List<Link> res = new ArrayList<>();
        for (Link l : byCode.values()) {
            if (l.getOwnerUuid().equals(ownerUuid)) res.add(l);
        }
        return res;
    }

    public void remove(String code) {
        byCode.remove(code);
    }

    public Collection<Link> all() {
        return byCode.values();
    }

    public void removeExpiredAndNotify(NotificationService notifier) {
        Instant now = Instant.now();
        List<String> toRemove = new ArrayList<>();
        for (Link l : byCode.values()) {
            if (l.isExpired()) {
                toRemove.add(l.getShortCode());
                notifier.notifyOwner(l.getOwnerUuid(), "Ссылка устарела и удалена: " + l.getShortCode());
            }
        }
        for (String code : toRemove) byCode.remove(code);
    }

    // Геттеры для использования из других сервисов
    public long getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }

    public int getDefaultMaxClicks() {
        return defaultMaxClicks;
    }

    public Link createLink(String originalUrl, int maxClicks, String ownerUuid) {
        // генерируем короткий код
        String code = UUID.randomUUID().toString().substring(0, 7);
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(defaultTtlSeconds);

        int clicks = (maxClicks > 0) ? maxClicks : defaultMaxClicks;

        Link link = new Link(code, originalUrl, ownerUuid, now, expires, clicks);
        add(link);
        return link;
    }

    public String getOriginalUrl(String code) {
        Link link = byCode.get(code);
        return (link != null) ? link.getOriginalUrl() : null;
    }
}
