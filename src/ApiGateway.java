import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * Main entry point for the L7 API Gateway.
 * Orchestrates networking, load balancing, and rate limiting components.
 */
public class ApiGateway {

    private static final int GATEWAY_PORT = 8080;
    private static final int MAX_REQUESTS_PER_SECOND = 50;
    private static final int THREAD_POOL_SIZE = 100;

    // We use a mix of reliable servers and a guaranteed failing one for demonstration purposes
    private static final List<String> BACKEND_SERVERS = List.of(
            "google.com:80",
            "cloudflare.com:80",
            "localhost:9999" // This server does not exist, so it will be marked as DOWN
    );

    // Core components initialized (Dependency Injection pattern)
    private static final RateLimiter rateLimiter = new RateLimiter(MAX_REQUESTS_PER_SECOND);
    private static final RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(BACKEND_SERVERS);
    private static final HealthChecker healthChecker = new HealthChecker(BACKEND_SERVERS, loadBalancer);

    // Thread pool for handling concurrent non-blocking I/O operations
    private static final ExecutorService requestHandlers = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    private static final ConfigWatcher configWatcher = new ConfigWatcher(rateLimiter);

    public static void main(String[] args) {
        startRateLimiterScheduler();
        healthChecker.start();
        configWatcher.start(); // START THE HOT RELOAD DAEMON
        startServer();
    }

    private static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(GATEWAY_PORT)) {
            System.out.println("[GATEWAY] Started on port " + GATEWAY_PORT + ". Awaiting connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Immediately offload the request to a worker thread to keep the main thread unblocked
                requestHandlers.execute(() -> handleClientRequest(clientSocket));
            }
        } catch (Exception e) {
            System.err.println("[GATEWAY] Critical Server Error: " + e.getMessage());
        }
    }

    private static void handleClientRequest(Socket clientSocket) {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        // Generăm un ID unic global pentru acest request (Distributed Tracing)
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try (clientSocket;
             InputStream input = clientSocket.getInputStream();
             OutputStream output = clientSocket.getOutputStream()) {

            // 1. Security Check: Rate Limiting
            if (!rateLimiter.isAllowed(clientIp)) {
                System.out.printf("[%s] [SECURITY] Rate limit exceeded for IP: %s%n", correlationId, clientIp);
                sendHttpResponse(output, 429, "Too Many Requests", "Rate limit exceeded. Try again later.");
                return;
            }

            // Read incoming HTTP request
            byte[] buffer = new byte[2048];
            int bytesRead = input.read(buffer);
            if (bytesRead == -1) return;

            // 2. Routing: Load Balancing
            String targetNode = loadBalancer.getNextNode();

            // Calculăm cât timp a durat procesarea în interiorul Gateway-ului
            long processingTimeMs = System.currentTimeMillis() - startTime;

            // Audit Log profesionist cu Correlation ID și Processing Time
            System.out.printf("[%s] [ROUTER] %s -> %s (Processed in %dms)%n",
                    correlationId, clientIp, targetNode, processingTimeMs);

            // 3. Mock Downstream Response (Injectăm Correlation ID-ul ca să-l vadă și clientul)
            String jsonResponse = "{\n" +
                    "  \"status\": \"success\",\n" +
                    "  \"routedTo\": \"" + targetNode + "\",\n" +
                    "  \"correlationId\": \"" + correlationId + "\"\n" +
                    "}";

            sendHttpResponse(output, 200, "OK", jsonResponse);

        } catch (Exception e) {
            System.err.printf("[%s] [ERROR] Connection handling error: %s%n", correlationId, e.getMessage());
        }
    }

    /**
     * Background task to clear the rate limiter token buckets every second.
     */
    private static void startRateLimiterScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(rateLimiter::reset, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Constructs and sends a raw HTTP response back to the client.
     */
    private static void sendHttpResponse(OutputStream output, int statusCode, String statusText, String body) throws Exception {
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: application/json\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                body;
        output.write(response.getBytes());
        output.flush();
    }
}