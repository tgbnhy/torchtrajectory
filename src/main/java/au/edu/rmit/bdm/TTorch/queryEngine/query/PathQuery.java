package au.edu.rmit.bdm.TTorch.queryEngine.query;

import au.edu.rmit.bdm.TTorch.base.PathQueryIndex;
import au.edu.rmit.bdm.TTorch.base.Torch;
import au.edu.rmit.bdm.TTorch.mapMatching.algorithm.Mapper;
import au.edu.rmit.bdm.TTorch.queryEngine.model.LightEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

class PathQuery extends QueryImpl {

    private static final Logger logger = LoggerFactory.getLogger(PathQuery.class);
    private PathQueryIndex index;

    PathQuery(PathQueryIndex index, Mapper mapper, TrajectoryResolver resolver){
        super(mapper, resolver);
        this.index = index;
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
        return resolver.resolve(Torch.QueryType.PathQ, trajIds, raw, mapped);
    }
}
