package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.PathQueryIndex;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import au.edu.rmit.trajectory.torch.base.persistance.TrajectoryMap;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.Mapper;
import au.edu.rmit.trajectory.torch.mapMatching.model.TorEdge;
import au.edu.rmit.trajectory.torch.mapMatching.model.TowerVertex;
import au.edu.rmit.trajectory.torch.queryEngine.model.LightEdge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class StrictPathQuery implements Query {

    private PathQueryIndex index;
    private Mapper mapper;
    private Trajectory<TrajEntry> mapped;
    private TrajectoryMap trajectoryMap;
    private Map<String, String[]> trajectoryPool;
    private Map<Integer, TowerVertex> idVertexLookup;


    StrictPathQuery(PathQueryIndex index, Mapper mapper, Map<String, String[]> trajectoryPool, Map<Integer, TowerVertex> idVertexLookup){
        this.index = index;
        this.mapper = mapper;
        this.trajectoryPool = trajectoryPool;
        this.idVertexLookup = idVertexLookup;

    }

    @Override
    public List<Trajectory<TrajEntry>> execute(Object Null) {
        if (mapped == null) throw new IllegalStateException("please invoke prepare(List<T> raw) first");

        List<TorEdge> edges = mapped.edges;

        List<LightEdge> l = new ArrayList<>(mapped.edges.size());

        for (TorEdge edge : mapped.edges)
            l.add(LightEdge.copy(edge));

        List<String> trajIds = index.findByStrictPath(l);

        List<Trajectory<TrajEntry>> ret = new ArrayList<>(trajIds.size());
        for (String trajId : trajIds){

            String[] trajectory = trajectoryPool.get(trajId);
            Trajectory<TrajEntry> t = new Trajectory<>();
            t.id = trajId;
            for (int i = 1; i < trajectory.length; i++)
                t.add(idVertexLookup.get(trajectory[i]));

            ret.add(t);
        }
        return ret;
    }

    @Override
    public <T extends TrajEntry> boolean prepare(List<T> raw){

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
