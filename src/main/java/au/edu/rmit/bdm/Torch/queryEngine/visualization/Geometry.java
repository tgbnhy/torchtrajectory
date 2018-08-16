package au.edu.rmit.bdm.Torch.queryEngine.visualization;

import au.edu.rmit.bdm.Torch.base.model.TrajEntry;

import java.util.List;

class Geometry{

    String type = "LineString";

    //lng, lat
    //order is vital
    double[][] coordinates;

    Geometry(List<TrajEntry> path){

        int pathLen = path == null ? 0 : path.size();
        coordinates = new double[pathLen][2];

        for (int i = 0; i < pathLen; i++){
            coordinates[i][0] = path.get(i).getLng();
            coordinates[i][1] = path.get(i).getLat();
        }
    }
}
