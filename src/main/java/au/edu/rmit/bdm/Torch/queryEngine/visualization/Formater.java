package au.edu.rmit.bdm.Torch.queryEngine.visualization;

import au.edu.rmit.bdm.Torch.base.model.TrajEntry;
import au.edu.rmit.bdm.Torch.base.model.Trajectory;
import au.edu.rmit.bdm.Torch.queryEngine.query.QueryResult;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class Formater {

    private static final Logger logger = LoggerFactory.getLogger(Formater.class);
    final static Gson gson = new Gson();

    public static String toMapVJSON(QueryResult queryResult, int maximum){

        QueryRetJsonModel queryRetJsonModel = new QueryRetJsonModel(queryResult, maximum);
        return gson.toJson(queryRetJsonModel);
    }

    public static String toMapVJSON(QueryResult queryResult){

        QueryRetJsonModel queryRetJsonModel = new QueryRetJsonModel(queryResult);
        return gson.toJson(queryRetJsonModel);
    }

    public static String toMapVJSON(Collection<Trajectory<TrajEntry>> trajs){

        return gson.toJson(model(trajs));
    }

    public static String toMapVJSON(List<TrajEntry> traj){
        return gson.toJson(new TrajJsonModel(traj));
    }

    static List<TrajJsonModel> model(Collection<Trajectory<TrajEntry>> trajs){
        List<TrajJsonModel> l = new ArrayList<>();
        for (Trajectory<TrajEntry> traj : trajs){
            l.add(new TrajJsonModel(traj));
        }
        return l;
    }
}
