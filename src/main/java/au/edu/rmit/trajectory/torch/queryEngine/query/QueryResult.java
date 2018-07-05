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

    QueryResult(List<Trajectory<TrajEntry>> trajectories){
        this.trajectories = trajectories;
    }

    public List<String> getMapVFormat(){
        return null;
    }

    public List<Trajectory<TrajEntry>> getResultTrajectory(){
        return trajectories;
    }
}
