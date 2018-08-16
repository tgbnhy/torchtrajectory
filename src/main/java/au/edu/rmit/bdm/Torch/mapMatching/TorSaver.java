package au.edu.rmit.bdm.Torch.mapMatching;

import au.edu.rmit.bdm.Torch.base.Instance;
import au.edu.rmit.bdm.Torch.base.Torch;
import au.edu.rmit.bdm.Torch.mapMatching.algorithm.TorGraph;
import au.edu.rmit.bdm.Torch.base.invertedIndex.EdgeInvertedIndex;
import au.edu.rmit.bdm.Torch.base.invertedIndex.InvertedIndex;
import au.edu.rmit.bdm.Torch.base.invertedIndex.VertexInvertedIndex;
import au.edu.rmit.bdm.Torch.base.model.TorEdge;
import au.edu.rmit.bdm.Torch.mapMatching.model.TowerVertex;
import au.edu.rmit.bdm.Torch.base.model.*;
import com.github.davidmoten.geo.GeoHash;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static au.edu.rmit.bdm.Torch.base.helper.FileUtil.*;

/**
 * The class is for saving relevant information to disk.
 *
 * These includes:
 *      ~ vertexId -- GPS Coordinate table
 *
 *      ~ edgeId -- edgeInfo table
 *      ~ edgeId -- vertexId table
 *
 *      ~ map-matched trajectory represented by vertices
 *      ~ map-matched trajectory represented by edges
 *
 *      ~ edge inverted invertedIndex( trajectory ids)
 *      ~ vertex inverted invertedIndex( trajectory ids)
 */
public class TorSaver {

    private static Logger logger = LoggerFactory.getLogger(TorSaver.class);
    private TorGraph graph;
    private boolean append = false;

    public InvertedIndex edgeInvertedList;
    public InvertedIndex vertexInvertedIndex;


    public TorSaver(){
        edgeInvertedList = new EdgeInvertedIndex();
        vertexInvertedIndex = new VertexInvertedIndex();
    }

    /**
     * Once a batch of trajectories have been mapped, we saveUncompressed it using a separate thread.
     *
     * @param mappedTrajectories trajectories to be saved
     * @param saveAll false -- asyncSave trajectory data only
     *                true -- asyncSave everything( It should only be set to true if it is the last batch.)
     *                As the method is expected to be called multiple times to write different batches of trajectories,
     *                other information should only be saved once.
     *
     *
     */
    public synchronized void asyncSave(final List<Trajectory<TowerVertex>> mappedTrajectories,final List<Trajectory<TrajEntry>> rawTrajectories, final boolean saveAll) {

        if (!TorGraph.getInstance().isBuilt)
            throw new IllegalStateException("should be called after TorGraph initialization");
        graph = TorGraph.getInstance();

        ExecutorService thread = Executors.newSingleThreadExecutor();
        thread.execute(() -> _save(mappedTrajectories,rawTrajectories, saveAll));
        thread.shutdown();

        try {
            thread.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            logger.error("{}", e);
        }
    }

    private void _save(final List<Trajectory<TowerVertex>> mappedTrajectories,final List<Trajectory<TrajEntry>> rawTrajectories, final boolean saveAll) {

        saveMappedTrajectories(mappedTrajectories);  // for purpose of debugging
        //trajectoryMap.saveAll(rawTrajectories);

        if (saveAll) {
            saveMeta();
            saveIdVertexLookupTable();
            saveEdges();
            edgeInvertedList.saveCompressed(Instance.fileSetting.EDGE_INVERTED_INDEX);
            vertexInvertedIndex.saveCompressed(Instance.fileSetting.VERTEX_INVERTED_INDEX);


            //trajectoryMap.cleanUp();
        }
    }

