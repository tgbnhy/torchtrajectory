package au.edu.rmit.bdm.TTorch.queryEngine.visualization;

import au.edu.rmit.bdm.TTorch.base.model.TrajEntry;

import java.util.List;

class TrajJsonModel {

    Geometry geometry;

    TrajJsonModel(List<TrajEntry> path){
        geometry = new Geometry(path);
    }
}
