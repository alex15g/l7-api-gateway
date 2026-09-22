import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * L7 API Gateway & Load Balancer
 * Features: Non-blocking Thread Pool, Round-Robin Routing, In-Memory Rate Limiting
 */
public class ApiGateway {

    private static final int GATEWAY_PORT = 8080;
    private static final int MAX_REQUESTS_PER_SECOND = 50;

    // Thread pool for handling concurrent client connections
    private static final ExecutorService requestHandlers = Executors.newFixedThreadPool(100);

    // Rate Limiter state: Maps IP address to current request count
    private static final Map<String, AtomicInteger> rateLimiterMap = new ConcurrentHashMap<>();

    // Downstream backend servers for Load Balancing
    private static final List<String> backendServers = List.of("Backend-Node-1", "Backend-Node-2", "Backend-Node-3");
    private static final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    public static void main(String[] args) {
        startRateLimiterResetTask();
        startGateway();
    }

    private static void startGateway() {
        try (ServerSocket serverSocket = new ServerSocket(GATEWAY_PORT)) {
            System.out.println("[GATEWAY] Started on port " + GATEWAY_PORT + ". Awaiting connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Delegate connection to a worker thread immediately to prevent blocking
                requestHandlers.execute(() -> handleClientRequest(clientSocket));
            }
        } catch (Exception e) {
            System.err.println("[GATEWAY] Critical Failure: " + e.getMessage());
        }
    }

    private static void handleClientRequest(Socket clientSocket) {
        String clientIp = clientSocket.getInetAddress().getHostAddress();

        try (clientSocket;
             InputStream input = clientSocket.getInputStream();
             OutputStream output = clientSocket.getOutputStream()) {

            // 1. RATE LIMITING CHECK (The Security Guard)
            if (!isRequestAllowed(clientIp)) {
                System.out.println("[SECURITY] Rate limit exceeded for IP: " + clientIp);
                sendHttpResponse(output, 429, "Too Many Requests", "Rate limit exceeded. Try again later.");
                return;
            }

            // Read the incoming HTTP request (Simulated read for MVP)
            byte[] buffer = new byte[2048];
            int bytesRead = input.read(buffer);
            if (bytesRead == -1) return;

            // 2. LOAD BALANCING (The Traffic Director)
            String targetBackend = getNextBackendNode();
            System.out.println("[ROUTER] Routing request from " + clientIp + " -> " + targetBackend);

            // 3. FORWARDING (Simulating the downstream response)
            // In a real proxy, we would open a new Socket to the targetBackend IP/Port here.
            String mockResponsePayload = "{\n  \"status\": \"success\",\n  \"servedBy\": \"" + targetBackend + "\"\n}";
            sendHttpResponse(output, 200, "OK", mockResponsePayload);

        } catch (Exception e) {
            System.err.println("[ROUTER] Connection error handling client: " + e.getMessage());
        }
    }

    /**
     * Checks if the client IP has exceeded the allowed requests per second.
     * Thread-safe operation using ConcurrentHashMap and AtomicInteger.
     */
    private static boolean isRequestAllowed(String ipAddress) {
        rateLimiterMap.putIfAbsent(ipAddress, new AtomicInteger(0));
        int currentRequests = rateLimiterMap.get(ipAddress).incrementAndGet();
        return currentRequests <= MAX_REQUESTS_PER_SECOND;
    }

    /**
     * Round-Robin algorithm for distributing load across backend servers.
     */
    private static String getNextBackendNode() {
        // getAndIncrement is an atomic, thread-safe operation
        int index = Math.abs(roundRobinIndex.getAndIncrement()) % backendServers.size();
        return backendServers.get(index);
    }

    /**
     * Background task that resets the rate limiter counts every second.
     */
    private static void startRateLimiterResetTask() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> rateLimiterMap.clear(), 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Helper method to construct and send raw HTTP responses.
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