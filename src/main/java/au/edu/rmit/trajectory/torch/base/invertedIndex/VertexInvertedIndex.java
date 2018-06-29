package au.edu.rmit.trajectory.torch.base.invertedIndex;

import au.edu.rmit.trajectory.torch.base.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VertexInvertedIndex extends InvertedIndex{

    /**
     * invertedIndex a list of trajectories
     * @param trajectories trajectories to be indexed
     */
    public <T extends TrajEntry> void indexAll(List<Trajectory<T>> trajectories){
        for (Trajectory<T> trajectory: trajectories) {

            int pos = 0;
            for (T vertex : trajectory) {
                Map<String, Integer> trajIdPosMap = computeIfAbsent(vertex.getId(), k -> new HashMap<>());
                trajIdPosMap.put(String.valueOf(trajectory.id), ++pos);
            }
        }
    }

}
