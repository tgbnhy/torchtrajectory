package au.edu.rmit.bdm.Torch.base.invertedIndex;

import au.edu.rmit.bdm.Torch.base.FileSetting;
import au.edu.rmit.bdm.Torch.base.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VertexInvertedIndex extends InvertedIndex{

    public VertexInvertedIndex(FileSetting setting){
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
        int pos = 0;
        for (T vertex : trajectory) {
            Map<String, Integer> trajIdPosMap = index.computeIfAbsent(vertex.getId(), k -> new HashMap<>());
            trajIdPosMap.put(String.valueOf(trajectory.id), ++pos);
        }
    }

}
