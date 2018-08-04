package au.edu.rmit.bdm.TTorch.base;

import au.edu.rmit.bdm.TTorch.queryEngine.model.Geometry;

import java.util.List;

public interface WindowQueryIndex extends Index{
    List<String> findInRange(Geometry geometry);
}
