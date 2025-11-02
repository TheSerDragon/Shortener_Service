package shortener;

public class Config {
    private long defaultTTL;
    private long cleanupInterval;

    // Константа для дефолтного TTL
    public static final long DEFAULT_TTL_SECONDS = 24 * 3600;

    public long getDefaultTTL() {
        return defaultTTL;
    }

    public long getCleanupInterval() {
        return cleanupInterval;
    }

    // Статический метод load() возвращает конфигурацию по умолчанию
    public static Config load() {
        Config cfg = new Config();
        cfg.defaultTTL = DEFAULT_TTL_SECONDS;
        cfg.cleanupInterval = 3600;
        return cfg;
    }
}