package au.edu.rmit.trajectory.torch.base.invertedIndex;

import au.edu.rmit.trajectory.torch.base.Index;
import au.edu.rmit.trajectory.torch.base.Torch;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import me.lemire.integercompression.differential.IntegratedIntCompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static au.edu.rmit.trajectory.torch.base.Torch.SEPARATOR;
import static au.edu.rmit.trajectory.torch.base.helper.FileUtil.*;

public abstract class InvertedIndex implements Index {

    private static Logger logger = LoggerFactory.getLogger(InvertedIndex.class);
    public boolean loaded = false;
    HashMap<Integer, Map<String, Integer>> index = new HashMap<>();
    HashMap<Integer, CompressedPair> compressedIndex = new HashMap<>();
    IntegratedIntCompressor iic = new IntegratedIntCompressor();


    /**
     * invertedIndex a list of trajectories, either by edges or vertices.
     * @param trajectories trajectories to be indexed
     */
    public abstract <T extends TrajEntry> void indexAll(List<Trajectory<T>> trajectories);

    /**
     * invertedIndex a list of trajectories, either by edges or vertices.
     * @param trajectories trajectories to be indexed
     */
    public abstract <T extends TrajEntry> void index(Trajectory<T> trajectories);

    /**
     * write inverted indexes to disk in a specific format
     * @param path URI to save the indexes
     */
    public final void save(String path){
        ensureExistence(path);

        try (BufferedWriter idBufWriter = new BufferedWriter((new FileWriter(path + "_id.txt", false)));
             BufferedWriter trajBufWriter = new BufferedWriter((new FileWriter(path + "_trajId.txt", false)));
             BufferedWriter posBufWriter = new BufferedWriter((new FileWriter(path+ "_pos.txt", false)))) {

            for (Map.Entry<Integer, Map<String, Integer>> entry : index.entrySet()) {

                //write id
                idBufWriter.write(String.valueOf(entry.getKey()));
                idBufWriter.newLine();

                //sort inverted list
                List<Pair> l = new ArrayList<>(entry.getValue().size());
                for(Map.Entry<String, Integer> entry1 : entry.getValue().entrySet())
                    l.add(new Pair(entry1));

                l.sort(Comparator.comparingInt(p -> p.trajid));

                //write inverted list

                for (Pair pair : l) {
                    //write trjectory id
                    trajBufWriter.write(pair.trajid + SEPARATOR);
                    //write position
                    posBufWriter.write(pair.pos + SEPARATOR);
                }
                trajBufWriter.newLine();
                posBufWriter.newLine();
            }

            idBufWriter.flush();
            trajBufWriter.flush();
            posBufWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     public List<Pair> getPairs(int vertexId){

        CompressedPair compressedPair = compressedIndex.get(vertexId);
        int[] trajIds = iic.uncompress(compressedPair.trajIds);
        int[] posis = iic.uncompress(compressedPair.posis);
        List<Pair> pairs = new LinkedList<>();
        for (int i = 0; i < trajIds.length; i++){
            pairs.add(new Pair(trajIds[i], posis[i]));
        }
        return pairs;
    }

    public List<String> getKeys(int vertexId){
        CompressedPair compressedPair = compressedIndex.get(vertexId);
        int[] trajIds = iic.uncompress(compressedPair.trajIds);
        List<String> l = new LinkedList<>();
        for (int trajId: trajIds)
            l.add(String.valueOf(trajId));

        return l;
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
                int[] trajId2beCompressed = new int[trajArray.length], pos2beCompressed = new int[posArray.length];

                for (int i = 0; i < trajId2beCompressed.length; i++){
                    trajId2beCompressed[i] = Integer.parseInt(trajArray[i]);
                    pos2beCompressed[i] = Integer.parseInt(posArray[i]);
                }

                CompressedPair p = new CompressedPair();
                p.trajIds = iic.compress(trajId2beCompressed);
                p.posis = iic.compress(pos2beCompressed);

                compressedIndex.put(Integer.valueOf(idString), p);
            }

            loaded = true;
            logger.info("build complete - " + compressedIndex.size());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("edge dataStructure file doesn't exist");
        return false;
    }

    public static class CompressedPair {
        public int[] trajIds;
        public int[] posis;
    }

    public static class Pair{
        Pair(Map.Entry<String, Integer> entry){
            this.trajid = Integer.parseInt(entry.getKey());
            this.pos = entry.getValue();
        }

        Pair(int trajid, int pos){
            this.trajid = trajid;
            this.pos = pos;
        }
        int trajid;
        int pos;
    }
}
