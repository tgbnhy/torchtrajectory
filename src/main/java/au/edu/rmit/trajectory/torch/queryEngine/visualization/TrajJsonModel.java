package au.edu.rmit.trajectory.torch.queryEngine.visualization;

import au.edu.rmit.trajectory.torch.base.model.TrajEntry;

import java.util.List;

class TrajJsonModel {

    Geometry geometry;

    TrajJsonModel(List<TrajEntry> path){
        geometry = new Geometry(path);
    }
}
