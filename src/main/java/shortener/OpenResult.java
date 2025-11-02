package shortener;

public class OpenResult {
    public enum Status { SUCCESS, NOT_FOUND, EXPIRED, EXHAUSTED }

    private final Status status;
    private final String url;
    private final int clicks;
    private final int maxClicks;

    private OpenResult(Status status, String url, int clicks, int maxClicks) {
        this.status = status;
        this.url = url;
        this.clicks = clicks;
        this.maxClicks = maxClicks;
    }

    public static OpenResult success(String url, int clicks, int maxClicks) {
        return new OpenResult(Status.SUCCESS, url, clicks, maxClicks);
    }
    public static OpenResult notFound() { return new OpenResult(Status.NOT_FOUND, null, 0, 0); }
    public static OpenResult expired() { return new OpenResult(Status.EXPIRED, null, 0, 0); }
    public static OpenResult exhausted() { return new OpenResult(Status.EXHAUSTED, null, 0, 0); }

    public Status getStatus() { return status; }
    public String getUrl() { return url; }
    public int getClicks() { return clicks; }
    public int getMaxClicks() { return maxClicks; }
}