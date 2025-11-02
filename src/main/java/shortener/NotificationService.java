package shortener;

public class NotificationService {
    // Для демонстрации — просто печатаем в консоль.
    public void notifyOwner(String ownerUuid, String message) {
        System.out.println("[NOTIFY] user=" + ownerUuid + " : " + message);
    }
}