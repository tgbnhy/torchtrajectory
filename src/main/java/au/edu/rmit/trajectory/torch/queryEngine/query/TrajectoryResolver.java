package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.Torch;
import au.edu.rmit.trajectory.torch.base.helper.MemoryUsage;
import au.edu.rmit.trajectory.torch.base.model.Coordinate;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

class TrajectoryResolver {

    private Logger logger = LoggerFactory.getLogger(TrajectoryResolver.class);
    private Map<String, String[]> trajectoryPool;
    private Map<Integer, String[]> rawEdgeLookup;

    public TrajectoryResolver(Map<String, String[]> trajectoryPool, Map<Integer, String[]> rawEdgeLookup){
        this.trajectoryPool = trajectoryPool;
        this.rawEdgeLookup = rawEdgeLookup;
    }

    TrajectoryResolver(){
        trajectoryPool = new HashMap<>();
        rawEdgeLookup = new HashMap<>();
        loadEdgeRepresentedTrajectories();
        loadRawEdgeLookupTable();

        printStats();
    }

    QueryResult resolve (String queryType, List<String> trajIds, List<TrajEntry> rawQuery, Trajectory<TrajEntry> _mappedQuery){

        List<Trajectory<TrajEntry>> retSet = resolveRet(trajIds);
        List<TrajEntry> mappedQuery = _mappedQuery;

        if (!queryType.equals(Torch.QueryType.RangeQ))
            mappedQuery = resolveMappedQuery(_mappedQuery);

        return new QueryResult(queryType, retSet, rawQuery, mappedQuery);
    }

    private List<TrajEntry> resolveMappedQuery(Trajectory<TrajEntry> mappedQuery) {

        List<TrajEntry> l = new Trajectory<>();
        int queryLen = mappedQuery.edges.size();

        for (int i = 1; i < queryLen; i++) {

            String[] tokens = rawEdgeLookup.get(mappedQuery.edges.get(i).id);
            String[] lats = tokens[0].split(",");
            String[] lngs = tokens[1].split(",");

            for (int j = 0; j < lats.length; j++) {
                l.add(new Coordinate(Double.parseDouble(lats[j]),Double.parseDouble(lngs[j])));
            }
        }
        return l;
    }

    private List<Trajectory<TrajEntry>> resolveRet(Collection<String> trajIds) {
        List<Trajectory<TrajEntry>> ret = new ArrayList<>(trajIds.size());
        for (String trajId : trajIds){

            String[] edges = trajectoryPool.get(trajId);
            if (edges == null) {
                logger.debug("cannot find edge id {}, this should not be happened");
                continue;
            }

            Trajectory<TrajEntry> t = new Trajectory<>();
            t.id = trajId;

            for (int i = 1; i < edges.length; i++) {

                String[] tokens = rawEdgeLookup.get(Integer.valueOf(edges[i]));
                String[] lats = tokens[0].split(",");
                String[] lngs = tokens[1].split(",");

                for (int j = 0; j < lats.length; j++) {
                    t.add(new Coordinate(Double.parseDouble(lats[j]),Double.parseDouble(lngs[j])));
                }
            }
            ret.add(t);
        }
        return ret;
    }

    private void loadEdgeRepresentedTrajectories() {

        logger.info("load edge represented trajectories");
        //read meta properties
        try(FileReader fr = new FileReader(Torch.URI.TRAJECTORY_EDGE_REPRESENTATION_PATH_200000);
            BufferedReader reader = new BufferedReader(fr)){

            String line;
            String[] tokens;
            String trajId;

            while((line = reader.readLine()) != null){
                tokens = line.split("\t");
                trajId = tokens[0];
                trajectoryPool.put(trajId, tokens[1].split(","));
            }

        }catch (IOException e){
            logger.error("some critical data is missing, system on exit...");
            System.exit(-1);
        }
    }

    private void loadRawEdgeLookupTable() {

        logger.info("load raw edge lookup table");

        try(FileReader fr = new FileReader(Torch.URI.ID_EDGE_RAW);
            BufferedReader reader = new BufferedReader(fr)){
            String line;
            String[] tokens;
            int id;
            String lats;
            String lngs;
            while((line = reader.readLine())!=null){
                tokens = line.split(";");
                id = Integer.parseInt(tokens[0]);
                lats = tokens[1];
                lngs = tokens[2];

                rawEdgeLookup.put(id, new String[]{lats, lngs});
            }

        }catch (IOException e){
            throw new RuntimeException("some critical data is missing, system on exit...");
        }
    }

    /**
     * for purpose of memory inspection
     */
    private void printStats() {
        MemoryUsage.printObjectMemUsage("trajectory edge representation", trajectoryPool);
        MemoryUsage.printObjectMemUsage("raw-edge-lookup-table", rawEdgeLookup);
    }

}
