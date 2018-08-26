package au.edu.rmit.bdm.Torch.base.invertedIndex;

import au.edu.rmit.bdm.Torch.base.FileSetting;
import au.edu.rmit.bdm.Torch.base.PathQueryIndex;
import au.edu.rmit.bdm.Torch.base.TopKQueryIndex;
import au.edu.rmit.bdm.Torch.base.model.TorEdge;
import au.edu.rmit.bdm.Torch.base.model.TrajEntry;
import au.edu.rmit.bdm.Torch.base.model.Trajectory;
import au.edu.rmit.bdm.Torch.queryEngine.model.LightEdge;
import au.edu.rmit.bdm.Torch.queryEngine.query.TrajectoryResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The class models inverted list for edge or vertex.
 *
 * key: an edge id or a vertex id
 * value: pairs( trajectory id -- position of that key in the trajectory)
 *
 */
public class EdgeInvertedIndex extends InvertedIndex implements PathQueryIndex, TopKQueryIndex {

    private static  final Logger logger = LoggerFactory.getLogger(EdgeInvertedIndex.class);

    public EdgeInvertedIndex(FileSetting setting){
        super(setting);
    }
    /**
     * invertedIndex a list of trajectories
     * @param trajectories trajectories to be indexed
     */
    public <T extends TrajEntry> void indexAll(List<Trajectory<T>> trajectories){

        for (Trajectory<T> trajectory: trajectories)
            index(trajectory);
    }

    @Override
    public <T extends TrajEntry> void index(Trajectory<T> trajectory) {
        List<TorEdge> edges = trajectory.edges;
        int pos = 0;

        for (TorEdge edge : edges) {
            Map<String, Integer> trajIdPosMap = index.computeIfAbsent(edge.id, k -> new HashMap<>());
            trajIdPosMap.put(trajectory.id, ++pos);
        }
    }

    @Override
    public List<String> findByPath(List<LightEdge> path) {

        Set<String> ret = new HashSet<>();
        for (LightEdge edge : path) {
            List<String> l = getKeys(edge.id);
            if (l != null) {
                ret.addAll(l);
            }
        }
        return new ArrayList<>(ret);
    }

