package au.edu.rmit.bdm.clustering.Streaming;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Streaming {
    private Simulator simulator;
    private Map<Integer, List<Integer>> edgeInvertedIndex;
    private Map<Integer, List<Integer>> trajectories;

    /**
     * @param windowSize sliding window in time dimension
     */
    public Streaming(int startFrom, int windowSize, int speedup) throws IOException {
        simulator= new Simulator(startFrom, windowSize, speedup);

    }

    public Streaming start() throws InterruptedException {
        simulator.start();
        return this;
    }

    public void updateIndexes() {
        edgeInvertedIndex = new HashMap<>();
        trajectories = new HashMap<>();
        for (Tuple t : simulator.cachedList){
            //update edge inverted index
            if (!edgeInvertedIndex.containsKey(t.edgeId))
                edgeInvertedIndex.put(t.edgeId, new ArrayList<>());
            edgeInvertedIndex.get(t.edgeId).add(t.carId);
            //update trajectory data
            if (!trajectories.containsKey(t.carId))
                trajectories.put(t.carId, new ArrayList<>());
            trajectories.get(t.carId).add(t.edgeId);
        }

        //sort
        for (List<Integer> l : edgeInvertedIndex.values())
            l.sort(Integer::compareTo);
        for (List<Integer> l : trajectories.values())
            l.sort(Integer::compareTo);
        System.out.println(edgeInvertedIndex);
        System.out.println(trajectories);
    }
}
