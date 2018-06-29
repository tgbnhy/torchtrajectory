package au.edu.rmit.trajectory.torch.index;

import au.edu.rmit.trajectory.torch.Torch;
import au.edu.rmit.trajectory.torch.model.TrajEntry;
import au.edu.rmit.trajectory.torch.model.Trajectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static au.edu.rmit.trajectory.torch.Torch.Props.SEPARATOR2;
import static au.edu.rmit.trajectory.torch.helper.FileUtil.*;

public abstract class InvertedIndex extends HashMap<String, Map<String, Integer>> {

    private static Logger logger = LoggerFactory.getLogger(InvertedIndex.class);
    /**
     * index a list of trajectories, either by edges or vertices.
     * @param trajectories trajectories to be indexed
     */
    public <T extends TrajEntry> void indexAll(List<Trajectory<T>> trajectories){}


    /**
     * write inverted indexes to disk in a specific format
     * @param path URI to save the indexes
     */
    public final void toDisk(String path){
        ensureExistence(path);

        try (BufferedWriter idBufWriter = new BufferedWriter((new OutputStreamWriter(new FileOutputStream(path + "_id.txt", false), StandardCharsets.UTF_8)));
             BufferedWriter trajBufWriter = new BufferedWriter((new OutputStreamWriter(new FileOutputStream(path + "_trajId.txt", false), StandardCharsets.UTF_8)));
             BufferedWriter posBufWriter = new BufferedWriter((new OutputStreamWriter(new FileOutputStream(path+ "_pos.txt", false), StandardCharsets.UTF_8)))) {

            for (Map.Entry<String, Map<String, Integer>> entry : this.entrySet()) {

                //write id
                idBufWriter.write(entry.getKey());
                idBufWriter.newLine();

                //write inverted list
                Collection<Map.Entry<String, Integer>> pairs = entry.getValue().entrySet();
                List<Map.Entry<String, Integer>> invertedList = pairs.stream().sorted(Comparator.comparing(e -> Integer.valueOf(e.getKey()))).collect(Collectors.toList());

                for (Map.Entry<String, Integer> pair : invertedList) {
                    //write trjectory hash
                    trajBufWriter.write(pair.getKey() + SEPARATOR2);
                    //write position
                    posBufWriter.write(pair.getValue() + SEPARATOR2);
                }
                trajBufWriter.newLine();
                posBufWriter.newLine();
            }

            trajBufWriter.flush();
            posBufWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * load edge dataStructure files from disk
     * the in-memory edge-dataStructure is an field of this instance
     *
     * @return true if the dataStructure file can be load and construct successfully
     *         false if indexes cannot be construct( cannot find dataStructure file or some other reasons)
     */
    public boolean load(String path) {

        if (!path.equals(Torch.Props.EDGE_INVERTED_INDEX) &&
                !path.equals(Torch.Props.VERTEX_INVERTED_INDEX))
            throw new IllegalStateException("base path got to be "+Torch.Props.EDGE_INVERTED_INDEX+" or "+Torch.Props.VERTEX_INVERTED_INDEX);

        try (BufferedReader idBufReader = new BufferedReader(new FileReader(path + "_id.txt"));
             BufferedReader trajBufReader = new BufferedReader(new FileReader(path + "_trajId.txt"));
             BufferedReader posBufReader = new BufferedReader(new FileReader(path + "_pos.txt"))) {
            String trajLine, posLine, idString;
            while ((idString = idBufReader.readLine()) != null) {

                trajLine = trajBufReader.readLine();
                posLine = posBufReader.readLine();

                String[] trajArray = trajLine.split(SEPARATOR2), posArray = posLine.split(SEPARATOR2);
                Map<String, Integer> invertedList= new HashMap<>();

                for (int i = 0; i < trajArray.length; i++)
                    invertedList.put(trajArray[i], Integer.valueOf(posArray[i]));

                this.put(idString, invertedList);
            }

            logger.info("load complete - " + this.size());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("edge dataStructure file doesn't exist");
        return false;
    }
}
