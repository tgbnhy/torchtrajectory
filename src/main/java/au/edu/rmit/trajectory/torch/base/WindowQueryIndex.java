package au.edu.rmit.trajectory.torch.base;

import au.edu.rmit.trajectory.torch.queryEngine.model.SearchWindow;

import java.util.Collection;
import java.util.List;

public interface WindowQueryIndex extends Index{
    List<String> findInRange(SearchWindow window);
}
