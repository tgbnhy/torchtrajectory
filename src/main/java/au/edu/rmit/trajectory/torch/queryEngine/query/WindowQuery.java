package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.WindowQueryIndex;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import au.edu.rmit.trajectory.torch.base.persistance.TrajectoryMap;
import au.edu.rmit.trajectory.torch.mapMatching.model.TowerVertex;
import au.edu.rmit.trajectory.torch.queryEngine.model.SearchWindow;
import com.sun.org.apache.regexp.internal.RE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class WindowQuery implements Query {

    WindowQueryIndex index;
    TrajectoryMap trajectoryMap;
    Map<Integer, TowerVertex> idVertexLookup;
    Map<String, String[]> trajectoryPool;

    WindowQuery(WindowQueryIndex index, Map<Integer, TowerVertex> idVertexLookup, Map<String, String[]> trajectoryPool){
        this.index = index;
        this.idVertexLookup = idVertexLookup;
        this.trajectoryPool = trajectoryPool;
    }

    @Override
    public List<Trajectory<TrajEntry>> execute(Object windowRange) {
        if (!(windowRange instanceof SearchWindow))
            throw new IllegalStateException(
                    "parameter passed to windowQuery should be of type SearchWindow, " +
                    "which indicates the range to search within");

        SearchWindow window = (SearchWindow) windowRange;
        Collection<String> trajIds = index.findInRange(window);

        List<Trajectory<TrajEntry>> ret = new ArrayList<>(trajIds.size());

        for (String trajId : trajIds){

            String[] trajectory = trajectoryPool.get(trajId);
            Trajectory<TrajEntry> t = new Trajectory<>();
            t.id = trajId;
            for (int i = 1; i < trajectory.length; i++)
                t.add(idVertexLookup.get(Integer.valueOf(trajectory[i])));

            ret.add(t);
        }

        return ret;
    }

    @Override
    public <T extends TrajEntry> boolean prepare(List<T> raw) {
        return true;
    }
}
