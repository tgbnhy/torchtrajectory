package au.edu.rmit.trajectory.torch.base;

import au.edu.rmit.trajectory.torch.base.model.TorPoint;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.queryEngine.model.LightEdge;

import java.util.List;

public interface TopKQueryIndex extends Index{
    <T extends TrajEntry> List<String> findTopK(int k, List<T> pointQuery, List<LightEdge> edgeQuery);
    boolean useEdge();
}
