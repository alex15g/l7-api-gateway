import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Active Health Checker component.
 * Periodically pings backend nodes via TCP to ensure they are reachable.
 */
public class HealthChecker {
    private final List<String> allConfiguredNodes;
    private final RoundRobinLoadBalancer loadBalancer;

    public HealthChecker(List<String> allConfiguredNodes, RoundRobinLoadBalancer loadBalancer) {
        this.allConfiguredNodes = allConfiguredNodes;
        this.loadBalancer = loadBalancer;
    }

    /**
     * Starts the background scheduler that runs health checks every 5 seconds.
     */
    public void start() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::performHealthCheck, 0, 5, TimeUnit.SECONDS);
    }

    private void performHealthCheck() {
        List<String> healthyNodes = new ArrayList<>();

        System.out.println("\n[HEALTH-CHECK] Running system diagnostics...");

        for (String node : allConfiguredNodes) {
            if (isNodeAlive(node)) {
                healthyNodes.add(node);
            } else {
                System.out.println("[HEALTH-CHECK] ALERT: Node " + node + " is DOWN! Removing from rotation.");
            }
        }

        // Update the load balancer with only the nodes that successfully responded
        loadBalancer.updateActiveNodes(healthyNodes);
    }

    /**
     * Attempts to establish a TCP connection to the specified host and port.
     *
     * @param node The backend node in "host:port" format.
     * @return true if the connection is successful, false otherwise.
     */
    private boolean isNodeAlive(String node) {
        try {
            String[] parts = node.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            // Try to open a socket with a 2-second timeout
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 2000);
                return true;
            }
        } catch (Exception e) {
            // If an exception is thrown, the port is closed or the server is unreachable
            return false;
        }
    }
}