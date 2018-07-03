package au.edu.rmit.trajectory.torch.base.invertedIndex;

import au.edu.rmit.trajectory.torch.base.Index;
import au.edu.rmit.trajectory.torch.base.Torch;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static au.edu.rmit.trajectory.torch.base.Torch.SEPARATOR;
import static au.edu.rmit.trajectory.torch.base.helper.FileUtil.*;

public abstract class InvertedIndex extends HashMap<Integer, Map<String, Integer>> implements Index {

    private static Logger logger = LoggerFactory.getLogger(InvertedIndex.class);
    public boolean loaded = false;

    /**
     * invertedIndex a list of trajectories, either by edges or vertices.
     * @param trajectories trajectories to be indexed
     */
    public abstract <T extends TrajEntry> void indexAll(List<Trajectory<T>> trajectories);


    /**
     * write inverted indexes to disk in a specific format
     * @param path URI to save the indexes
     */
    public final void save(String path){
        ensureExistence(path);

        try (BufferedWriter idBufWriter = new BufferedWriter((new OutputStreamWriter(new FileOutputStream(path + "_id.txt", false), StandardCharsets.UTF_8)));
             BufferedWriter trajBufWriter = new BufferedWriter((new OutputStreamWriter(new FileOutputStream(path + "_trajId.txt", false), StandardCharsets.UTF_8)));
             BufferedWriter posBufWriter = new BufferedWriter((new OutputStreamWriter(new FileOutputStream(path+ "_pos.txt", false), StandardCharsets.UTF_8)))) {

            for (Map.Entry<Integer, Map<String, Integer>> entry : this.entrySet()) {

                //write id
                idBufWriter.write(entry.getKey());
                idBufWriter.newLine();

                //write inverted list
                Collection<Map.Entry<String, Integer>> pairs = entry.getValue().entrySet();
                List<Map.Entry<String, Integer>> invertedList = pairs.stream().sorted(Comparator.comparing(e -> Integer.valueOf(e.getKey()))).collect(Collectors.toList());

                for (Map.Entry<String, Integer> pair : invertedList) {
                    //write trjectory hash
                    trajBufWriter.write(pair.getKey() + SEPARATOR);
                    //write position
                    posBufWriter.write(pair.getValue() + SEPARATOR);
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
     * build edge dataStructure files from disk
     * the in-memory edge-dataStructure is an field of this instance
     *
     * @return true if the dataStructure file can be build and construct successfully
     *         false if indexes cannot be construct( cannot find dataStructure file or some other reasons)
     */
    public boolean build(String path) {

        if (!path.equals(Torch.URI.EDGE_INVERTED_INDEX) &&
                !path.equals(Torch.URI.VERTEX_INVERTED_INDEX))
            throw new IllegalStateException("base path got to be "+Torch.URI.EDGE_INVERTED_INDEX+" or "+Torch.URI.VERTEX_INVERTED_INDEX);

        try (BufferedReader idBufReader = new BufferedReader(new FileReader(path + "_id.txt"));
             BufferedReader trajBufReader = new BufferedReader(new FileReader(path + "_trajId.txt"));
             BufferedReader posBufReader = new BufferedReader(new FileReader(path + "_pos.txt"))) {

            //idString either be edge id or tower point id.
            String trajIdLine, posLine, idString;
            while ((idString = idBufReader.readLine()) != null) {

                trajIdLine = trajBufReader.readLine();
                posLine = posBufReader.readLine();

                String[] trajArray = trajIdLine.split(SEPARATOR), posArray = posLine.split(SEPARATOR);
                Map<String, Integer> invertedList= new HashMap<>();

                for (int i = 0; i < trajArray.length; i++)
                    invertedList.put(trajArray[i], Integer.valueOf(posArray[i]));

                this.put(Integer.valueOf(idString), invertedList);
            }

            loaded = true;
            logger.info("build complete - " + this.size());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("edge dataStructure file doesn't exist");
        return false;
    }
}
