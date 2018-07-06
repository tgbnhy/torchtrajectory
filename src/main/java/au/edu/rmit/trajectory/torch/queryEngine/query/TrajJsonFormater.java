package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

abstract class TrajJsonFormater {
    private static  final Logger logger = LoggerFactory.getLogger(TrajJsonFormater.class);
    static Gson gson = new Gson();

    static String toJSON(List<Trajectory<TrajEntry>> retTrajs, List<TrajEntry> raw, List<TrajEntry> mapped){

        List<TrajJsonModel> models = new ArrayList<>(retTrajs.size()+2);

        models.add(new TrajJsonModel(raw, "grey"));
        models.add(new TrajJsonModel(mapped, "red"));
        //models.addAll(getModels(retTrajs));

        return gson.toJson(models);
    }

    static String toJSON(List<Trajectory<TrajEntry>> retTrajs){

        return gson.toJson(getModels(retTrajs));
    }

    private static List<TrajJsonModel> getModels(List<Trajectory<TrajEntry>> retTrajs){
        List<TrajJsonModel> l = new ArrayList<>();
        for (Trajectory<TrajEntry> traj : retTrajs){
            l.add(new TrajJsonModel(traj));
        }
        return l;
    }

    private static class TrajJsonModel {

        TrajJsonModel(List<TrajEntry> path){
            geometry = new Geometry(path);
        }

        TrajJsonModel(List<TrajEntry> path, String color){
            this(path);
            this.fillStyle = color;
        }

        Geometry geometry;
        String fillStyle = "blue";
    }

    private static class Geometry{

        String type = "LineString";

        //lng, lat
        //order is vital
        double[][] coordinates;

        Geometry(List<TrajEntry> path){

            int pathLen = path.size();
            coordinates = new double[pathLen][2];

            for (int i = 0; i < pathLen; i++){
                coordinates[i][0] = path.get(i).getLng();
                coordinates[i][1] = path.get(i).getLat();
            }
        }

    }
}
