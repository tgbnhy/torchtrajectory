package au.edu.rmit.trajectory.torch.queryEngine.model;

import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;

import java.util.List;

public class QueryResult {
    private List<Trajectory<TrajEntry>> trajectories;

    public QueryResult(List<Trajectory<TrajEntry>> trajectories){
        this.trajectories = trajectories;
    }

    public List<String> getMapVFormat(){
        return null;
    }

    public List<Trajectory<TrajEntry>> getResultTrajectory(){
        return trajectories;
    }
}