    private void saveMeta() {
        ensureExistence(Instance.fileSetting.metaURI);
        try(FileWriter fw = new FileWriter(Instance.fileSetting.metaURI);
            BufferedWriter writer = new BufferedWriter(fw))
        {
            writer.write(graph.vehicleType);
            writer.newLine();
            writer.write(graph.OSMPath);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveIdVertexLookupTable(){


        Graph hopperGraph = TorGraph.getInstance().getGH().getGraphHopperStorage().getBaseGraph();
        int numNodes = hopperGraph.getNodes();

        NodeAccess nodeAccess = hopperGraph.getNodeAccess();

        ensureExistence(Instance.fileSetting.ID_VERTEX_LOOKUP);

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(Instance.fileSetting.ID_VERTEX_LOOKUP, false))){
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < numNodes; i++){
                builder.append(i).append(";")
                        .append(nodeAccess.getLatitude(i)).append(";")
                        .append(nodeAccess.getLongitude(i));

                writer.write(builder.toString());
                writer.newLine();
                builder.setLength(0);
            }
            writer.flush();
        }catch (IOException e){
            logger.error(e.getMessage());
        }
    }


    private void saveEdges(){

        Collection<TorEdge> allEdges = TorGraph.getInstance().allEdges.values();

        List<TorEdge> edges = new ArrayList<>(allEdges);
        edges.sort(Comparator.comparing(e -> e.id));

        ensureExistence(Instance.fileSetting.ID_EDGE_RAW);

        try(BufferedWriter rawWriter = new BufferedWriter(new FileWriter(Instance.fileSetting.ID_EDGE_RAW));
            BufferedWriter writer = new BufferedWriter(new FileWriter(Instance.fileSetting.ID_EDGE_LOOKUP))) {

            StringBuilder builder = new StringBuilder();
            Set<Integer> visited = new HashSet<>();

            for (TorEdge edge : edges){

                if (visited.contains(edge.id)) continue;
                visited.add(edge.id);

                rawWriter.write(edge.convertToDatabaseForm());
                rawWriter.newLine();

                builder.append(edge.id).append(Torch.SEPARATOR_1)
                       .append(graph.vertexIdLookup.get(edge.baseVertex.hash)).append(Torch.SEPARATOR_1)
                       .append(graph.vertexIdLookup.get(edge.adjVertex.hash)).append(Torch.SEPARATOR_1)
                       .append(edge.getLength());

                writer.write(builder.toString());
                writer.newLine();
                builder.setLength(0);
            }

            rawWriter.flush();
            writer.flush();

        }catch (IOException e){
            logger.error(e.getMessage());
        }
    }

    private void saveMappedTrajectories(List<Trajectory<TowerVertex>> mappedTrajectories){

        if (!append) ensureExistence(Instance.fileSetting.TRAJECTORY_VERTEX_REPRESENTATION_PATH);

        //invertedIndex trajectories
        vertexInvertedIndex.indexAll(mappedTrajectories);
        edgeInvertedList.indexAll(mappedTrajectories);

        //write vertex id representation of trajectories.
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(Instance.fileSetting.TRAJECTORY_VERTEX_REPRESENTATION_PATH,append))) {

            StringBuilder trajBuilder = new StringBuilder();
            String hash;

            for (Trajectory<TowerVertex> traj : mappedTrajectories) {
                trajBuilder.append(traj.id).append(";");

                for (TowerVertex vertex : traj) {
                    hash = GeoHash.encodeHash(vertex.lat, vertex.lng);
                    Integer id = graph.vertexIdLookup.get(hash);

                    if (id == null)
                        logger.error("a mapped edge is missing when processing trajectory id "+ traj.id);
                    else
                        trajBuilder.append(id).append(";");
                }

                //remove the tail ";" character
                trajBuilder.setLength(trajBuilder.length()-1);
                writer.write(trajBuilder.toString());
                writer.newLine();

                trajBuilder.setLength(0);
            }

            writer.flush();

        }catch (IOException e){
            e.printStackTrace();
        }

        //write edge id representation of trajectories.
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(Instance.fileSetting.TRAJECTORY_EDGE_REPRESENTATION_PATH, append))) {

            StringBuilder trajBuilder = new StringBuilder();
            Iterator<TorEdge> iterator;
            TorEdge curEdge;

            for (Trajectory<TowerVertex> traj : mappedTrajectories) {

                trajBuilder.append(traj.id).append(";");
                iterator = traj.edges.iterator();

                while(iterator.hasNext()) {
                    curEdge = iterator.next();
                    trajBuilder.append(curEdge.id).append(";");
                }

                //remove the tail ";" character
                trajBuilder.setLength(trajBuilder.length()-1);
                writer.write(trajBuilder.toString());
                writer.newLine();

                trajBuilder.setLength(0);
            }

            writer.flush();

        }catch (IOException e){
            e.printStackTrace();
        }

        append = true;
    }
}
