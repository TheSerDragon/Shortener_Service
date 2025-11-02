package shortener;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Scanner;

public class ConsoleScanner {
    private final ShortenerService service;
    private final LinkStore store;
    private final UserService userService;
    private final NotificationService notifier;

    public ConsoleScanner(ShortenerService service, LinkStore store, UserService userService, NotificationService notifier) {
        this.service = service;
        this.store = store;
        this.userService = userService;
        this.notifier = notifier;
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        String me = userService.getOrCreateUserUuid();
        while (true) {
            System.out.print("> ");
            String line;
            try {
                if (!sc.hasNextLine()) break;
                line = sc.nextLine().trim();
            } catch (Exception e) { break; }
            if (line.isBlank()) continue;
            String[] parts = line.split("\\s+", 2);
            String cmd = parts[0].toLowerCase();
            String arg = parts.length > 1 ? parts[1].trim() : "";
            try {
                switch (cmd) {
                    case "help": printHelp(); break;
                    case "create":
                        if (arg.isBlank()) { System.out.println("Введите: create <longUrl> [maxClicks]"); break; }
                        String[] a = arg.split("\\s+");
                        String url = a[0];
                        int max = 10;
                        if (a.length > 1) {
                            try { max = Integer.parseInt(a[1]); }
                            catch (Exception ex) { System.out.println("Неверное значение maxClicks; используется по умолчанию 10."); }
                        }
                        String shortLink = service.create(url, me, max);
                        System.out.println("Короткая ссылка: " + shortLink + " (maxClicks=" + max + ")");
                        break;
                    case "list":
                        List<Link> my = store.getByOwner(me);
                        if (my.isEmpty()) System.out.println("Нет ссылок.");
                        else {
                            System.out.printf("%-12s %-40s %-20s %-8s %-10s%n", "short", "original", "expiresAt", "clicks", "status");
                            for (Link l : my) {
                                System.out.printf("%-12s %-40s %-20s %4d/%-4d %-10s%n",
                                        l.getShortCode(), shorten(l.getOriginalUrl(),40), l.getExpiresAt(), l.getClickCount(), l.getMaxClicks(), l.getStatus());
                            }
                        }
                        break;
                    case "open":
                        if (arg.isBlank()) { System.out.println("Введите: open <shortCode>"); break; }
                        String code = extractCode(arg);
                        OpenResult res = service.open(code, me);
                        switch (res.getStatus()) {
                            case NOT_FOUND: System.out.println("Не найдено."); break;
                            case EXPIRED: System.out.println("Срок действия ссылки истек."); break;
                            case EXHAUSTED: System.out.println("Ссылка исчерпана (достигнуто максимальное количество клкиков)."); break;
                            case SUCCESS:
                                System.out.println("Открытие: " + res.getUrl() + " (" + res.getClicks() + "/" + res.getMaxClicks() + ")");
                                try {
                                    if (Desktop.isDesktopSupported()) {
                                        Desktop.getDesktop().browse(new URI(res.getUrl()));
                                    } else {
                                        System.out.println("Рабочий стол не поддерживается; невозможно открыть браузер.");
                                    }
                                } catch (Exception e) {
                                    System.out.println("Не удалось открыть браузер: " + e.getMessage());
                                }
                                break;
                        }
                        break;
                    case "update":
                        if (arg.isBlank()) {
                            System.out.println("Введите: update <code> ttl=[seconds] max=[clicks]");
                            break;
                        }
                        String[] u = arg.split("\\s+");
                        String ucode = extractCode(u[0]);
                        Long newTTL = null;
                        Integer newMax = null;
                        for (int i = 1; i < u.length; i++) {
                            String part = u[i].toLowerCase();
                            if (part.startsWith("ttl=")) {
                                try { newTTL = Long.parseLong(part.substring(4)); }
                                catch (Exception ex) { System.out.println("Неверное значение TTL; игнорируется."); }
                            } else if (part.startsWith("max=")) {
                                try { newMax = Integer.parseInt(part.substring(4)); }
                                catch (Exception ex) { System.out.println("Неверное значение maxClicks; игнорируется."); }
                            } else {
                                System.out.println("Неизвестный параметр: " + part);
                            }
                        }
                        try {
                            service.updateLinkParams(ucode, me, newTTL, newMax);
                            System.out.println("Параметры ссылки обновлены.");
                        } catch (Exception ex) {
                            System.out.println("Ошибка обновления: " + ex.getMessage());
                        }
                        break;
                    case "delete":
                        if (arg.isBlank()) { System.out.println("Введите: delete <code>"); break; }
                        String dcode = extractCode(arg);
                        Link l = store.getByCode(dcode);
                        if (l == null) { System.out.println("Не найдено."); break; }
                        if (!l.getOwnerUuid().equals(me)) { System.out.println("Запрещено: удалить может только владелец."); break; }
                        store.remove(dcode);
                        System.out.println("Удалено " + dcode);
                        break;
                    case "stats":
                        if (arg.isBlank()) { System.out.println("Введите: stats <code>"); break; }
                        String scode = extractCode(arg);
                        Link s = store.getByCode(scode);
                        if (s == null) { System.out.println("Не найдено."); break; }
                        System.out.println("Short: " + s.getShortCode());
                        System.out.println("Original: " + s.getOriginalUrl());
                        System.out.println("Owner: " + s.getOwnerUuid());
                        System.out.println("Created: " + s.getCreatedAt());
                        System.out.println("Expires: " + s.getExpiresAt());
                        System.out.println("Clicks: " + s.getClickCount() + " / " + s.getMaxClicks());
                        System.out.println("Status: " + s.getStatus());
                        break;
                    case "exit":
                        return;
                    default:
                        System.out.println("Неизвестная команда. Введите 'help'.");
                }
            } catch (Exception ex) {
                System.out.println("Ошибка: " + ex.getMessage());
            }
        }
    }

    private void printHelp() {
        System.out.println("Команды:");
        System.out.println(" create <longUrl> [maxClicks]  - создать короткую ссылку (maxClicks по умолчанию 10)");
        System.out.println(" list                          - вывести список ссылок");
        System.out.println(" open <code|clck.ru/code>      - открыть короткую ссылку");
        System.out.println(" delete <code>                 - удалить ссылку");
        System.out.println(" stats <code>                  - показать статистику ссылок");
        System.out.println(" update <code> ttl=[Seconds] max=[Clicks] - изменить TTL и/или лимит переходов (только владелец)");
        System.out.println(" help                          - справка команд");
        System.out.println(" exit                          - выход");
    }

    private String extractCode(String arg) {
        if (arg.contains("/")) {
            String[] p = arg.split("/");
            return p[p.length - 1];
        }
        return arg;
    }

    private String shorten(String s, int n) {
        if (s.length() <= n) return s;
        return s.substring(0, n-3) + "...";
    }
}