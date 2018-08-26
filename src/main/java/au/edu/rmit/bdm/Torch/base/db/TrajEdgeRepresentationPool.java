package au.edu.rmit.bdm.Torch.base.db;

import au.edu.rmit.bdm.Torch.base.FileSetting;

public class TrajEdgeRepresentationPool extends TrajectoryPool{

    public TrajEdgeRepresentationPool(boolean isMem, FileSetting setting){
        super(isMem, setting);
        tableName = setting.TRAJECTORY_EDGE_TABLE;
    }
}
