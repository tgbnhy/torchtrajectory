package au.edu.rmit.bdm.Torch.base.db;

import au.edu.rmit.bdm.Torch.base.Instance;

public class TrajVertexRepresentationPool extends TrajectoryPool {

    public TrajVertexRepresentationPool(boolean isMem){
        super(isMem);
        tableName = Instance.fileSetting.TRAJECTORY_VERTEX_TABLE;
    }
}
