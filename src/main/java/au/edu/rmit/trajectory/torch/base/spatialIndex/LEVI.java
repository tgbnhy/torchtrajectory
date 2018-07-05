package au.edu.rmit.trajectory.torch.base.spatialIndex;

import au.edu.rmit.trajectory.torch.base.Torch;
import au.edu.rmit.trajectory.torch.base.WindowQueryIndex;
import au.edu.rmit.trajectory.torch.base.TopKQueryIndex;
import au.edu.rmit.trajectory.torch.base.helper.GeoUtil;
import au.edu.rmit.trajectory.torch.base.invertedIndex.VertexInvertedIndex;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import au.edu.rmit.trajectory.torch.mapMatching.model.TowerVertex;
import au.edu.rmit.trajectory.torch.queryEngine.model.LightEdge;
import au.edu.rmit.trajectory.torch.queryEngine.model.SearchWindow;
import au.edu.rmit.trajectory.torch.queryEngine.similarity.SimilarityFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static au.edu.rmit.trajectory.torch.queryEngine.similarity.SimilarityFunction.*;

/**
 * ﻿LEVI stands for Lightweight edge & vertex vertexInvertedIndex.<p>
 * LEVI basically has two parts: grid vertexInvertedIndex and inverted vertexInvertedIndex on vertex.
 * This two level indexes structure supports range query as well as top k query over vertex.
 */
public class LEVI implements WindowQueryIndex, TopKQueryIndex {

    private static Logger logger = LoggerFactory.getLogger(LEVI.class);
    private VertexInvertedIndex vertexInvertedIndex;
    private VertexGridIndex gridIndex;
    private Map<String, Trajectory<TowerVertex>> trajectoryMapMemory;

    private MeasureType measureType;
    private SimilarityFunction<TrajEntry> similarityFunction = SimilarityFunction.DEFAULT;
    private Map<String, String[]> trajectoryPool;
    private Map<Integer, TowerVertex> idVertexLookup;
    
    public LEVI(VertexInvertedIndex vertexInvertedIndex, VertexGridIndex gridIndex,
                MeasureType measureType, Map<String, String[]> trajectoryPool, Map<Integer, TowerVertex> idVertexLookup){

        this.vertexInvertedIndex = vertexInvertedIndex;
        this.gridIndex = gridIndex;
        this.measureType = measureType;
        this.trajectoryPool = trajectoryPool;
        this.idVertexLookup = idVertexLookup;
    }

    @Override
    public boolean build(String Null) {
        
        if (!vertexInvertedIndex.loaded) vertexInvertedIndex.build(Torch.URI.VERTEX_INVERTED_INDEX);
        if (!gridIndex.loaded) gridIndex.build(Torch.URI.GRID_INDEX);
        
        return vertexInvertedIndex.loaded && gridIndex.loaded;
    }
    
    @Override
    public boolean useEdge() {
        return false;
    }


    //todo
    @Override
    public Collection<String> findInRange(SearchWindow window) {

        Collection<Integer> points = gridIndex.pointsInWindow(window);
        Set<String> ret = new HashSet<>();

        for (Integer pointId : points)
            ret.addAll(vertexInvertedIndex.getKeys(pointId));

        return ret;
    }

