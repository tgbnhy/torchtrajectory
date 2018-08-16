package au.edu.rmit.bdm.Torch.base;

/**
 * Internal use for File fileSetting.
 */
public enum Instance {
    fileSetting;

    public String baseURI = "";
    public String hopperURI = "T-Torch/HopperMeta";
    public String metaURI = "T-Torch/meta";
    public String TorchPrefixURI = "T-Torch/T-Torch";
    public String ID_VERTEX_LOOKUP = TorchPrefixURI+"/id_vertex.txt";
    public String ID_EDGE_LOOKUP = TorchPrefixURI + "/id_edge.txt";
    public String ID_EDGE_RAW = TorchPrefixURI + "/id_edge_raw.txt";

    public String EDGE_INVERTED_INDEX = TorchPrefixURI + "/invertedIndex/edgeInvertedIdx";
    public String VERTEX_INVERTED_INDEX = TorchPrefixURI + "/invertedIndex/vertexInvertedIdx";

    public String TRAJECTORY_VERTEX_REPRESENTATION_PATH = TorchPrefixURI + "/trajectory_vertex";
    public String TRAJECTORY_EDGE_REPRESENTATION_PATH = TorchPrefixURI + "/trajectory_edge";

    public String TRAJECTORY_VERTEX_REPRESENTATION_PATH_200000 = TorchPrefixURI + "/trajectory_vertex_200000.txt";
    public String TRAJECTORY_EDGE_REPRESENTATION_PATH_200000 = TorchPrefixURI + "/trajectory_edge_200000.txt";
    public String TRAJECTORY_START_END_TIME_200000 = TorchPrefixURI + "/trajectory_time_200000.txt";

    //for db
    public String DB_PREFIX = "jdbc:sqlite:";
    public String DB_URL = DB_PREFIX+TorchPrefixURI+"/db/db";
    public String TRAJECTORY_VERTEX_TABLE = "vertex";
    public String TRAJECTORY_EDGE_TABLE = "edge";
    public String EDGENAME_ID_TABLE= "edgename";

    public String RAW_TRAJECTORY_INDEX = TorchPrefixURI + "/raw_trajectories";

    public String GRID_INDEX = TorchPrefixURI + "/grid_vertex.idx";
    public String RTREE_INDEX = TorchPrefixURI + "/rtree_raw.idx";

    public void update(String _baseURI) {
        baseURI = _baseURI;
        hopperURI = baseURI + hopperURI;
        metaURI = baseURI + metaURI;
        ID_VERTEX_LOOKUP = baseURI + ID_VERTEX_LOOKUP;
        ID_EDGE_LOOKUP = baseURI + ID_EDGE_LOOKUP;
        ID_EDGE_RAW = baseURI + ID_EDGE_RAW;
        EDGE_INVERTED_INDEX = baseURI + EDGE_INVERTED_INDEX;
        VERTEX_INVERTED_INDEX = baseURI + VERTEX_INVERTED_INDEX;
        TRAJECTORY_VERTEX_REPRESENTATION_PATH = baseURI + TRAJECTORY_VERTEX_REPRESENTATION_PATH;
        TRAJECTORY_EDGE_REPRESENTATION_PATH = baseURI + TRAJECTORY_EDGE_REPRESENTATION_PATH;
        TRAJECTORY_VERTEX_REPRESENTATION_PATH_200000 = baseURI + TRAJECTORY_VERTEX_REPRESENTATION_PATH_200000;
        TRAJECTORY_EDGE_REPRESENTATION_PATH_200000 = baseURI + TRAJECTORY_EDGE_REPRESENTATION_PATH_200000;
        TRAJECTORY_START_END_TIME_200000 = baseURI + TRAJECTORY_START_END_TIME_200000;
        GRID_INDEX = baseURI + GRID_INDEX;
        RTREE_INDEX = baseURI + RTREE_INDEX;

        DB_URL = DB_PREFIX + baseURI + DB_URL.split(":")[2];

        RAW_TRAJECTORY_INDEX = baseURI + "/raw_trajectories";
    }
}
