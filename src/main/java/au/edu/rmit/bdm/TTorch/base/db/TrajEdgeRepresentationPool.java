package au.edu.rmit.bdm.TTorch.base.db;

import au.edu.rmit.bdm.TTorch.base.Instance;

import java.util.HashMap;
import java.util.Map;

public class TrajEdgeRepresentationPool extends TrajectoryPool{

    public TrajEdgeRepresentationPool(boolean isMem){
        super(isMem);
        tableName = Instance.fileSetting.TRAJECTORY_EDGE_TABLE;
    }
}