    @Override
    public List<String> findByStrictPath(List<LightEdge> edges) {

        logger.info("start find trajectories on the strict path");

        //key is trajectory id, value is number of different query.txt edges
        Map<String, Integer> map = new HashMap<>();

        for (LightEdge edge : edges) {
            List<String> l = getKeys(edge.id);
            if (l != null) {
                for (String trajId : l) {
                    map.merge(trajId, 1, (a, b) -> a + b);
                }
            }
        }

        List<String> ret = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() >= edges.size()) {
                ret.add(entry.getKey());
            }
        }

        return new ArrayList<>(ret);
    }

    /**
     * LEVI( Longest Overlapping Road Segments) algorithm.
     * Find top K trajectories that has the max score( similarity) against query.txt trajectory( represented by a list of edges).
     * Used in efficiency test.
     *
     * @param edgeQuery a list of edges representing a query.txt
     * @param k number of results returned
     *
     * @return A list of results of type Integer meaning ids of trajectory.
     */
    @Override
    public <T extends TrajEntry> List<String> findTopK(int k, List<T> pointQuery, List<LightEdge> edgeQuery, TrajectoryResolver resolver){

        // 1. compute upper bound for each candidate trajectory

        // key for trajectory id, value for its upper bound
        Map<String, Double> candidateUpperBound = new HashMap<>();
        // key for trajectory id, value for the list of edges overlapped with the query.txt edge.
        Map<String, List<LightEdge>> candidates = new HashMap<>();

        for (LightEdge queryEdge : edgeQuery) {

            List<Pair> trajPosMap = getPairs(queryEdge.id);
            if (trajPosMap == null) continue;

            //key for trajectory hash, value for position
            for (Pair pair : trajPosMap) {

                String trajId = String.valueOf(pair.trajid);
                //calculate upper bound for each trajectory
                candidateUpperBound.merge(trajId, queryEdge.length, (a, b) -> b + a);
                //re-construct every trajectory, need to reorder in the next steps
                List<LightEdge> candidateEdges = candidates.computeIfAbsent(trajId, key -> new ArrayList<>());
                candidateEdges.add(new LightEdge(queryEdge.id, queryEdge.length, pair.pos));
            }
        }

        // 2. prune candidates.
        // compute exact score from highest upper bound trajectory to lowest,
        // If the current k ranked trajectory exact bound is higher than current candidate trajectory upper bound,
        // terminate.

        List<String> retList = new ArrayList<>();

        // key for trajectory id, value for its upper bound
        PriorityQueue<Map.Entry<String, Double>> upperBoundRank = new PriorityQueue<>((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        upperBoundRank.addAll(candidateUpperBound.entrySet());

        // key for trajectory id, value for its exact score
        PriorityQueue<Map.Entry<String, Double>> topKHeap = new PriorityQueue<>(Map.Entry.comparingByValue());
        double bestKth = -Integer.MAX_VALUE;

        /* an early termination heuristics for LCSS.
           a list containing the sum of rest edges length in total
           example: if the query.txt contains 3 edges, which are 3 meters, 1 meters and 2 meters respectively in length.
           Then the restDistance contains [3, 2, 0], which means that if it getList to dataStructure 0, then the rest is 3.
           If it getList to dataStructure 1, then the rest is 2. And if it getList to dataStructure 3, then the rest is 0. */
        double[] restDistance = new double[edgeQuery.size()];
        for (int i = edgeQuery.size() - 2; i >= 0 && i + 1 < edgeQuery.size(); --i) {
            restDistance[i] = restDistance[i + 1] + edgeQuery.get(i + 1).length;
        }

        while (!upperBoundRank.isEmpty()) {
            Map.Entry<String, Double> entry = upperBoundRank.poll();           //key-trajId, value-upper bound
            if (!resolver.meetTimeConstrain(entry.getKey())) continue;

            if (topKHeap.size() >= k &&
                    bestKth > entry.getValue()) break; //early termination

            List<LightEdge> candidate = candidates.get(entry.getKey());
            candidate.sort(Comparator.comparingInt(e -> e.position));

            double exactValue = lors(edgeQuery, candidate, Integer.MAX_VALUE, restDistance, bestKth);

            entry.setValue(exactValue);
            topKHeap.add(entry);
            if (topKHeap.size() > k) topKHeap.poll();

            bestKth = topKHeap.peek().getValue();
        }

        while (topKHeap.size() > 0) {
            retList.add(topKHeap.poll().getKey());
        }

        return retList;
    }

    /**
     * LEVI stands for longest overlapped road segments.<p>
     * It is LCSS algorithm twitched for computing similarity between two sequences over edges.
     *
     * @param qEdges          edges representing query.txt trajectory
     * @param cEdges          edges representing sub candidate trajectory
     * @param theta           For instance, theta is 5. If the 3rd edge in query.txt trajectory matches the 11th edge in candidate trajectory,
     *                        it won't count because the position between than is larger than theta.
     * @param restDistance     a list containing the sum of rest edges length in total
     *                         example:
     *                         if the query.txt contains 3 edges, which are 3 meters, 1 meters and 2 meters respectively in length.
     *                         Then the restDistance contains [3, 2, 0], which means that if it getList to the first one, then the rest is 3( 2 + 1).
     *                         If it getList to the second, then the rest is 2. And if it getList to dataStructure 3, then the rest is 0.
     * @param bestKthSofar     score for the min score element in the heap.
     * @return similarity score computed using LEVI sim measure.
     */

    private double lors(List<? extends LightEdge> qEdges, List<? extends LightEdge> cEdges, int theta, double[] restDistance, double bestKthSofar) {

        if (qEdges == null || cEdges == null || qEdges.size() == 0 || cEdges.size() == 0)
            return 0;

        double[][] dpInts = new double[qEdges.size()][cEdges.size()];

        if (qEdges.get(0).id == cEdges.get(0).id) dpInts[0][0] = qEdges.get(0).length;

        for (int i = 1; i < qEdges.size(); ++i) {
            if (qEdges.get(i).id == cEdges.get(0).id)
                dpInts[i][0] = qEdges.get(i).length;
            else dpInts[i][0] = dpInts[i - 1][0];
        }

        for (int j = 1; j < cEdges.size(); ++j) {
            if (cEdges.get(j).id == qEdges.get(0).id)
                dpInts[0][j] = cEdges.get(j).length;
            else dpInts[0][j] = dpInts[0][j - 1];
        }

        for (int i = 1; i < qEdges.size(); ++i) {
            for (int j = 1; j < cEdges.size(); ++j) {
                if (Math.abs(i - j) <= theta) {
                    if (qEdges.get(i).id == cEdges.get(j).id) {
                        dpInts[i][j] = qEdges.get(i).length + dpInts[i - 1][j - 1];
                    } else {
                        dpInts[i][j] = Math.max(dpInts[i - 1][j], dpInts[i][j - 1]);
                    }
                    //todo
                    if (restDistance != null && dpInts[i][j] + restDistance[i] < bestKthSofar)
                        return dpInts[i][j] + restDistance[i];
                }
            }
        }

        return dpInts[qEdges.size() - 1][cEdges.size() - 1];
    }

    @Override
    public boolean useEdge() {
        return true;
    }
}
