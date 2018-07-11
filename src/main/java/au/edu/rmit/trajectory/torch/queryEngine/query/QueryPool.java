package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.Torch;
import au.edu.rmit.trajectory.torch.base.helper.MemoryUsage;
import au.edu.rmit.trajectory.torch.base.invertedIndex.EdgeInvertedIndex;
import au.edu.rmit.trajectory.torch.base.invertedIndex.VertexInvertedIndex;
import au.edu.rmit.trajectory.torch.base.spatialIndex.LEVI;
import au.edu.rmit.trajectory.torch.base.spatialIndex.VertexGridIndex;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.Mapper;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.Mappers;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.TorGraph;
import au.edu.rmit.trajectory.torch.mapMatching.model.TowerVertex;
import au.edu.rmit.trajectory.torch.queryEngine.similarity.SimilarityFunction;
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
    private final String preferedIndex;
    private Mapper mapper;
    private EdgeInvertedIndex edgeInvertedIndex = new EdgeInvertedIndex();
    private LEVI LEVI;


    private Map<Integer, TowerVertex> idVertexLookup;
    private TrajectoryResolver resolver;

    /**
     * initilize supported indexes for the 4 types of queries.
     * @param props
     */
    public QueryPool(QueryProperties props) {
        //set client preference
        useRawDataSet = props.useRaw;
        queryUsed = props.queryUsed;
        preferedDistFunc = props.similarityMeasure;
        preferedIndex = props.preferedIndex;

        //initialize queries and map-matching algorithm
        init();

    }

    private void init() {

        buildMapper();
        resolver = new TrajectoryResolver();

        if (queryUsed.contains(Torch.QueryType.PathQ))
            put(Torch.QueryType.PathQ, initPathQuery());

        if (queryUsed.contains(Torch.QueryType.RangeQ))
            put(Torch.QueryType.RangeQ, initRangeQuery());

        if (queryUsed.contains(Torch.QueryType.TopK))
            put(Torch.QueryType.TopK, initTopKQuery());
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
            MemoryUsage.printObjectMemUsage("graph", graph);

            idVertexLookup = graph.idVertexLookup;
            mapper = Mappers.getMapper(Torch.Algorithms.HMM, graph);

        }catch (IOException e){
            logger.error("some critical data is missing, system on exit...");
            System.exit(-1);
        }

    }

    private Query initPathQuery() {

        initEdgeInvertedIndex();

        return new PathQuery(edgeInvertedIndex, mapper, resolver);
    }

    private Query initTopKQuery() {

        // edge based top K
        if (preferedIndex.equals(Torch.Index.EDGE_INVERTED_INDEX)) {
            initEdgeInvertedIndex();
            return new TopKQuery(edgeInvertedIndex, mapper, resolver);
        }

        // point based topK with GVI
        initLEVI();
        return new TopKQuery(LEVI, mapper, resolver);
    }

    private Query initRangeQuery() {
        initEdgeInvertedIndex();
        if (LEVI == null) initLEVI();
        return new WindowQuery(LEVI, resolver);
    }

    private void initEdgeInvertedIndex() {
        if (!edgeInvertedIndex.loaded) {
            if (!edgeInvertedIndex.build(Torch.URI.EDGE_INVERTED_INDEX))
                throw new RuntimeException("some critical data is missing, system on exit...");
            edgeInvertedIndex.loaded = true;
        }
    }


    private void initLEVI() {

        if (LEVI!=null) return;

        VertexInvertedIndex vertexInvertedIndex = new VertexInvertedIndex();
        VertexGridIndex vertexGridIndex = new VertexGridIndex(idVertexLookup, 100);
        Map<String, String[]> trajectoryPool = loadVertexRepresentedTrajectories();

        if (!vertexInvertedIndex.build(Torch.URI.VERTEX_INVERTED_INDEX))
            throw new RuntimeException("some critical data is missing, system on exit...");
        vertexInvertedIndex.loaded = true;


        if (!vertexGridIndex.loaded){
            if (!vertexGridIndex.build(Torch.URI.GRID_INDEX))
                throw new RuntimeException("some critical data is missing, system on exit...");
            vertexGridIndex.loaded = true;
        }

        SimilarityFunction.MeasureType measureType;
        switch (preferedDistFunc) {
            case Torch.Algorithms.DTW:
                measureType = SimilarityFunction.MeasureType.DTW;
                break;
            case Torch.Algorithms.Frechet:
                measureType = SimilarityFunction.MeasureType.Frechet;
                break;
            case Torch.Algorithms.Hausdorff:
                measureType = SimilarityFunction.MeasureType.Hausdorff;
                break;
            default:
                throw new IllegalStateException("please lookup Torch.Algorithms for valid measure type");
        }

        this.LEVI = new LEVI(vertexInvertedIndex, vertexGridIndex, measureType, trajectoryPool, idVertexLookup);

        MemoryUsage.printObjectMemUsage("vertexGridIndex", vertexGridIndex);
    }

    private Map<String, String[]> loadVertexRepresentedTrajectories() {

        Map<String, String[]> trajectoryPool = new HashMap<>();

        //read meta properties
        try(FileReader fr = new FileReader(Torch.URI.TRAJECTORY_VERTEX_REPRESENTATION_PATH_200000);
            BufferedReader reader = new BufferedReader(fr)){

            String line;
            String[] tokens;
            String trajId;

            while((line = reader.readLine()) != null){
                tokens = line.split("\t");
                trajId = tokens[0];
                trajectoryPool.put(trajId, tokens[1].split(","));
            }

            MemoryUsage.printObjectMemUsage("trajectory vertex representation", trajectoryPool);

        }catch (IOException e){
            logger.error("some critical data is missing, system on exit...");
            System.exit(-1);
        }
        return trajectoryPool;
    }

}
