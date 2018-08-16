package au.edu.rmit.bdm.Torch.mapMatching.algorithm;

import au.edu.rmit.bdm.Torch.base.helper.GeoUtil;
import au.edu.rmit.bdm.Torch.base.model.TorEdge;
import au.edu.rmit.bdm.Torch.mapMatching.model.TorVertex;
import au.edu.rmit.bdm.Torch.mapMatching.model.TowerVertex;
import au.edu.rmit.bdm.Torch.base.model.*;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistancePlaneProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is a implementation of HiddenMarkovModel, a map-matching algorithm. For more information, refer to
 * @see HiddenMarkovModel
 * @see com.graphhopper.matching.MapMatching
 *
 * PrecomputedHiddenMarkovModel is optimized for algorithm a batch of trajectories.
 * It precomputes and stores information of nodes within a range from the source node,
 * which could be directly looked up while process trajectories.
 *
 * The drawback of this approach is that the space complexity is relatively high.
 *
 * When to use it: A bit math would help you decide whether to use it or not:
 *
 * given:
 * - sample rate
 *   suppose 30 seconds
 * - average object moving speed
 *   suppose the object is car and the average max speed for a car is 30 m/s (118.8 km/h)
 * out: 900m
 *
 * In above senario, the distance between two sample point could be at most 900m.
 * 900m is a reasonable range for precomputation the shorest path.
 * Trajectories containing adjancent sample points which distance are larger than 900m will be considered illegal and excluded.
 *
 * If after simple math, you found the distance between sample points could be a large more than that( say above 3000),
 * you should consider not using the other HMM implementation.
 */
public class PrecomputedHiddenMarkovModel implements Mapper {

    private static final Object LOCK = new Object();
    private final TorGraph graph;
    private final ShortestPathCache shortestPathCache;
    private final Logger logger = LoggerFactory.getLogger(PrecomputedHiddenMarkovModel.class);
    private DistanceCalc distanceCalc = new DistancePlaneProjection();

    private static final double GPS_ERROR_SIGMA = 50;
    private static final double TRANSITION_PROBABILITY_BETA = 2;

    private static final double INITIAL_SEARCH_RANGE = 50;
    private static final double INCREMENT = 20;

    PrecomputedHiddenMarkovModel(TorGraph graph){
        if (!graph.isBuilt) throw new IllegalStateException("please build the graph first.");
        this.graph = graph;
        this.shortestPathCache = graph.pool;
    }

