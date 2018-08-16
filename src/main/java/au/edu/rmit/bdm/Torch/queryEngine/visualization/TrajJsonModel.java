package au.edu.rmit.bdm.Torch.queryEngine.visualization;

import au.edu.rmit.bdm.Torch.base.model.TrajEntry;

import java.util.List;

class TrajJsonModel {

    Geometry geometry;

    TrajJsonModel(List<TrajEntry> path){
        geometry = new Geometry(path);
    }
}
