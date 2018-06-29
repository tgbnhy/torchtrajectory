package au.edu.rmit.trajectory.torch.mapMatching.algorithm;

import au.edu.rmit.trajectory.torch.mapMatching.Mapper;
import au.edu.rmit.trajectory.torch.mapMatching.model.TowerVertex;
import au.edu.rmit.trajectory.torch.model.*;
import com.github.davidmoten.geo.GeoHash;
import com.graphhopper.matching.*;
import com.graphhopper.routing.*;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.*;

import java.util.*;

/**
 * HiddenMarkovModel is a map-matching algorithm.
 *
 * It is a wrapper class of Graph-hopper HMM implementation.
 * This implementation is optimized for single trajectory map-matching, and
 * it is highly effective. In terms of matching a large set of trajectories,
 * you may use a different implementation version supported by T-Torch.
 *
 * @see com.graphhopper.matching.MapMatching More details on Hidden Markov Model
 * @see PrecomputedHiddenMarkovModel
 * @see AlgorithmFactory the place to instantiate HiddenMarkovModel algorithm
 */
public class HiddenMarkovModel implements Mapper {

    private com.graphhopper.matching.MapMatching hmm;
    private TorGraph torGraph;

    HiddenMarkovModel(TorGraph torGraph, AlgorithmOptions options){
        hmm = new com.graphhopper.matching.MapMatching(torGraph.getGH(), options);
        this.torGraph = torGraph;
    }

    @Override
    public Trajectory<TowerVertex> match(Trajectory<? extends TrajEntry> in) {

        Trajectory<TowerVertex> mappedTrajectory = new Trajectory<>();
        Graph hopperGraph = torGraph.getGH().getGraphHopperStorage();
        Map<String, TowerVertex> towerVertexes =  torGraph.towerVertexes;

        mappedTrajectory.hasTime = in.hasTime;
        mappedTrajectory.id = in.id;

        List<GPXEntry> queryTrajectory = new ArrayList<>(in.size());
        for (TrajEntry entry: in)
            queryTrajectory.add(new GPXEntry(entry.getLat(), entry.getLng(), 0));
        MatchResult ret = hmm.doWork(queryTrajectory);
        List<EdgeMatch> matches = ret.getEdgeMatches();

        NodeAccess accessor = hopperGraph.getNodeAccess();

        boolean first = true;
        int pre;
        TowerVertex preVertex = null;
        TowerVertex adjVertex;
        int preAdjId = -1;

        for (EdgeMatch match : matches){
            EdgeIteratorState edge = match.getEdgeState();

            pre = edge.getBaseNode();
            int cur = edge.getAdjNode();
            adjVertex = towerVertexes.get(GeoHash.encodeHash(accessor.getLatitude(cur), accessor.getLongitude(cur)));

            if (first){
                preVertex = towerVertexes.get(GeoHash.encodeHash(accessor.getLatitude(pre), accessor.getLongitude(pre)));
                mappedTrajectory.add(preVertex);
                first = false;
            }else{
                assert (preAdjId == pre);
            }

            mappedTrajectory.add(adjVertex);
        }
        return mappedTrajectory;
    }

    @Override
    public <T extends TrajEntry>List<Trajectory<TowerVertex>> batchMatch(List<Trajectory<T>> in) {

        List<Trajectory<TowerVertex>> mappedTrajectories = new ArrayList<>(in.size());

        for (Trajectory<T> raw : in){
            Trajectory<TowerVertex> t = match(raw);
            mappedTrajectories.add(t);
        }

        return mappedTrajectories;
    }
}