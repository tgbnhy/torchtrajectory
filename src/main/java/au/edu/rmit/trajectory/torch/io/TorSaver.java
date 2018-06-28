package au.edu.rmit.trajectory.torch.io;

import au.edu.rmit.trajectory.torch.Torch;
import au.edu.rmit.trajectory.torch.mapping.TorGraph;
import au.edu.rmit.trajectory.torch.model.*;
import com.github.davidmoten.geo.GeoHash;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The class is for saving results to disk.
 * Those result will be used for building index later.
 */
public class TorSaver {

    private static Logger logger = LoggerFactory.getLogger(TorSaver.class);
    private TorGraph graph;
    private boolean append = false;

    public TorSaver(){}



     private void save(List<Trajectory<TowerVertex>> mappedTrajectories){
        if (!TorGraph.getInstance().isBuilt)
            throw new IllegalStateException("should be called after TorGraph initialization");

        graph = TorGraph.getInstance();

        saveIdVertexLookupTable();
        saveEdges();
        saveMappedTrajectories(mappedTrajectories);
    }

     public synchronized void asyncSave(List<Trajectory<TowerVertex>> mappedTrajectories){
        if (!TorGraph.getInstance().isBuilt)
            throw new IllegalStateException("should be called after TorGraph initialization");

        ExecutorService thread = Executors.newSingleThreadExecutor();
        thread.execute(()-> save(mappedTrajectories));

        thread.shutdown();

        try {
            thread.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            logger.error("{}", e);
        }
    }

    private void saveIdVertexLookupTable(){


        Graph hopperGraph = TorGraph.getInstance().getGH().getGraphHopperStorage().getBaseGraph();
        int numNodes = hopperGraph.getNodes();

        NodeAccess nodeAccess = hopperGraph.getNodeAccess();

        ensureExistence(Torch.Props.ID_VERTEX_LOOKUP);

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(Torch.Props.ID_VERTEX_LOOKUP, false))){
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
        edges.sort(Comparator.comparing(e -> Integer.valueOf(e.id)));

        ensureExistence(Torch.Props.ID_EDGE_RAW);

        try( BufferedWriter rawWriter = new BufferedWriter(new FileWriter(Torch.Props.ID_EDGE_RAW));
             BufferedWriter writer = new BufferedWriter(new FileWriter(Torch.Props.ID_EDGE_LOOKUP))) {

            StringBuilder builder = new StringBuilder();
            Set<String> visited = new HashSet<>();

            for (TorEdge edge : edges){

                if (visited.contains(edge.id)) continue;
                visited.add(edge.id);

                rawWriter.write(edge.convertToDatabaseForm());
                rawWriter.newLine();

                builder.append(edge.id).append(Torch.Props.SEPARATOR2)
                       .append(graph.vertexIdLookup.get(edge.baseVertex.hash)).append(Torch.Props.SEPARATOR2)
                       .append(graph.vertexIdLookup.get(edge.adjVertex.hash));

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

        //write vertex id representation of trajectories.
        ensureExistence(Torch.Props.TRAJECTORY_VERTEX_REPRESENTATION_PATH);

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(Torch.Props.TRAJECTORY_VERTEX_REPRESENTATION_PATH,append))) {

            StringBuilder trajBuilder = new StringBuilder();
            String hash;

            for (Trajectory<TowerVertex> traj : mappedTrajectories) {
                trajBuilder.append(traj.id).append(";");

                for (TorVertex vertex : traj) {
                    hash = GeoHash.encodeHash(vertex.lat, vertex.lng);
                    String id = graph.vertexIdLookup.get(hash);

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
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(Torch.Props.TRAJECTORY_EDGE_REPRESENTATION_PATH, append))) {

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

    private void ensureExistence(String path){
        File f = new File(path);
        if (!f.getParentFile().exists()){
            f.getParentFile().mkdirs();
        }
    }
}
