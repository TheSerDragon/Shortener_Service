package shortener;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

public class LinkStoreTest {

    @Test
    void testAddAndGetLink() {
        LinkStore store = new LinkStore();
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(3600);

        Link link = new Link("abc123", "https://example.com", "test-user", now, expires, 5);
        store.add(link);

        // Получаем код из объекта Link
        String code = link.getShortCode();

        // Проверяем, что ссылка успешно добавлена и извлекается
        Link retrieved = store.getByCode(code);
        assertNotNull(retrieved);
        assertEquals("https://example.com", retrieved.getOriginalUrl());
        assertEquals("test-user", retrieved.getOwnerUuid());
    }
}