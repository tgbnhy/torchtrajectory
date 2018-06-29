package au.edu.rmit.trajectory.torch.index;

import au.edu.rmit.trajectory.torch.mapMatching.model.TorEdge;
import au.edu.rmit.trajectory.torch.model.TrajEntry;
import au.edu.rmit.trajectory.torch.model.Trajectory;

import java.util.*;

/**
 * The class models inverted list for edge or vertex.
 *
 * key: an edge id or a vertex id
 * value: pairs( trajectory id -- position of that key in the trajectory)
 *
 */
public class EdgeInvertedIndex extends InvertedIndex{

    /**
     * index a list of trajectories
     * @param trajectories trajectories to be indexed
     */
    public <T extends TrajEntry> void indexAll(List<Trajectory<T>> trajectories){
        for (Trajectory<T> trajectory: trajectories) {

            List<TorEdge> edges = trajectory.edges;
            int pos = 0;

            for (TorEdge edge : edges) {
                Map<String, Integer> trajIdPosMap = computeIfAbsent(edge.id, k -> new HashMap<>());
                trajIdPosMap.put(String.valueOf(trajectory.id), ++pos);
            }
        }
    }


}
