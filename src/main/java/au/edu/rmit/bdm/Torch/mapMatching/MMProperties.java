package au.edu.rmit.bdm.Torch.mapMatching;

import au.edu.rmit.bdm.Torch.base.Torch;

public class MMProperties {

    public String vehicleType = Torch.vehicleType.CAR;
    public String mmAlg = Torch.Algorithms.HMM_PRECOMPUTED;
    public String trajSrcPath;
    public String osmPath;
    public int batchSize = 10000;
    public int preComputationRange = 1000;
    public String baseDir = "Torch";

    public MMProperties(){}

    public MMProperties(MMProperties p){
        vehicleType = p.vehicleType;
        mmAlg = p.mmAlg;
        trajSrcPath = p.trajSrcPath;
        osmPath = p.osmPath;
        batchSize = p.batchSize;
        preComputationRange = p.preComputationRange;
        baseDir = p.baseDir;
    }
    public void reset() {
        vehicleType = Torch.vehicleType.CAR;
        mmAlg = Torch.Algorithms.HMM_PRECOMPUTED;
        trajSrcPath = null;
        osmPath = null;
        batchSize = 10000;
        preComputationRange = 1000;
        baseDir = "Torch";
    }
}
