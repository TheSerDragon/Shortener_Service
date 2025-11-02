package shortener;

import java.time.Instant;
import java.util.Random;

public class ShortenerService {
    private final LinkStore store;
    private final NotificationService notifier;
    private final Random rnd = new Random();

    private static final String DOMAIN = "clck.ru/";
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LEN = 7;

    public ShortenerService(LinkStore store, NotificationService notifier) {
        this.store = store;
        this.notifier = notifier;
    }

    public String create(String originalUrl, String ownerUuid, int maxClicks) {
        // генерируем уникальный код; учитываем ownerUuid для уникальности
        String code;
        do {
            code = generateCode(ownerUuid);
        } while (store.existsCode(code));

        Instant now = Instant.now();

        // Берем TTL и лимит из конфига (LinkStore)
        long ttl = store.getDefaultTtlSeconds();
        int max = (maxClicks > 0) ? maxClicks : store.getDefaultMaxClicks();

        Instant expires = now.plusSeconds(ttl);

        Link link = new Link(code, originalUrl, ownerUuid, now, expires, max);
        store.add(link);

        return DOMAIN + code;
    }

    private String generateCode(String ownerUuid) {
        long seed = System.nanoTime() ^ ownerUuid.hashCode();
        rnd.setSeed(seed + rnd.nextInt());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    // Переход по короткому коду: возвращает результат и уведомляет владельца при необходимости.

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