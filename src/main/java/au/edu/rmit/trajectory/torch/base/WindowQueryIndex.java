package au.edu.rmit.trajectory.torch.base;

import au.edu.rmit.trajectory.torch.queryEngine.model.SearchWindow;

import java.util.Collection;

public interface WindowQueryIndex extends Index{
    Collection<String> findInRange(SearchWindow window);
}
