package shortener;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    private static final String UUID_STORE = ".shortener-uuid";

    public static void main(String[] args) throws Exception {
        System.out.println("=== Shortener Service (console) ===");

        // Загружаем конфиг
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("config/app.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.out.println("Не удалось загрузить config/app.properties, используются значения по умолчанию.");
        }

        long ttl = Long.parseLong(props.getProperty("link.ttl.default", "86400"));
        int cleanupInterval = Integer.parseInt(props.getProperty("cleanup.interval", "60"));
        int defaultMaxClicks = Integer.parseInt(props.getProperty("link.maxClicks.default", "10"));

        // Инициализация сервисов
        UserService userService = new UserService(UUID_STORE);
        String userUuid = userService.getOrCreateUserUuid();

        System.out.println("UUID пользователя: " + userUuid);
        System.out.println("TTL ссылки по умолчанию (секунды): " + ttl);
        System.out.println("Интервал очистки (секунды): " + cleanupInterval);
        System.out.println("Для получения команд введите 'help'.");

        // Передаём TTL и лимиты в сервисы
        LinkStore store = new LinkStore(ttl, defaultMaxClicks);
        NotificationService notifier = new NotificationService();
        ShortenerService shortener = new ShortenerService(store, notifier);

        // Запускаем очистку
        Cleaner cleaner = new Cleaner(store, notifier);
        cleaner.startPeriodicClean(cleanupInterval, TimeUnit.SECONDS);

        // Запуск CLI
        ConsoleScanner console = new ConsoleScanner(shortener, store, userService, notifier);
        console.run();

        cleaner.stop();
        System.out.println("Выход.");
        System.exit(0);
    }
}