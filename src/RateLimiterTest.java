import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the RateLimiter component.
 * Validates the token bucket logic and thread-reset capabilities.
 */
class RateLimiterTest {

    private RateLimiter rateLimiter;
    private static final int MAX_REQUESTS = 50;
    private static final String TEST_IP = "192.168.1.100";

    @BeforeEach
    void setUp() {
        // Initialize a fresh RateLimiter before each test
        rateLimiter = new RateLimiter(MAX_REQUESTS);
    }

    @Test
    void shouldAllowRequestsUnderLimit() {
        // Act & Assert
        for (int i = 1; i <= MAX_REQUESTS; i++) {
            assertTrue(rateLimiter.isAllowed(TEST_IP), "Request " + i + " should be allowed.");
        }
    }

    @Test
    void shouldBlockRequestsOverLimit() {
        // Arrange: Consume all allowed tokens
        for (int i = 0; i < MAX_REQUESTS; i++) {
            rateLimiter.isAllowed(TEST_IP);
        }

        // Act
        boolean isAllowedAfterLimit = rateLimiter.isAllowed(TEST_IP);

        // Assert
        assertFalse(isAllowedAfterLimit, "The 51st request MUST be blocked by the rate limiter.");
    }

    @Test
    void shouldResetCountersCorrectly() {
        // Arrange: Consume all tokens and verify blocking
        for (int i = 0; i < MAX_REQUESTS; i++) {
            rateLimiter.isAllowed(TEST_IP);
        }
        assertFalse(rateLimiter.isAllowed(TEST_IP), "Should be blocked before reset.");

        // Act: Trigger the scheduled reset manually for testing
        rateLimiter.reset();

        // Assert: The IP should be allowed again
        assertTrue(rateLimiter.isAllowed(TEST_IP), "Should be allowed after reset.");
    }
}