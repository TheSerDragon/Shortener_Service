package shortener;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShortenerServiceTest {

    @Test
    void testCreateLink() {
        LinkStore store = new LinkStore() {
            @Override
            public boolean existsCode(String code) { return false; }
            @Override
            public void add(Link link) {}
            @Override
            public Link getByCode(String code) { return null; }
            @Override
            public void remove(String code) {}
            @Override
            public long getDefaultTtlSeconds() { return 3600; }
            @Override
            public int getDefaultMaxClicks() { return 10; }
        };

        NotificationService notifier = new NotificationService() {
            @Override
            public void notifyOwner(String ownerUuid, String message) {
                // Пустая реализация
            }
        };

        ShortenerService service = new ShortenerService(store, notifier);

        String url = "https://example.com";
        String owner = "user123";
        String shortUrl = service.create(url, owner, 0);

        assertNotNull(shortUrl);
        assertTrue(shortUrl.startsWith("clck.ru/"));
    }
}