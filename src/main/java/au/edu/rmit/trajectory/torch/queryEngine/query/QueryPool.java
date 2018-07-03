package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.Torch;
import au.edu.rmit.trajectory.torch.base.invertedIndex.EdgeInvertedIndex;
import au.edu.rmit.trajectory.torch.base.invertedIndex.InvertedIndex;
import au.edu.rmit.trajectory.torch.base.invertedIndex.VertexInvertedIndex;
import au.edu.rmit.trajectory.torch.base.persistance.TrajectoryMap;
import au.edu.rmit.trajectory.torch.base.spatialIndex.LEVI;
import au.edu.rmit.trajectory.torch.base.spatialIndex.VertexGridIndex;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.Mapper;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.Mappers;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.TorGraph;
import au.edu.rmit.trajectory.torch.mapMatching.model.TowerVertex;
import au.edu.rmit.trajectory.torch.queryEngine.model.QueryProperties;
import au.edu.rmit.trajectory.torch.queryEngine.similarity.SimilarityFunction;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class QueryPool extends HashMap<String, Query> {

    private static final Logger logger = LoggerFactory.getLogger(QueryPool.class);
    private final boolean useRawDataSet;
    private final Set<String> queryUsed;
    private final String preferedDistFunc;

    private Mapper mapper;
    private EdgeInvertedIndex edgeInvertedIndex = new EdgeInvertedIndex();
    private VertexInvertedIndex vertexInvertedIndex = new VertexInvertedIndex();
    private VertexGridIndex vertexGridIndex;
    private LEVI LEVI;

    private Map<String, String[]> trajectoryPool = new HashMap<>();
    private Map<Integer, TowerVertex> idVertexLookup;

    /**
     * initilize supported indexes for the 4 types of queries.
     * @param props
     */
    public QueryPool(QueryProperties props) {

        //set client preference
        useRawDataSet = props.dataUsed();
        queryUsed = props.queryUsed();
        preferedDistFunc = props.getSimilarityMeasure();

        //initialize queries and map-matching algorithm
        init();
    }


    private void init() {

        buildMapper();
        loadTrajectories();

        vertexGridIndex = new VertexGridIndex(idVertexLookup, 100);


        if (queryUsed.contains(Torch.QueryType.PathQ))
            put(Torch.QueryType.PathQ, initPathQuery());

        if (queryUsed.contains(Torch.QueryType.StrictPathQ))
            put(Torch.QueryType.StrictPathQ, initStrictPathQuery());

        if (queryUsed.contains(Torch.QueryType.RangeQ))
            put(Torch.QueryType.RangeQ,initRangeQuery());

        if (queryUsed.contains(Torch.QueryType.TopK))
            put(Torch.QueryType.TopK, initTopKQuery());
    }

    private void loadTrajectories() {

        //read meta properties
        try(FileReader fr = new FileReader(Torch.URI.TRAJECTORY_VERTEX_REPRESENTATION_PATH);
            BufferedReader reader = new BufferedReader(fr)){

            String line;
            String[] tokens;
            String trajId;

            while((line = reader.readLine()) != null){
                tokens = line.split(";");
                trajId = tokens[0];
                trajectoryPool.put(trajId, tokens);
            }

        }catch (IOException e){
            logger.error("some critical data is missing, system on exit...");
            System.exit(-1);
        }
    }

    private void buildMapper() {
        if (mapper != null)
            return;

        //read meta properties
        try(FileReader fr = new FileReader(Torch.URI.META);
            BufferedReader reader = new BufferedReader(fr)){
            String vehicleType = reader.readLine();
            String osmPath = reader.readLine();

            TorGraph graph = TorGraph.getInstance().
                    initGH(Torch.URI.HOPPER_META, osmPath, vehicleType).buildFromDiskData();

            idVertexLookup = graph.idVertexLookup;
            mapper = Mappers.getMapper(Torch.Algorithms.HMM, graph);

        }catch (IOException e){
            logger.error("some critical data is missing, system on exit...");
            System.exit(-1);
        }

    }

    private Query initPathQuery() {
        if (!edgeInvertedIndex.loaded) {
            if (!edgeInvertedIndex.build(Torch.URI.EDGE_INVERTED_INDEX))
                throw new RuntimeException("some critical data is missing, system on exit...");
            edgeInvertedIndex.loaded = true;
        }

        return new PathQuery(edgeInvertedIndex, mapper, trajectoryPool, idVertexLookup);
    }

    private Query initStrictPathQuery() {

        if (!edgeInvertedIndex.loaded) {
            if (!edgeInvertedIndex.build(Torch.URI.EDGE_INVERTED_INDEX))
                throw new RuntimeException("some critical data is missing, system on exit...");
            edgeInvertedIndex.loaded = true;
        }

        return new StrictPathQuery(edgeInvertedIndex, mapper, trajectoryPool, idVertexLookup);
    }

    private Query initTopKQuery() {

        //edge topK
//        if (!edgeInvertedIndex.loaded) {
//            if (!edgeInvertedIndex.build(Torch.URI.EDGE_INVERTED_INDEX))
//                throw new RuntimeException("some critical data is missing, system on exit...");
//            edgeInvertedIndex.loaded = true;
//        }
//        return new TopKQuery(edgeInvertedIndex, mapper, idVertexLookup, trajectoryPool);

        //LEVI topK
        if (LEVI!= null) return new TopKQuery(LEVI, mapper, idVertexLookup, trajectoryPool);

        initLEVI();
        return new TopKQuery(LEVI, mapper, idVertexLookup, trajectoryPool);
    }

    private Query initRangeQuery() {

        if (LEVI!= null) return new WindowQuery(LEVI,
                idVertexLookup, trajectoryPool);

        initLEVI();

        return new WindowQuery(LEVI,
                idVertexLookup, trajectoryPool);
    }

    private void initLEVI() {
        if (!vertexGridIndex.loaded){
            if (!vertexInvertedIndex.build(Torch.URI.VERTEX_INVERTED_INDEX))
                throw new RuntimeException("some critical data is missing, system on exit...");
            edgeInvertedIndex.loaded = true;
        }

        if (!vertexGridIndex.loaded){
            if (!vertexGridIndex.build(Torch.URI.GRID_INDEX))
                throw new RuntimeException("some critical data is missing, system on exit...");
            vertexGridIndex.loaded = true;
        }

        SimilarityFunction.MeasureType measureType;
        if (preferedDistFunc.equals(Torch.Algorithms.DTW))
            measureType = SimilarityFunction.MeasureType.DTW;
        else if (preferedDistFunc.equals(Torch.Algorithms.Frechet))
            measureType = SimilarityFunction.MeasureType.Frechet;
        else if (preferedDistFunc.equals(Torch.Algorithms.Hausdorff))
            measureType = SimilarityFunction.MeasureType.Hausdorff;
        else
            throw new IllegalStateException("please lookup Torch.Algorithms for valid measure type");

        this.LEVI = new LEVI(vertexInvertedIndex, vertexGridIndex, measureType, trajectoryPool, idVertexLookup);
    }
}
