package au.edu.rmit.trajectory.torch.base;

/**
 * Internal use for File fileSetting.
 */
public enum Instance {
    fileSetting;
    
    public String baseURI = "";
    public String hopperURI = baseURI+"T-Torch/HopperMeta";
    public String metaURI = baseURI+"T-Torch/meta";
    public String TorchPrefixURI = baseURI+"T-Torch/T-Torch";
    public String ID_VERTEX_LOOKUP = TorchPrefixURI +"/id_vertex.txt";
    public String ID_EDGE_LOOKUP = TorchPrefixURI +"/id_edge.txt";
    public String ID_EDGE_RAW = TorchPrefixURI +"/id_edge_raw.txt";

    public String EDGE_INVERTED_INDEX = TorchPrefixURI+"/invertedIndex/edgeInvertedIdx";
    public String VERTEX_INVERTED_INDEX=  TorchPrefixURI+"/invertedIndex/vertexInvertedIdx";

    public String TRAJECTORY_VERTEX_REPRESENTATION_PATH = TorchPrefixURI +"/trajectory_vertex";
    public String TRAJECTORY_EDGE_REPRESENTATION_PATH = TorchPrefixURI +"/trajectory_edge";

    public String TRAJECTORY_VERTEX_REPRESENTATION_PATH_200000 = TorchPrefixURI+"/trajectory_vertex_200000.txt";
    public String TRAJECTORY_EDGE_REPRESENTATION_PATH_200000 = TorchPrefixURI+"/trajectory_edge_200000.txt";

    public String RAW_TRAJECTORY_INDEX = TorchPrefixURI + "/raw_trajectories";

    public String GRID_INDEX = TorchPrefixURI + "/grid_vertex.idx";
    public  String RTREE_INDEX = TorchPrefixURI + "/rtree_raw.idx";
}
