package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.TopKQueryIndex;
import au.edu.rmit.trajectory.torch.base.Torch;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import au.edu.rmit.trajectory.torch.base.persistance.TrajectoryMap;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.Mapper;
import au.edu.rmit.trajectory.torch.mapMatching.model.TorEdge;
import au.edu.rmit.trajectory.torch.mapMatching.model.TowerVertex;
import au.edu.rmit.trajectory.torch.queryEngine.model.LightEdge;
import au.edu.rmit.trajectory.torch.queryEngine.model.QueryResult;
import au.edu.rmit.trajectory.torch.queryEngine.model.SearchWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class TopKQuery implements Query{

    private TopKQueryIndex index;
    private TrajectoryMap trajectoryMap;
    private Trajectory<TrajEntry> mapped;
    private Mapper mapper;
    Map<Integer, TowerVertex> idVertexLookup;
    Map<String, String[]> trajectoryPool;


    TopKQuery(TopKQueryIndex index, Mapper mapper, Map<Integer, TowerVertex> idVertexLookup, Map<String, String[]> trajectoryPool){
        this.index = index;
        this.mapper = mapper;
        this.idVertexLookup = idVertexLookup;
        this.trajectoryPool = trajectoryPool;
    }

    @Override
    public QueryResult execute(Object K) {
        if (!(K instanceof Integer))
            throw new IllegalStateException(
                    "parameter passed to windowQuery should be of type SearchWindow, " +
                    "which indicates top k results to return");

        int k = (int) K;

        List<String> trajIds;

        if (index.useEdge()) {
            List<LightEdge> l = new ArrayList<>(mapped.edges.size());
            for (TorEdge edge : mapped.edges)
                l.add(LightEdge.copy(edge));
            trajIds = index.findTopK(k, null, l);

        }else{
            trajIds = index.findTopK(k, mapped, null);
        }

        List<Trajectory<TrajEntry>> ret = new ArrayList<>(trajIds.size());
        for (String trajId : trajIds){

            String[] trajectory = trajectoryPool.get(trajId);
            Trajectory<TrajEntry> t = new Trajectory<>();
            t.id = trajId;
            for (int i = 1; i < trajectory.length; i++)
                t.add(idVertexLookup.get(Integer.valueOf(trajectory[i])));

            ret.add(t);
        }

        return new QueryResult(ret);
    }

    @Override
    public <T extends TrajEntry> boolean prepare(List<T> raw) {
        Trajectory<T> t = new Trajectory<>();
        t.addAll(raw);

        try {
            mapped = (Trajectory<TrajEntry>)(Object)mapper.match(t);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
