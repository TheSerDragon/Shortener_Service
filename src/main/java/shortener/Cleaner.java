package shortener;

import java.util.concurrent.*;

public class Cleaner {
    private final ScheduledExecutorService sched = Executors.newSingleThreadScheduledExecutor();
    private final LinkStore store;
    private final NotificationService notifier;

    public Cleaner(LinkStore store, NotificationService notifier) {
        this.store = store;
        this.notifier = notifier;
    }

    public void startPeriodicClean(long interval, TimeUnit unit) {
        sched.scheduleAtFixedRate(() -> {
            try {
                store.removeExpiredAndNotify(notifier);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, interval, interval, unit);
    }

    public void stop() {
        sched.shutdownNow();
    }
}