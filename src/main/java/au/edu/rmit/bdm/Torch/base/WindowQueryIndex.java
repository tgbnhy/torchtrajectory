package au.edu.rmit.bdm.Torch.base;

import au.edu.rmit.bdm.Torch.queryEngine.model.Geometry;

import java.util.List;

public interface WindowQueryIndex extends Index{
    List<String> findInRange(Geometry geometry);
}