    @Override
    public <T extends TrajEntry> List<String> findTopK(int k, List<T> pointQuery,  List<LightEdge> edgeQuery){

        double bestKthSoFar = Double.MAX_VALUE, unseenBound = 0;
        int round = 0;
        //trajectory hash and score
        PriorityQueue<Pair> topKHeap = new PriorityQueue<>((p1, p2) -> Double.compare(p2.score, p1.score));
        Set<String> visitTrajectorySet = new HashSet<>();

        Set<Integer> visitedGrid = new HashSet<>();

        int check = 0;
        while (check == 0) {

            unseenBound = 0;
            //each query point match with the nearest point of a trajectory,
            // and the lower bound is the maximun distance between a query and existing points of a trajectory
            Map<String, Double> trajBound = new HashMap<>();
            Map<String, Map<TrajEntry, Double>> trajLowerBoundForDTW = new HashMap<>();

            //findMoreVertices candiates incrementally and calculate their lower bound
            for (T queryPoint : pointQuery) {

                int tileId = gridIndex.calculateTileID(queryPoint);
                if (!visitedGrid.contains(tileId))
                    findMoreVertices(queryPoint, round, measureType, trajBound, trajLowerBoundForDTW);

                visitedGrid.add(tileId);

                double curRadius = gridIndex.findBound(queryPoint, round);
                switch (measureType) {
                    case DTW:
                        unseenBound += curRadius;
                        break;
                    case Hausdorff:
                        if (unseenBound < curRadius) unseenBound = curRadius;
                        break;
                    case Frechet:
                        if (unseenBound < curRadius) unseenBound = curRadius;
                        break;
                }
            }
            visitedGrid.clear();

            //rank trajectories by their lower bound
            List<Map.Entry<String, Double>> tempList = new ArrayList<>(trajBound.entrySet());

            PriorityQueue<Map.Entry<String, Double>> rankedTrajectories = new PriorityQueue<>(Map.Entry.comparingByValue());
            for (Map.Entry<String, Double> entry : tempList) {
                if (!visitTrajectorySet.contains(entry.getKey()))
                    rankedTrajectories.add(entry);
            }
            //mark visited trajectories
            visitTrajectorySet.addAll(trajBound.keySet());
            logger.debug("total number of candidate trajectories in {}th round: {}", round, rankedTrajectories.size());


            //calculate exact distance for each candidate
            int j = 0;
            while (!rankedTrajectories.isEmpty()) {
                if (++j % 1000 == 0 && (measureType == MeasureType.Frechet || measureType == MeasureType.Hausdorff))
                    logger.info("has processed trajectories: {}, current kth score: {}, bound: {}", j, bestKthSoFar, unseenBound );
                Map.Entry<String, Double> entry1 = rankedTrajectories.poll();
                String trajID = entry1.getKey();
                String[] trajectory = trajectoryPool.get(trajID);
                Trajectory<TrajEntry> t = new Trajectory<>();
                t.id = trajID;
                for (int i = 1; i < trajectory.length; i++) {
                    t.add(idVertexLookup.get(Integer.valueOf(trajectory[i])));
                }

                double score = 0;
                switch (measureType) {
                    case DTW:
                        score = similarityFunction.fastDynamicTimeWarping(t, (List<TrajEntry>)pointQuery, 10, bestKthSoFar);
                        break;
                    case Hausdorff:
                        score = similarityFunction.Hausdorff(t, (List<TrajEntry>)pointQuery);
                        break;
                    case Frechet:
                        score = similarityFunction.Frechet(t, (List<TrajEntry>)pointQuery);
                        break;
                }

                Pair pair = new Pair(trajID, score);
                topKHeap.add(pair);

                if (topKHeap.size() > k) {
                    topKHeap.poll();
                    bestKthSoFar = topKHeap.peek().score;
                    if (rankedTrajectories.size() == 0) break;
                    if (bestKthSoFar < unseenBound){
                        check = 1;
                        break;
                    }
                }
            }

            bestKthSoFar = topKHeap.peek().score;
            logger.info("round: {}, kth score: {}, unseen bound: {}", round, bestKthSoFar, unseenBound);

            if (round == 5) {
                logger.error("round = 5");
                break;
            }
            ++round;
        }


        List<String> resIDList = new ArrayList<>();
        while (!topKHeap.isEmpty()) {
            resIDList.add(topKHeap.poll().trajectoryID);
        }

        return resIDList;
    }



    private void findMoreVertices(TrajEntry queryVertex, int round, MeasureType measureType, Map<String, Double> existingTrajBound, Map<String, Map<TrajEntry, Double>> existingTrajIDLowerBoundForDTW) {

        //findMoreVertices the nearest pair between a trajectory and query queryVertex
        //trajectory hash, queryVertex hash vertices
        Set<Integer> vertices = new HashSet<>();
        gridIndex.incrementallyFind(queryVertex, round, vertices);
        for (Integer vertexId : vertices){
            Double dist = GeoUtil.distance(idVertexLookup.get(vertexId), queryVertex);
            logger.info("dist between queryVertex and vertices: {}", dist);
            List<String> l= vertexInvertedIndex.getKeys(vertexId);
            for (String trajId : l){

                if (measureType == MeasureType.DTW) {
                    Map<TrajEntry, Double> map = existingTrajIDLowerBoundForDTW.get(trajId);
                    if (map != null) {
                        if (!map.containsKey(queryVertex))
                            map.put(queryVertex, dist);
                        else if (map.get(queryVertex) > dist)
                            map.put(queryVertex, dist);
                    } else {
                        map = existingTrajIDLowerBoundForDTW.computeIfAbsent(trajId, key -> new HashMap<>());
                        map.put(queryVertex, dist);
                    }

                    double temp = 0.;
                    for (Double d : map.values())
                        temp += d;

                    existingTrajBound.put(trajId, temp);
                } else{
                    if (existingTrajBound.get(trajId) == null){
                        existingTrajBound.put(trajId, dist);
                    }else {
                        double pre = existingTrajBound.get(trajId);
                        existingTrajBound.put(trajId, Math.min(dist, pre));
                    }
                }
            }
        }
    }

    class Pair {
        final String trajectoryID;
        final double score;

        Pair(String trajectoryID, double score) {
            this.trajectoryID = trajectoryID;
            this.score = score;
        }
    }
}
