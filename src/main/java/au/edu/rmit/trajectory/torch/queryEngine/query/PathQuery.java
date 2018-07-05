package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.PathQueryIndex;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.Mapper;
import au.edu.rmit.trajectory.torch.queryEngine.model.LightEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

class PathQuery implements Query {

    private static final Logger logger = LoggerFactory.getLogger(PathQuery.class);
    private PathQueryIndex index;
    private Mapper mapper;
    private Trajectory<TrajEntry> mapped;
    private TrajectoryResolver resolver;

    PathQuery(PathQueryIndex index, Mapper mapper, TrajectoryResolver resolver){
        this.index = index;
        this.mapper = mapper;
        this.resolver = resolver;
    }

    @Override
    public QueryResult execute(Object _isStrict) {

        if (mapped == null) throw new IllegalStateException("please invoke prepare(List<T> raw) first");
        if (!(_isStrict instanceof Boolean))
            throw new IllegalStateException("parameter passed to PathQuery should be of type SearchWindow, " +
                "which indicates top k results to return");
        boolean isStrictPath = (Boolean) _isStrict;

        List<LightEdge> queryEdges = LightEdge.copy(mapped.edges);
        List<String> trajIds = isStrictPath ? index.findByStrictPath(queryEdges) : index.findByPath(queryEdges);
        return resolver.resolve(trajIds);
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
}
