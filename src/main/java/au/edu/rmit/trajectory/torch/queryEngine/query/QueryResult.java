package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.model.Coordinate;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class QueryResult {
    private static final Logger logger = LoggerFactory.getLogger(QueryResult.class);
    private List<Trajectory<TrajEntry>> trajectories;

    private QueryResult(List<Trajectory<TrajEntry>> trajectories){
        this.trajectories = trajectories;
    }

    public List<String> getMapVFormat(){
        return null;
    }

    public List<Trajectory<TrajEntry>> getResultTrajectory(){
        return trajectories;
    }

    static QueryResult construct(Collection<String> trajIds, Map<String, String[]> trajectoryPool, Map<Integer, String[]> rawEdgeLookup){

        logger.info("total qualified trajectories: {}", trajIds.size());
        logger.info("matched trajectory id set: {}",trajIds);

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
//            logger.debug("result trajectory: {}", t);
            ret.add(t);
        }

        return new QueryResult(ret);
    }
}
