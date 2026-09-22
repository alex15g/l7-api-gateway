import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dynamic Round-Robin Load Balancer.
 * Supports safely adding or removing backend nodes at runtime using CopyOnWriteArrayList.
 */
public class RoundRobinLoadBalancer {
    // Thread-safe list optimized for scenarios where traversals vastly outnumber mutations
    private final List<String> activeNodes = new CopyOnWriteArrayList<>();
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public RoundRobinLoadBalancer(List<String> initialNodes) {
        this.activeNodes.addAll(initialNodes);
    }

    /**
     * Safely replaces the current list of active nodes with a new list of healthy nodes.
     *
     * @param healthyNodes The updated list of reachable backend servers.
     */
    public void updateActiveNodes(List<String> healthyNodes) {
        activeNodes.clear();
        activeNodes.addAll(healthyNodes);
    }

    /**
     * Retrieves the next available backend node using a round-robin algorithm.
     *
     * @return The IP/Hostname of the target node, or an error message if all nodes are down.
     */
    public String getNextNode() {
        if (activeNodes.isEmpty()) {
            return "ERROR: 503 Service Unavailable - All backend servers are down!";
        }
        int index = Math.abs(currentIndex.getAndIncrement()) % activeNodes.size();
        return activeNodes.get(index);
    }
}