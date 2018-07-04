package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.PathQueryIndex;
import au.edu.rmit.trajectory.torch.base.model.Coordinate;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import au.edu.rmit.trajectory.torch.base.persistance.TrajectoryMap;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.Mapper;
import au.edu.rmit.trajectory.torch.mapMatching.model.TorEdge;
import au.edu.rmit.trajectory.torch.mapMatching.model.TowerVertex;
import au.edu.rmit.trajectory.torch.queryEngine.model.LightEdge;
import au.edu.rmit.trajectory.torch.queryEngine.model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PathQuery implements Query {

    private static final Logger logger = LoggerFactory.getLogger(PathQuery.class);
    private PathQueryIndex index;
    private Mapper mapper;
    private Trajectory<TrajEntry> mapped;
    private Map<String, String[]> trajectoryPool;
    Map<Integer, String[]> rawEdgeLookup;

    PathQuery(PathQueryIndex index, Mapper mapper, Map<String, String[]> trajectoryPool, Map<Integer, String[]> rawEdgeLookup){
        this.index = index;
        this.mapper = mapper;
        this.trajectoryPool = trajectoryPool;
        this.rawEdgeLookup = rawEdgeLookup;
    }

    @Override
    public QueryResult execute(Object _isStrict) {

        if (mapped == null) throw new IllegalStateException("please invoke prepare(List<T> raw) first");
        if (!(_isStrict instanceof Boolean))
            throw new IllegalStateException("parameter passed to PathQuery should be of type SearchWindow, " +
                "which indicates top k results to return");

        boolean isStrictPath = (Boolean) _isStrict;
        List<LightEdge> queryEdges = new ArrayList<>(mapped.edges.size());
        for (TorEdge edge : mapped.edges)
            queryEdges.add(LightEdge.copy(edge));


        List<String> trajIds = isStrictPath ? index.findByStrictPath(queryEdges) : index.findByPath(queryEdges);
        List<Trajectory<TrajEntry>> ret = constructResult(trajIds);

        return new QueryResult(ret);
    }


    @Override
    public <T extends TrajEntry> boolean prepare(List<T> raw){

        Trajectory<T> t = new Trajectory<>();
        t.addAll(raw);

        try {
            mapped = (Trajectory<TrajEntry>)(Object)mapper.match(t);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private List<Trajectory<TrajEntry>> constructResult(List<String> trajIds){

        logger.info("matched trajectory id set: {}",trajIds);

        List<Trajectory<TrajEntry>> ret = new ArrayList<>(trajIds.size());
        for (String trajId : trajIds){

            String[] edges = trajectoryPool.get(trajId);
            if (edges == null) {
                logger.debug("cannot find edge id {}, this should not be happened");
                continue;
            }

            Trajectory<TrajEntry> t = new Trajectory<>();
            t.id = trajId;

            for (int i = 1; i < edges.length; i++) {

                String[] tokens = rawEdgeLookup.get(Integer.valueOf(edges[i]));
                String[] lats = tokens[0].split(",");
                String[] lngs = tokens[1].split(",");

                for (int j = 0; j < lats.length; j++) {
                    t.add(new Coordinate(Double.parseDouble(lats[j]),Double.parseDouble(lngs[j])));
                }
            }
            ret.add(t);
        }

        return ret;
    }
}
