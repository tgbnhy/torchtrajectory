package au.edu.rmit.bdm.TTorch.base.db;

import au.edu.rmit.bdm.TTorch.base.Instance;

public class TrajVertexRepresentationPool extends TrajectoryPool {

    public TrajVertexRepresentationPool(boolean isMem){
        super(isMem);
        tableName = Instance.fileSetting.TRAJECTORY_VERTEX_TABLE;
    }
}
