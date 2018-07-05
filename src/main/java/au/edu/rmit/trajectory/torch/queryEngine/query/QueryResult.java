package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.model.Coordinate;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class QueryResult {
    private static final Logger logger = LoggerFactory.getLogger(QueryResult.class);

    private List<TrajEntry> rawQuery;
    private List<TrajEntry> mappedQuery;
    private List<Trajectory<TrajEntry>> ret;

    QueryResult(List<Trajectory<TrajEntry>> ret, List<TrajEntry> rawQuery, List<TrajEntry> mappedQuery){
        this.ret = ret;
        this.rawQuery = rawQuery;
        this.mappedQuery = mappedQuery;
    }

    public List<String> getMapVFormat(){


        Gson gson = new Gson();
        return null;
    }

    public List<Trajectory<TrajEntry>> getResultTrajectory(){
        return ret;
    }
}
