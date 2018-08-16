package au.edu.rmit.bdm.Torch.base;

import au.edu.rmit.bdm.Torch.base.model.TrajEntry;
import au.edu.rmit.bdm.Torch.queryEngine.model.LightEdge;
import au.edu.rmit.bdm.Torch.queryEngine.query.TrajectoryResolver;

import java.util.List;

public interface TopKQueryIndex extends Index{
    <T extends TrajEntry> List<String> findTopK(int k, List<T> pointQuery, List<LightEdge> edgeQuery, TrajectoryResolver resolver);
    boolean useEdge();
}
