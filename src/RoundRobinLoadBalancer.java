import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe Round-Robin Load Balancer.
 * Distributes incoming requests sequentially across available backend nodes.
 */
public class RoundRobinLoadBalancer {
    private final List<String> backendNodes;

    // Atomic integer ensures that multiple threads reading the index don't get the same value
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public RoundRobinLoadBalancer(List<String> backendNodes) {
        this.backendNodes = backendNodes;
    }

    /**
     * Retrieves the next available backend node using a round-robin approach.
     *
     * @return The identifier (IP or hostname) of the target backend node.
     */
    public String getNextNode() {
        // Safely increment the index and wrap around using modulo
        int index = Math.abs(currentIndex.getAndIncrement()) % backendNodes.size();
        return backendNodes.get(index);
    }
}