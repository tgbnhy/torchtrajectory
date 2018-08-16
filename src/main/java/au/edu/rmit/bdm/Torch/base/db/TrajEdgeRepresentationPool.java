package au.edu.rmit.bdm.Torch.base.db;

import au.edu.rmit.bdm.Torch.base.Instance;

public class TrajEdgeRepresentationPool extends TrajectoryPool{

    public TrajEdgeRepresentationPool(boolean isMem){
        super(isMem);
        tableName = Instance.fileSetting.TRAJECTORY_EDGE_TABLE;
    }
}
