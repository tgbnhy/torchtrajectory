package au.edu.rmit.trajectory.torch.queryEngine.similarity;

import au.edu.rmit.trajectory.torch.base.invertedIndex.EdgeInvertedIndex;
import au.edu.rmit.trajectory.torch.queryEngine.model.LightEdge;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LORS {

    EdgeInvertedIndex index;

    public LORS(EdgeInvertedIndex index){
        if (index == null)
            throw new IllegalStateException("cannot perform similarity search without EdgeInvertedIndex");
        this.index = index;
    }

    /**
     * LORS( Longest Overlapping Road Segments) algorithm.
     * Find top K trajectories that has the max score( similarity) against query trajectory( represented by a list of edges).
     * Used in efficiency test.
     *
     * @param queryTrajectory a list of edges representing a query
     * @param k number of results returned
     *
     * @return A list of results of type Integer meaning ids of trajectory.
     */
    public List<String> topK(List<LightEdge> queryTrajectory, int k) {

        // 1. compute upperbound for each candidate trajectory

        // key for trajectory id, value for its upper bound
        Map<String, Double> candidateUpperBound = new HashMap<>();
        // key for trajectory id, value for the list of edges overlapped with the query edge.
        Map<String, List<LightEdge>> candidates = new HashMap<>();

        for (LightEdge queryEdge : queryTrajectory) {

            Map<String, Integer> trajPosMap = index.get(queryEdge.id);
            if (trajPosMap == null) continue;

            //key for trajectory hash, value for position
            for (Map.Entry<String, Integer> entry : trajPosMap.entrySet()) {

                String trajId = entry.getKey();
                //calculate upper bound for each trajectory
                candidateUpperBound.merge(trajId, queryEdge.length, (a, b) -> b + a);
                //re-construct every trajectory, need to reorder in the next steps
                List<LightEdge> candidateEdges = candidates.computeIfAbsent(trajId, key -> new ArrayList<>());
                candidateEdges.add(new LightEdge(queryEdge.id, queryEdge.length, entry.getValue()));
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
           example: if the query contains 3 edges, which are 3 meters, 1 meters and 2 meters respectively in length.
           Then the restDistance contains [3, 2, 0], which means that if it get to dataStructure 0, then the rest is 3.
           If it get to dataStructure 1, then the rest is 2. And if it get to dataStructure 3, then the rest is 0. */
        double[] restDistance = new double[queryTrajectory.size()];
        for (int i = queryTrajectory.size() - 2; i >= 0 && i + 1 < queryTrajectory.size(); --i) {
            restDistance[i] = restDistance[i + 1] + queryTrajectory.get(i + 1).length;
        }

        while (!upperBoundRank.isEmpty()) {
            Map.Entry<String, Double> entry = upperBoundRank.poll();           //key-trajId, value-upper bound

            if (topKHeap.size() >= k &&
                    bestKth > entry.getValue()) break; //early termination

            List<LightEdge> candidate = candidates.get(entry.getKey());
            candidate.sort(Comparator.comparingInt(e -> e.position));

            double exactValue = lcssAlg(queryTrajectory, candidate, Integer.MAX_VALUE, restDistance, bestKth);

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
     * LCSS algorithm twitched for LORS exact similarity score computation.
     * 
     * @param qEdges          edges representing query trajectory
     * @param cEdges          edges representing sub candidate trajectory
     * @param theta           For instance, theta is 5. If the 3rd edge in query trajectory matches the 11th edge in candidate trajectory,
     *                        it won't count because the position between than is larger than theta.
     * @param restDistance     a list containing the sum of rest edges length in total
     *                         example:
     *                         if the query contains 3 edges, which are 3 meters, 1 meters and 2 meters respectively in length.
     *                         Then the restDistance contains [3, 2, 0], which means that if it get to the first one, then the rest is 3( 2 + 1).
     *                         If it get to the second, then the rest is 2. And if it get to dataStructure 3, then the rest is 0.
     * @param bestKthSofar     score for the min score element in the heap.
     * @return similarity score computed using LORS sim measure.
     */
    
    private double lcssAlg(List<? extends LightEdge> qEdges, List<? extends LightEdge> cEdges, int theta, double[] restDistance, double bestKthSofar) {

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

    public List<String> pathQuery(List<LightEdge> path) {

        Set<String> ret = new HashSet<>();
        for (LightEdge edge : path) {
            Map<String, Integer> trajIdPosMap = index.get(edge.id);

            if (trajIdPosMap != null) {
                ret.addAll(trajIdPosMap.keySet());
            }
        }
        return new ArrayList<>(ret);
    }

    public List<String> strictPathQuery(List<LightEdge> edges) {

        //key is trajectory id, value is its position list
        Map<String, Integer> map = new HashMap<>();

        for (LightEdge edge : edges) {
            Map<String, Integer> trajIdPosMap = index.get(edge.id);
            if (trajIdPosMap != null) {
                for (String trajId : trajIdPosMap.keySet())
                    map.merge(trajId, 1, (v1, v2) -> (v1 + v2));
            }
        }

        List<String> ret = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() == edges.size())
                ret.add(entry.getKey());
        }

        return new ArrayList<>(ret);
    }
}
