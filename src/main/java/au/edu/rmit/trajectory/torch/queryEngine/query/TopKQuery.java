package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.TopKQueryIndex;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import au.edu.rmit.trajectory.torch.base.persistance.TrajectoryMap;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.Mapper;
import au.edu.rmit.trajectory.torch.mapMatching.model.TowerVertex;
import au.edu.rmit.trajectory.torch.queryEngine.model.LightEdge;
import au.edu.rmit.trajectory.torch.queryEngine.similarity.SimilarityFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class TopKQuery implements Query{

    private static final Logger logger = LoggerFactory.getLogger(TopKQuery.class);
    private boolean useEdge;
    private TopKQueryIndex index;
    private Trajectory<TrajEntry> mapped;
    private Mapper mapper;
    private Map<Integer, TowerVertex> idVertexLookup;
    private TrajectoryResolver resolver;
    private SimilarityFunction simFunc;


    TopKQuery(TopKQueryIndex index, Mapper mapper, TrajectoryResolver resolver){
        this.index = index;
        this.mapper = mapper;
        this.resolver = resolver;
        useEdge = true;
    }

    TopKQuery(TopKQueryIndex index, Mapper mapper, TrajectoryResolver resolver , SimilarityFunction simFunc){
        this.index = index;
        this.mapper = mapper;
        this.simFunc = simFunc;
        this.resolver = resolver;
        useEdge = false;
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

    @Override
    public QueryResult execute(Object K) {

        if (!(K instanceof Integer))
            throw new IllegalStateException(
                    "parameter passed to windowQuery should be of type SearchWindow, " +
                            "which indicates top k results to return");

        if (useEdge)
            return topKusingEdge((int)K);
        else
            return topkusingVertex((int)K);
    }

    private QueryResult topKusingEdge(int k) {

        List<String> trajIds = index.findTopK(k, null, LightEdge.copy(mapped.edges));

        logger.info("total qualified trajectories: {}", trajIds.size());
        logger.info("top {} trajectory id set: {}",trajIds.size(),trajIds);
        return resolver.resolve(trajIds);
    }


    private QueryResult topkusingVertex(int k) {

        List<String> trajIds = index.findTopK(k, mapped, null);
        logger.info("top {} trajectory id set: {}",trajIds.size(),trajIds);
        return resolver.resolve(trajIds);
    }
}
