package au.edu.rmit.bdm.Torch.mapMatching.algorithm;

import au.edu.rmit.bdm.Torch.base.helper.GeoUtil;
import au.edu.rmit.bdm.Torch.mapMatching.model.TowerVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Dijkstra algorithm computes the shortest path between nodes with twitches.
 * It calculate the shortest path from a start candidateVertex s to its adjacent points (tower points) limited by a max distance
 * the result will be store in ShortestPathCache which will be used for PrecomputedHiddenMarkovModel
 *
 * @see ShortestPathCache
 * @see PrecomputedHiddenMarkovModel
 */
public class TorDijkstra {

    private static Logger logger = LoggerFactory.getLogger(TorDijkstra.class);

    // search radius around src entry
    // metric: meter
    private int computationRange;
    private ShortestPathCache pool;

    TorDijkstra(TorGraph graph) {
        computationRange = graph.preComputationRange;
        pool = graph.pool;
    }

    /**
     * from source entry, calculate min path to other tower points that are within maxDistance around src
     * relevant information are recorded and modeled into ShortestPathCache.ShortestPathEntry
     *
     * @param src the tower entry as the source
     *
     * @see ShortestPathCache.ShortestPathEntry
     */
     void run(TowerVertex src) {

        Map<String, Double> dist = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Map<String, List<TowerVertex>> routing = new HashMap<>();
        Map<String, FibonacciHeap.Entry<TowerVertex>> fibEntries = new HashMap<>();
        FibonacciHeap<TowerVertex> priorityQ = new FibonacciHeap<>();

        Iterator<TowerVertex> itr = src.adjIterator();
        while (itr.hasNext()) {
            TowerVertex adjPoint = itr.next();
            FibonacciHeap.Entry<TowerVertex> entry = priorityQ.enqueue(adjPoint, GeoUtil.distance(src, adjPoint));
            fibEntries.put(adjPoint.hash, entry);
            List<TowerVertex> route = new LinkedList<>();
            route.add(src);
            route.add(adjPoint);
            routing.put(adjPoint.hash, route);
        }

        dist.put(src.hash, 0.0);
        List<TowerVertex> toSelf = Collections.singletonList(src);
        routing.put(src.hash, toSelf);

        while (priorityQ.size() > 0) {
            FibonacciHeap.Entry<TowerVertex> entry = priorityQ.dequeueMin();
            if (visited.contains(entry.getValue().hash))
                continue;
            visited.add(entry.getValue().hash);
            dist.put(entry.getValue().hash, entry.getPriority());
            if (entry.getPriority() >= computationRange) break;

            itr = entry.getValue().adjIterator();
            while (itr.hasNext()) {
                TowerVertex adjPoint = itr.next();
                Double oldValue = dist.get(adjPoint.hash);
                double newValue = entry.getPriority() + entry.getValue().getAdjDistance(adjPoint);
                if (oldValue != null) {
                    if (oldValue > newValue) {
                        priorityQ.decreaseKey(fibEntries.get(adjPoint.hash), newValue);
                        dist.put(adjPoint.hash, newValue);
                        List<TowerVertex> route = new ArrayList<>(routing.get(entry.getValue().hash));
                        route.add(adjPoint);
                        routing.put(adjPoint.hash, route);
                    }
                } else {
                    dist.put(adjPoint.hash, newValue);
                    fibEntries.put(adjPoint.hash, priorityQ.enqueue(adjPoint, newValue));
                    List<TowerVertex> route = new ArrayList<>(routing.get(entry.getValue().hash));
                    route.add(adjPoint);
                    routing.put(adjPoint.hash, route);
                }
            }
        }
        pool.addEntry(src, dist, routing);
    }
}