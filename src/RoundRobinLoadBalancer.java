import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dynamic Round-Robin Load Balancer.
 * Supports adding/removing backend nodes at runtime safely using CopyOnWriteArrayList.
 */
public class RoundRobinLoadBalancer {
    // Thread-safe list perfect for cases where reading is frequent, but writing is rare.
    private final List<String> activeNodes = new CopyOnWriteArrayList<>();
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public RoundRobinLoadBalancer(List<String> initialNodes) {
        this.activeNodes.addAll(initialNodes);
    }

    /**
     * Replaces the current list of active nodes with a new healthy list.
     */
    public void updateActiveNodes(List<String> healthyNodes) {
        activeNodes.clear();
        activeNodes.addAll(healthyNodes);
    }

    public String getNextNode() {
        if (activeNodes.isEmpty()) {
            return "ERROR: 503 Service Unavailable - All backends are down!";
        }
        int index = Math.abs(currentIndex.getAndIncrement()) % activeNodes.size();
        return activeNodes.get(index);
    }
}