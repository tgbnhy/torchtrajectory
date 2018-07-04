package au.edu.rmit.trajectory.torch.base;

import au.edu.rmit.trajectory.torch.queryEngine.model.LightEdge;

import java.util.List;


public interface PathQueryIndex extends Index{

    List<String> findByPath(List<LightEdge> path);
    List<String> findByStrictPath(List<LightEdge> path);
}
