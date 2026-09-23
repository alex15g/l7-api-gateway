import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory Rate Limiter using a simplified Token Bucket algorithm.
 * Ensures thread-safe operation across multiple concurrent client requests.
 */
public class RateLimiter {
    // 'volatile' ensures that thread caches are bypassed and the updated value is seen immediately by all worker threads
    private volatile int maxRequestsPerSecond;
    private final Map<String, AtomicInteger> clientRequestCounts = new ConcurrentHashMap<>();

    public RateLimiter(int initialLimit) {
        this.maxRequestsPerSecond = initialLimit;
    }

    /**
     * Updates the rate limit dynamically at runtime without restarting the server.
     */
    public void setMaxRequestsPerSecond(int newLimit) {
        this.maxRequestsPerSecond = newLimit;
        System.out.println("[CONFIG] Rate limit dynamically updated to: " + newLimit + " req/sec");
    }

    /**
     * Evaluates whether a request from the given IP address is allowed.
     *
     * @param ipAddress The client's IP address.
     * @return true if the request is within the allowed limit, false otherwise.
     */
    public boolean isAllowed(String ipAddress) {
        // Initialize the counter for a new IP in a thread-safe manner
        clientRequestCounts.putIfAbsent(ipAddress, new AtomicInteger(0));

        // Atomically increment and get the current request count to prevent race conditions
        int currentRequests = clientRequestCounts.get(ipAddress).incrementAndGet();

        return currentRequests <= maxRequestsPerSecond;
    }

    /**
     * Clears the current request counts.
     * Designed to be called by a background scheduler every second.
     */
    public void reset() {
        clientRequestCounts.clear();
    }
}