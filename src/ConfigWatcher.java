import java.io.FileInputStream;
import java.io.File;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background daemon that watches for configuration changes and applies them without restarting the Gateway (Hot Reloading).
 */
public class ConfigWatcher {
    private static final String CONFIG_FILE = "gateway.properties";
    private final RateLimiter rateLimiter;
    private long lastModifiedTime = 0;

    public ConfigWatcher(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public void start() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkForUpdates, 0, 3, TimeUnit.SECONDS);
    }

    private void checkForUpdates() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) return;

        long currentModifiedTime = file.lastModified();

        // Only read the file if it was actually saved/modified since the last check
        if (currentModifiedTime > lastModifiedTime) {
            lastModifiedTime = currentModifiedTime;

            try (FileInputStream input = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(input);

                String limitStr = props.getProperty("rate.limit");
                if (limitStr != null) {
                    int newLimit = Integer.parseInt(limitStr.trim());
                    rateLimiter.setMaxRequestsPerSecond(newLimit);
                }
            } catch (Exception e) {
                System.err.println("[CONFIG] Failed to load configuration: " + e.getMessage());
            }
        }
    }
}