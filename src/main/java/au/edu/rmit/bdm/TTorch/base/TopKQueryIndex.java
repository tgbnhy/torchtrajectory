package au.edu.rmit.bdm.TTorch.base;

import au.edu.rmit.bdm.TTorch.base.model.TrajEntry;
import au.edu.rmit.bdm.TTorch.queryEngine.model.LightEdge;

import java.util.List;

public interface TopKQueryIndex extends Index{
    <T extends TrajEntry> List<String> findTopK(int k, List<T> pointQuery, List<LightEdge> edgeQuery);
    boolean useEdge();
}
