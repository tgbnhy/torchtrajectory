package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.WindowQueryIndex;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import au.edu.rmit.trajectory.torch.base.persistance.TrajectoryMap;
import au.edu.rmit.trajectory.torch.mapMatching.model.TowerVertex;
import au.edu.rmit.trajectory.torch.queryEngine.model.SearchWindow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class WindowQuery extends QueryImpl {

    private WindowQueryIndex index;

    WindowQuery(WindowQueryIndex index, TrajectoryResolver resolver){
        super(null, resolver);
        this.index = index;
    }

    @Override
    public QueryResult execute(Object windowRange) {
        if (!(windowRange instanceof SearchWindow))
            throw new IllegalStateException(
                    "parameter passed to windowQuery should be of type SearchWindow, " +
                    "which indicates the range to search within");

        SearchWindow window = (SearchWindow) windowRange;
        List<String> trajIds = index.findInRange(window);
        return resolver.resolve(trajIds, null, null);
    }

    @Override
    public boolean prepare(List<? extends TrajEntry> raw) {
        return true;
    }
}
