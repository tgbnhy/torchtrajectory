package au.edu.rmit.trajectory.torch.mapMatching;

import au.edu.rmit.trajectory.torch.mapMatching.algorithm.Mapper;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.Mappers;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.TorDijkstra;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.TorGraph;
import au.edu.rmit.trajectory.torch.mapMatching.io.TrajReader;
import au.edu.rmit.trajectory.torch.base.helper.MemoryUsage;
import au.edu.rmit.trajectory.torch.base.Torch;
import au.edu.rmit.trajectory.torch.mapMatching.io.TorSaver;
import au.edu.rmit.trajectory.torch.mapMatching.model.TowerVertex;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * An mapMatching object is for projecting raw trajectory data to graph.
 * There are options to customize your mapMatching object.
 * @see Builder
 *
 * Note that there will be only one MapMatching object for each application instance.
 * Do not share it between threads as it is not designed to do so.
 *
 * Usage:
 * MapMatching mm = MapMatching.getBuilder().build("Resources/porto_raw_trajectory.txt","Resources/porto.osm.pbf");
 * mm.start();
 * @see #start()
 */
public class MapMatching {

    private static Logger logger = LoggerFactory.getLogger(MapMatching.class);
    private static Builder props = new Builder();

    private TorGraph graph;
    private Mapper mapper;

    public static Builder getBuilder(){
        return props;
    }

    private MapMatching(){

        //check trajSrcPath file
        File trajFile = new File(props.trajSrcPath);
        if (!trajFile.exists()) {
            logger.error("{} does not exist", props.trajSrcPath);
            throw new RuntimeException();
        }

        File PBFFile = new File(props.osmPath);
        if (!PBFFile.exists()){
            logger.error("{} does not exist", props.osmPath);
            System.exit(-1);
            throw new RuntimeException();
        }

        //check output directory
        File dir = new File(Torch.URI.TORCH_META_PREFIX);
        if (!dir.exists()) {
            if (!dir.mkdirs()){
                logger.error("{} cannot make directory, possibly Torch do not have permission for it.", Torch.URI.TORCH_META_PREFIX);
                throw new RuntimeException();
            }
        }else if (!dir.isDirectory()){
            logger.error("{} already exists and it is not a directory", Torch.URI.TORCH_META_PREFIX);
            throw new RuntimeException();
        }
    }

    /**
     * readBatch raw trajectory data --> map it on graph --> store mapped trajectories on disk.
     * Since some times the trajectory data file is too large and it cannot be loaded into memory at once,
     * the subroutine will readBatch and do the work on batch. The batch size could be specified via
     *
     * @see Builder#setBatchSize(int)
     */
    public void start(){

        TorSaver saver = new TorSaver();
        TrajReader reader = new TrajReader(props);
        MemoryUsage.start();

        //readBatch and build graph
        if (graph == null) {
            graph = TorGraph.getInstance().
                    initGH(Torch.URI.HOPPER_META, props.osmPath, props.vehicleType);
            MemoryUsage.printCurrentMemUsage("[after init graph hopper]");
            graph.build(props);
            MemoryUsage.printCurrentMemUsage("[after build tor graph]");
        }

        mapper = Mappers.getMapper(props.mmAlg, graph);

        //readBatch trajectory data in batch from file
        List<Trajectory<TrajEntry>> rawTrajs = new LinkedList<>();
        while ( !reader.readBatch(props.trajSrcPath, null, rawTrajs)) {

            MemoryUsage.printCurrentMemUsage("[after loading trajectories]");

            //do map-matching
            List<Trajectory<TowerVertex>> mappedTrajectories = mapper.batchMatch(rawTrajs);

            //asyncSave data
            saver.asyncSave(mappedTrajectories, rawTrajs, false);
        }

        //do map-matching for the rest
        List<Trajectory<TowerVertex>> mappedTrajectories = mapper.batchMatch(rawTrajs);
        saver.asyncSave(mappedTrajectories, rawTrajs, true);
    }



//    /**
//     * todo
//     *
//     * In addition to required files, you can pass your timestamp file along to mm.
//     * Time stamp provides information that would help in the query.
//     * It won't make difference in trajectory map-matching.
//     *
//     */
//    private static MapMatching build(String trajSrc, String dateSrc, String PBFPath, String outDir) {
//
//        MapMatching mm = MapMatching.build(trajSrc, PBFPath, outDir);
//
//        //check date file
//        File dateFile = new File(dateSrc);
//        if (!dateFile.exists()) {
//            logger.error("{} does not exist", dateSrc);
//            System.exit(-1);
//        }
//
//        mm.datePath = dateSrc;
//
//        return mm;
//    }

    /**
     * Builder to build mapMatching object.
     */
    public static class Builder {

        String vehicleType = Torch.vehicleType.CAR;
        String mmAlg = Torch.Algorithms.HMM_PRECOMPUTED;
        MapMatching mm;
        String trajSrcPath;
        String osmPath;
        int batchSize = 10000;
        int preComputationRange = 1000;

        /**
         * @param algorithm the algorithm used in map-matching
         * @see Torch.Algorithms
         */
        public Builder setMapMatchingAlgorithm(String algorithm){
            mmAlg = algorithm;
            return this;
        }

        /**
         * @param vehicle It tells osm parser which edges and vertices to build.
         *                As car and bike do not run on same roads
         * @see Torch.vehicleType
         */
        public Builder setVehicleType(String vehicle){
            vehicleType = vehicle;
            return this;
        }

        /**
         * @param range This param will be used if the algorithm used is Torch.Algorithms.HMM_PRECOMPUTED.
         *              It tells the range to compute shortest path information between src and its near points.
         * @see TorDijkstra#run(TowerVertex)
         */
        public Builder setPrecomputationRange(int range){
            this.preComputationRange = range;
            return this;
        }

        /**
         * T-Torch will build and process trajectories in batch as sometimes dataset is too large to fit into memory at once
         * This tell T-Torch how many trajectories should be loaded and processed at once.
         *
         * @see TrajReader
         */
        public Builder setBatchSize(int batchSize){
            this.batchSize = batchSize;
            return this;
        }

        /**
         * construct an MapMatching object with required files.
         *
         * @param trajSrcPath raw trajectory file with following format
         *                1    [[39.92123, 116.51172],[39.93883, 116.51135],[39.91034, 116.51627]]
         *                1 represents the hash of current trajectory.
         *                separated by \t character, a tuple of gps coordinates( lat, lng) that defines the trajectory.
         *
         * @param osmPath map data in format of *.osm.pbf
         * @return mapMatching object
         */
        public MapMatching build(String trajSrcPath, String osmPath){

            if ( mm != null )return mm;
            this.trajSrcPath = trajSrcPath;
            this.osmPath = osmPath;
            mm = new MapMatching();
            return mm;
        }

        public int getBatchSize() {
            return batchSize;
        }
        public int getPreComputationRange() {
            return preComputationRange;
        }
        public String getMmAlg() {
            return mmAlg;
        }
    }
}
