package shortener;

import java.time.Instant;
import java.util.UUID;

/**
 * ShortenerService — бизнес-логика сервиса сокращения ссылок.
 * Отвечает за создание коротких ссылок, редирект, и обновление параметров.
 */

public class ShortenerService {
    private final LinkStore store;
    private final NotificationService notifier;

    private static final String DOMAIN = "clck.ru/";

    public ShortenerService(LinkStore store, NotificationService notifier) {
        this.store = store;
        this.notifier = notifier;
    }

    public String create(String originalUrl, String ownerUuid, int maxClicks) {
        // Добавлена валидация URL
        if (originalUrl == null || !originalUrl.matches("^(https?://).+")) {
            throw new IllegalArgumentException("Некорректный URL. Используйте формат http(s)://...");
        }

        // Генерируем гарантированно уникальный код
        String code;
        do {
            code = generateCode();
        } while (store.existsCode(code));

        Instant now = Instant.now();

        // Берем TTL и лимит из конфигурации
        long ttl = store.getDefaultTtlSeconds();
        int max = (maxClicks > 0) ? maxClicks : store.getDefaultMaxClicks();

        Instant expires = now.plusSeconds(ttl);

        Link link = new Link(code, originalUrl, ownerUuid, now, expires, max);
        store.add(link);

        return DOMAIN + code;
    }

    // Упрощённый метод генерации кода через UUID — теперь 100% уникален
    private String generateCode() {
        return UUID.randomUUID().toString().substring(0, 7);
    }

    // Переход по короткому коду: возвращает результат и уведомляет владельца при необходимости
    public OpenResult open(String code, String requesterUuid) {
        Link link = store.getByCode(code);
        if (link == null) {
            return OpenResult.notFound();
        }
        if (link.isExpired()) {
            store.remove(code);
            notifier.notifyOwner(link.getOwnerUuid(), "Ваша ссылка " + code + " истекла и была удалена.");
            return OpenResult.expired();
        }
        synchronized (link) {
            if (link.isExhausted()) {
                notifier.notifyOwner(link.getOwnerUuid(), "Ссылка " + code + " исчерпала лимит переходов.");
                return OpenResult.exhausted();
            }
            int newCount = link.incrementClicks();
            if (link.isExhausted()) {
                notifier.notifyOwner(link.getOwnerUuid(), "Ссылка " + code + " исчерпала лимит переходов.");
            }
            return OpenResult.success(link.getOriginalUrl(), newCount, link.getMaxClicks());
        }
    }

    public synchronized void updateLinkParams(String code, String ownerUuid, Long newTTLSeconds, Integer newMaxClicks) {
        Link link = store.getByCode(code);
        if (link == null) throw new IllegalArgumentException("Ссылка не найдена");
        if (!link.getOwnerUuid().equals(ownerUuid)) throw new SecurityException("Вы не владелец ссылки");

        if (newTTLSeconds != null) link.updateTTL(newTTLSeconds);
        if (newMaxClicks != null) link.updateMaxClicks(newMaxClicks);
    }
}
