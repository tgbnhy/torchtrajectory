package au.edu.rmit.bdm.TTorch.base;

import au.edu.rmit.bdm.TTorch.queryEngine.model.SearchWindow;

import java.util.List;

public interface WindowQueryIndex extends Index{
    List<String> findInRange(SearchWindow window);
}
