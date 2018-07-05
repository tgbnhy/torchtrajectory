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

class WindowQuery implements Query {

    WindowQueryIndex index;
    Map<Integer, TowerVertex> idVertexLookup;
    Map<String, String[]> trajectoryPool;
    Map<Integer, String[]> rawEdgeLookup;

    WindowQuery(WindowQueryIndex index, Map<Integer, TowerVertex> idVertexLookup, Map<String, String[]> trajectoryPool, Map<Integer, String[]> rawEdgeLookup){
        this.index = index;
        this.idVertexLookup = idVertexLookup;
        this.trajectoryPool = trajectoryPool;
        this.rawEdgeLookup = rawEdgeLookup;
    }

    @Override
    public QueryResult execute(Object windowRange) {
        if (!(windowRange instanceof SearchWindow))
            throw new IllegalStateException(
                    "parameter passed to windowQuery should be of type SearchWindow, " +
                    "which indicates the range to search within");

        SearchWindow window = (SearchWindow) windowRange;
        Collection<String> trajIds = index.findInRange(window);

        List<Trajectory<TrajEntry>> ret = new ArrayList<>(trajIds.size());

        return QueryResult.construct(trajIds, trajectoryPool, rawEdgeLookup);
    }

    @Override
    public <T extends TrajEntry> boolean prepare(List<T> raw) {
        return true;
    }
}
