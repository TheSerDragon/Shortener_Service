package shortener;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {
    @Test
    void testConfigLoad() {
        Config cfg = Config.load();
        assertTrue(cfg.getDefaultTTL() > 0);
        assertTrue(cfg.getCleanupInterval() > 0);
    }
}