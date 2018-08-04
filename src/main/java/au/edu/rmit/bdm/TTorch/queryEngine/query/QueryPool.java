package au.edu.rmit.bdm.TTorch.queryEngine.query;

import au.edu.rmit.bdm.TTorch.base.Index;
import au.edu.rmit.bdm.TTorch.base.Instance;
import au.edu.rmit.bdm.TTorch.base.Torch;
import au.edu.rmit.bdm.TTorch.base.db.TrajVertexRepresentationPool;
import au.edu.rmit.bdm.TTorch.base.invertedIndex.EdgeInvertedIndex;
import au.edu.rmit.bdm.TTorch.base.invertedIndex.VertexInvertedIndex;
import au.edu.rmit.bdm.TTorch.base.model.TrajEntry;
import au.edu.rmit.bdm.TTorch.base.model.Trajectory;
import au.edu.rmit.bdm.TTorch.base.spatialIndex.LEVI;
import au.edu.rmit.bdm.TTorch.base.spatialIndex.VertexGridIndex;
import au.edu.rmit.bdm.TTorch.mapMatching.algorithm.Mapper;
import au.edu.rmit.bdm.TTorch.mapMatching.algorithm.Mappers;
import au.edu.rmit.bdm.TTorch.mapMatching.algorithm.TorGraph;
import au.edu.rmit.bdm.TTorch.mapMatching.model.TowerVertex;
import au.edu.rmit.bdm.TTorch.queryEngine.similarity.SimilarityFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryPool extends HashMap<String, Query> {

    private static final Logger logger = LoggerFactory.getLogger(QueryPool.class);
    private final QueryProperties props;
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
        this.props = props;

        //initialize queries and map-matching algorithm
        init();

    }

    private void init() {

        buildMapper();
        resolver = new TrajectoryResolver(props.resolveAll);

        if (props.queryUsed.contains(Torch.QueryType.PathQ))
            put(Torch.QueryType.PathQ, initPathQuery());

        if (props.queryUsed.contains(Torch.QueryType.RangeQ))
            put(Torch.QueryType.RangeQ, initRangeQuery());

        if (props.queryUsed.contains(Torch.QueryType.TopK))
            put(Torch.QueryType.TopK, initTopKQuery());
    }


    private void buildMapper() {
        if (mapper != null)
            return;

        //read meta properties
        try(FileReader fr = new FileReader(Instance.fileSetting.metaURI);
            BufferedReader reader = new BufferedReader(fr)){
            String vehicleType = reader.readLine();
            String osmPath = reader.readLine();

            TorGraph graph = TorGraph.getInstance().
                    initGH(Instance.fileSetting.hopperURI, osmPath, vehicleType).buildFromDiskData();

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
        if (props.preferedIndex.equals(Torch.Index.EDGE_INVERTED_INDEX)) {
            initEdgeInvertedIndex();
            return props.resolveAll ? new TopKQuery(edgeInvertedIndex, mapper, resolver) :
                    new TopKQuery(edgeInvertedIndex, mapper, resolver);
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
            if (!edgeInvertedIndex.build(Instance.fileSetting.EDGE_INVERTED_INDEX))
                throw new RuntimeException("some critical data is missing, system on exit...");
            edgeInvertedIndex.loaded = true;
        }
    }


    private void initLEVI() {

        if (LEVI!=null) return;

        VertexInvertedIndex vertexInvertedIndex = new VertexInvertedIndex();
        VertexGridIndex vertexGridIndex = new VertexGridIndex(idVertexLookup, 100);
        TrajVertexRepresentationPool trajVertexRepresentationPool = new TrajVertexRepresentationPool(false);

        if (!vertexInvertedIndex.build(Instance.fileSetting.VERTEX_INVERTED_INDEX))
            throw new RuntimeException("some critical data is missing, system on exit...");
        vertexInvertedIndex.loaded = true;


        if (!vertexGridIndex.loaded){
            if (!vertexGridIndex.build(Instance.fileSetting.GRID_INDEX))
                throw new RuntimeException("some critical data is missing, system on exit...");
            vertexGridIndex.loaded = true;
        }

        SimilarityFunction.MeasureType measureType = convertMeasureType(props.similarityMeasure);
        this.LEVI = new LEVI(vertexInvertedIndex, vertexGridIndex, measureType, trajVertexRepresentationPool, idVertexLookup);
    }

    public void update(String queryType, Map<String,String> props) {
        Query q = get(queryType);

        if (props.containsKey("simFunc") && LEVI != null)
            LEVI.updateMeasureType(convertMeasureType(props.get("simFunc")));
        if (props.containsKey("index")){
            q.updateIdx(convertIndex(props.get("index")));
        }
    }

    private Index convertIndex(String indexType){
        Index index;
        switch (indexType){
            case Torch.Index.EDGE_INVERTED_INDEX:
                if (!edgeInvertedIndex.loaded)
                    initEdgeInvertedIndex();
                index = edgeInvertedIndex;
                break;
            case Torch.Index.LEVI:
                if (LEVI == null)
                    initLEVI();
                index = LEVI;
                break;
            default:
                throw new IllegalStateException("please lookup Torch.Index for currently supported index type");
        }
        return index;
    }

    private SimilarityFunction.MeasureType convertMeasureType(String preferedDistFunc) {
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
            case Torch.Algorithms.LCSS:
                measureType = SimilarityFunction.MeasureType.LCSS;
                break;
            case Torch.Algorithms.EDR:
                measureType = SimilarityFunction.MeasureType.EDR;
                break;
            default:
                throw new IllegalStateException("please lookup Torch.Algorithms for currently supported measure type");
        }
        return measureType;
    }

    public QueryResult resolve(int[] idArr) {
        List<Trajectory<TrajEntry>> resolved = resolver.resolveResult(idArr);
        return new QueryResult(resolved);
    }
}
