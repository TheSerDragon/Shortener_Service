package shortener;

import java.io.*;
import java.util.UUID;

public class UserService {
    private final String storagePath;

    public UserService(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getOrCreateUserUuid() {
        File f = new File(storagePath);
        if (f.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String uuid = r.readLine();
                if (uuid != null && !uuid.isBlank()) return uuid.trim();
            } catch (Exception ignored) {}
        }
        String uuid = UUID.randomUUID().toString();
        try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
            w.write(uuid);
        } catch (Exception ignored) {}
        return uuid;
    }
}