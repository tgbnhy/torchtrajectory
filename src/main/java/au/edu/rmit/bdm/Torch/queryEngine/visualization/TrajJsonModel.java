package au.edu.rmit.bdm.Torch.queryEngine.visualization;

import au.edu.rmit.bdm.Torch.base.model.TrajEntry;

import java.util.List;

public class TrajJsonModel {

    Geometry geometry;

    public TrajJsonModel(List<TrajEntry> path){
        geometry = new Geometry(path);
    }
}
