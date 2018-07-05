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
    private TrajectoryMap trajectoryMap;
    private Trajectory<TrajEntry> mapped;
    private Mapper mapper;
    private Map<Integer, TowerVertex> idVertexLookup;
    private Map<String, String[]> trajectoryPool;
    private Map<Integer, String[]> rawEdgeLookup;
    private SimilarityFunction simFunc;


    TopKQuery(TopKQueryIndex index, Mapper mapper, Map<String, String[]> trajectoryPool, Map<Integer, String[]> rawEdgeLookup){
        this.index = index;
        this.mapper = mapper;
        this.rawEdgeLookup = rawEdgeLookup;
        this.trajectoryPool = trajectoryPool;
        useEdge = true;
    }

    TopKQuery(TopKQueryIndex index, Mapper mapper, Map<String, String[]> trajectoryPool, Map<Integer, TowerVertex> idVertexLookup, SimilarityFunction simFunc){
        this.index = index;
        this.mapper = mapper;
        this.trajectoryPool = trajectoryPool;
        this.simFunc = simFunc;
        this.idVertexLookup = idVertexLookup;
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
        logger.info("top {} trajectory id set: {}",trajIds.size(),trajIds);
        return QueryResult.construct(trajIds, trajectoryPool, rawEdgeLookup);
    }


    private QueryResult topkusingVertex(int k) {
        List<String> trajIds;

        trajIds = index.findTopK(k, mapped, null);

        List<Trajectory<TrajEntry>> ret = new ArrayList<>(trajIds.size());
        for (String trajId : trajIds){

            String[] trajectory = trajectoryPool.get(trajId);
            Trajectory<TrajEntry> t = new Trajectory<>();
            t.id = trajId;
            for (int i = 1; i < trajectory.length; i++)
                t.add(idVertexLookup.get(Integer.valueOf(trajectory[i])));

            ret.add(t);
        }

        return QueryResult.construct(trajIds, trajectoryPool, rawEdgeLookup);
    }
}