    @Override
    public <T extends TrajEntry>List<Trajectory<TowerVertex>> batchMatch(List<Trajectory<T>> in) {

        logger.info("start map-matching, total number of raw trajectories for current batch: {}", in.size());

        List<Trajectory<TowerVertex>> mappedTrajectories = new ArrayList<>(in.size());

        final long time = System.currentTimeMillis();

        ExecutorService threadPool = new ThreadPoolExecutor(10, 15, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
        AtomicInteger counter = new AtomicInteger(1);
        int reportForEach = in.size() / 1000 == 0 ? 50 : (int)Math.floor(in.size() / 1000.) ;

        for (Trajectory<T> raw : in) {

            if ( counter.getAndIncrement() % reportForEach == 0) {
                String finishRate =  String.format("%.2f", 100. * counter.intValue() / in.size());
                logger.info("current progress for this batch: {} %", finishRate);
            }

            threadPool.execute(() -> {
                try {
                    Trajectory<TowerVertex> mappedTrajectory = match(raw);
                    if (mappedTrajectory.size() != 0) {
                        synchronized (LOCK) {
                            mappedTrajectories.add(mappedTrajectory);
                        }
                    }
                } catch (Exception unqualifiedTrajectory) {}
            });
        }

        threadPool.shutdown();
        try {
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            logger.error("{}", e);
        }

        logger.info("have done current batch");
        String timeUsed = String.format("%.2f", (System.currentTimeMillis() - time) / 60000.);
        logger.info("time used: {} minutes", timeUsed);
        String mappingRate = String.format("%.2f", 100. * mappedTrajectories.size() / (in.size()));
        logger.info("{}% of trajectories in this batch has been mapped properly.", mappingRate);
        return mappedTrajectories;
    }

    @Override
    public Trajectory<TowerVertex> match(Trajectory<? extends TrajEntry> rawTraj) throws Exception {

        Trajectory<TowerVertex> mappedTrajectory = new Trajectory<>();
        mappedTrajectory.id = rawTraj.id;
        mappedTrajectory.hasTime = rawTraj.hasTime;

        rawTraj = filterEntries(rawTraj);

        // at least, a trajectory should be defined with 2 points.
        if (rawTraj.size() < 2) {
            logger.debug("remote unqualified trajectory id: {}", rawTraj.id);
            throw new IllegalStateException();
        }

        logger.debug("start matching for trajectory id: {}", rawTraj.id);

        // find candidates for each entry of the raw trajectory, compute emission probability
        List<List<Candidate>> timeSteps = new ArrayList<>();
        for (TrajEntry entry : rawTraj) {

            final List<Candidate> timeStep = new LinkedList<>();
            double searchRange = INITIAL_SEARCH_RANGE;

            //if rtree cannot retrieve any entry within the search window, then expand the range until find candidate entry
            while (timeStep.size() == 0) {
                Observable<Entry<TorVertex, Geometry>> results = graph.rTree.search(Geometries.rectangleGeographic(
                        GeoUtil.increaseLng(entry.getLat(), entry.getLng(), -(searchRange)),
                        GeoUtil.increaseLat(entry.getLat(), -(searchRange)),
                        GeoUtil.increaseLng(entry.getLat(), entry.getLat(), (searchRange)),
                        GeoUtil.increaseLat(entry.getLat(), (searchRange)))
                );

                results.forEach(result -> {
                    Candidate candidate = new Candidate(entry, result.value());
                    timeStep.add(candidate);
                });

                searchRange += INCREMENT;
            }

            //normalize emission probability for each candidate
            double sumEmissionProb = 0;
            for (Candidate candidate : timeStep) {
                sumEmissionProb += candidate.emissionProbability;
            }
            for (Candidate candidate : timeStep) {
                candidate.emissionProbability /= sumEmissionProb;
            }
            timeSteps.add(timeStep);
        }


        //vertibi algorithm
        List<Candidate> preCandidates = timeSteps.get(0);
        for (Candidate preCandidate : preCandidates) {
            preCandidate.probability = 0;
            for (Candidate candidate : timeSteps.get(1)) {
                double lineDistance = GeoUtil.distance(preCandidate.candidateVertex, candidate.candidateVertex);
                double shortestPathDistance = shortestPathCache.minDistance(preCandidate.candidateVertex, candidate.candidateVertex);
                double transitionProbability;
                if (shortestPathDistance == 0) transitionProbability = 1.0;
                else if ( shortestPathDistance == Double.MAX_VALUE) transitionProbability =0.;
                else transitionProbability = 1. / TRANSITION_PROBABILITY_BETA * Math.exp(-1 * Math.abs(lineDistance - shortestPathDistance) / TRANSITION_PROBABILITY_BETA);

                //double transitionProbability = lineDistance / shortestPathDistance;
                preCandidate.probability += preCandidate.emissionProbability * transitionProbability;
            }
        }

        normalization(preCandidates);

        for (int i = 1; i < timeSteps.size(); ++i) {
            List<Candidate> curCandidates = timeSteps.get(i);
            double maxProb = Double.MIN_VALUE;
            for (Candidate preCandidate : preCandidates) {
                for (Candidate curCandidate : curCandidates) {
                    double lineDistance = GeoUtil.distance(preCandidate.candidateVertex, curCandidate.candidateVertex);
                    double shortestPathDistance = shortestPathCache.minDistance(preCandidate.candidateVertex, curCandidate.candidateVertex);

                    double transitionProbability;
                    if (shortestPathDistance == 0) transitionProbability = 1.0;
                    else if ( shortestPathDistance == Double.MAX_VALUE) transitionProbability =0.;
                    else transitionProbability = 1. / TRANSITION_PROBABILITY_BETA * Math.exp(-1 * Math.abs(lineDistance - shortestPathDistance) / TRANSITION_PROBABILITY_BETA);
                    // double transitionProbability = lineDistance / shortestPathDistance;

                    double p = preCandidate.probability * transitionProbability * curCandidate.emissionProbability;
                    if (p > curCandidate.probability) {
                        curCandidate.probability = p;
                        curCandidate.preCandidate = preCandidate;
                    }
                    if (p > maxProb)
                        maxProb = p;
                }
            }
            preCandidates = curCandidates;
            if (maxProb < 1e-150)
                normalization(preCandidates);
        }

        //first find the candidate from the last candidate set with the maximum probability,
        //then find the optimal candidate chain
        //finally getList the shortest edges between two candidates
        Candidate maxCandidate = null;
        double maxProbability = Double.MIN_VALUE;
        for (Candidate candidate : timeSteps.get(timeSteps.size() - 1)) {
            if (candidate.probability > maxProbability) {
                maxProbability = candidate.probability;
                maxCandidate = candidate;
            }
        }

        if (maxCandidate == null) {
            logger.debug("cannot find candidate edges, {}", rawTraj.id);
            throw new Exception();
        }

        List<TorVertex> mapMatchedVertices = new LinkedList<>();

        while (maxCandidate != null) {
            mapMatchedVertices.add(0, maxCandidate.candidateVertex);
            maxCandidate = maxCandidate.preCandidate;
        }

        //find all tower vertices of the mapMatchedVertices.
        List<TorEdge> edges = new ArrayList<>();
        List<TowerVertex> vertices = new ArrayList<>();
        TorVertex preVertex = mapMatchedVertices.get(0);
        TorVertex currentVertex;

        for (int i = 1; i < mapMatchedVertices.size()-1; ++i) {
            currentVertex = mapMatchedVertices.get(i);
            if (preVertex == currentVertex) continue;

            List<TowerVertex> curShortestPath = shortestPathCache.shortestPath(preVertex, currentVertex);

            if (curShortestPath != null && curShortestPath.size() > 0) {

                addEdges(edges, curShortestPath);
                addVertices(vertices, curShortestPath);

            }else
            {
                logger.debug("trajectory id: {}, cannot find shortest edges", rawTraj.id);
                throw new IllegalStateException();
            }

            preVertex = currentVertex;
        }

        if (vertices.size() == 0) return mappedTrajectory;
        mappedTrajectory.addAll(vertices);
//
//        Iterator<TorEdge> iterator = edges.iterator();
//        TorEdge pre = iterator.next();
//        while (iterator.hasNext()){
//            TorEdge cur = iterator.next();
//            if (pre == cur) iterator.remove();
//            else pre = cur;
//        }

        mappedTrajectory.edges.addAll(edges);
        return mappedTrajectory;
    }

    /**
     * filter out repeated vertices and construct result at current step.
     */
    private void addVertices(List<TowerVertex> retVertices, List<TowerVertex> curShortestPath) throws Exception {
        if (retVertices.size() != 0) {
            int verticesLastIdx = retVertices.size() - 1;
            if (retVertices.get(verticesLastIdx)==curShortestPath.get(1) &&
                    retVertices.get(verticesLastIdx-1)==curShortestPath.get(0)){
                retVertices.remove(verticesLastIdx);
                retVertices.remove(verticesLastIdx - 1);
            } else if (retVertices.get(verticesLastIdx) == curShortestPath.get(0)){
                retVertices.remove(verticesLastIdx);
            }else{
                System.err.println("cannot connect");
                throw new Exception();
            }
        }
        retVertices.addAll(curShortestPath);
    }


    /**
     * filter out repeated edges and construct result at current step.
     */
    private void addEdges(List<TorEdge> retEdges, List<TowerVertex> curShortestPath) {

        Map<String, TorEdge> allEdges = TorGraph.getInstance().allEdges;
        List<TorEdge> edges = new ArrayList<>(15);

        TorEdge curEdge;
        TorEdge preEdge;
        TowerVertex pre = curShortestPath.get(0);
        TowerVertex cur = curShortestPath.get(1);

        if ((preEdge = allEdges.get(pre.hash+cur.hash)) == null)
            preEdge = allEdges.get(cur.hash + pre.hash);
        if (preEdge == null) {
            System.err.println("given two tower vertices, Torch cannot find the edge.");
            throw new IllegalStateException();
        }

        if (retEdges.size() == 0 || retEdges.get(retEdges.size()-1) != preEdge)
        retEdges.add(preEdge);

        for (int i = 1; i < curShortestPath.size(); i++){
            cur = curShortestPath.get(i);

            if ((curEdge = allEdges.get(pre.hash+cur.hash)) == null)
                curEdge = allEdges.get(cur.hash + pre.hash);

            if (curEdge == null) {
                System.err.println("given two tower vertices, Torch cannot find the edge.");
                throw new IllegalStateException();
            }
            if (curEdge != preEdge) {
                edges.add(curEdge);
            }

            preEdge = curEdge;
            pre = cur;
        }

        retEdges.addAll(edges);
    }

    private void normalization(List<Candidate> candidates) {
        double sumP = 0.0;
        for (Candidate candidate : candidates) {
            sumP += candidate.probability;
        }
        if (sumP != 0.0) {
            for (Candidate candidate : candidates) {
                candidate.probability /= sumP;
            }
        }
    }

    /**
     * only include trajectory node that is either the first one, 
     * the last one, or not within two GPS standard deviation with each other.
     * 
     * @param in the trajectory to be checked
     * @return filtered trajectory
     */
    private Trajectory<TrajEntry> filterEntries(Trajectory<? extends TrajEntry> in) {
        Trajectory<TrajEntry> ret = new Trajectory<>(in.id,in.hasTime);
        TrajEntry prevEntry = null;
        int last = in.size() - 1;

        for (int i = 0; i <= last; i++) {
            TrajEntry point = in.get(i);
            if (i == 0 || i == last
                    || (distanceCalc.calcDist(prevEntry.getLat(), prevEntry.getLng(), point.getLat(), point.getLng()) > 2 * GPS_ERROR_SIGMA
                    || distanceCalc.calcDist(in.get(i + 1).getLat(), in.get(i + 1).getLng(), point.getLat(), point.getLng()) > 2 * GPS_ERROR_SIGMA)) {
                ret.add(point);
                prevEntry = point;
            }
        }
        return ret;
    }

    /**
     * The class contains candidate vertex for the given trajectory node,
     * as well as necessary information for HMM algorithm.
     */
    class Candidate {

        /**
         * standard deviation of GPS device
         * @see com.graphhopper.matching.MapMatching
         */
        final double SIGMA = 50;

        /**
         * query.txt entry: the entry in a query.txt
         */
        TrajEntry entry;

        /**
         * candidate entry of the query.txt entry
         */
        TorVertex candidateVertex;

        double probability;

        /**
         * measurement probability, in this case, it indicates how close between query.txt points and map-matched entry.
         */
        double emissionProbability;

        Candidate preCandidate;

        /**
         * vertex is the query.txt entry, nearestVertex is the candidate vertex of the query.txt vertex
         * Assuming that GPS errors are Gaussian distribution
         *
         * @param entry         one node of the rawTrajectory
         * @param nearestVertex map matched vertex
         */
        Candidate(TrajEntry entry, TorVertex nearestVertex) {
            this.entry = entry;
            this.candidateVertex = nearestVertex;
            double x = GeoUtil.distance(entry, nearestVertex);
            this.emissionProbability = Math.exp(-0.5 * x * x / (SIGMA * SIGMA)) / (Math.sqrt(2 * Math.PI) * SIGMA);
        }

        @Override
        public String toString() {
            return "{" + candidateVertex + ", " + probability + '}';
        }
    }
}
