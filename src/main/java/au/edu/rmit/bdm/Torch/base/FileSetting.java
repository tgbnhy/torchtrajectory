package au.edu.rmit.bdm.Torch.base;

import java.util.HashMap;
import java.util.Map;

/**
 * Internal use for File fileSetting.
 */
public class FileSetting {

    public static Map<String, FileSetting> map = new HashMap<>();

    public String prefix = "";
    public String hopperURI;
    public String metaURI;
    public String TorchBase;
    public String ID_VERTEX_LOOKUP;
    public String ID_EDGE_LOOKUP;
    public String ID_EDGE_RAW;

    public String EDGE_INVERTED_INDEX;
    public String VERTEX_INVERTED_INDEX;

    public String TRAJECTORY_VERTEX_REPRESENTATION_PATH;
    public String TRAJECTORY_EDGE_REPRESENTATION_PATH;

    public String TRAJECTORY_VERTEX_REPRESENTATION_PATH_PARTIAL;
    public String TRAJECTORY_EDGE_REPRESENTATION_PATH_PARTIAL;
    public String TRAJECTORY_START_END_TIME_PARTIAL;

    //for Torch_Porto.db
    public String DB_PREFIX = "jdbc:sqlite:";
    public String DB_URL;
    public String TRAJECTORY_VERTEX_TABLE = "vertex";
    public String TRAJECTORY_EDGE_TABLE = "edge";
    public String EDGENAME_ID_TABLE= "edgename";

    public String RAW_TRAJECTORY_INDEX;

    public String GRID_INDEX;
    public String RTREE_INDEX;

    public FileSetting(String baseDir){
        map.put(baseDir, this);

        hopperURI = baseDir + "/HopperMeta";
        metaURI = baseDir + "/meta";
        TorchBase = baseDir + "/Torch";
        ID_VERTEX_LOOKUP = TorchBase +"/id_vertex.txt";
        ID_EDGE_LOOKUP = TorchBase + "/id_edge.txt";
        ID_EDGE_RAW = TorchBase + "/id_edge_raw.txt";

        EDGE_INVERTED_INDEX = TorchBase + "/invertedIndex/edgeInvertedIdx";
        VERTEX_INVERTED_INDEX = TorchBase + "/invertedIndex/vertexInvertedIdx";

        TRAJECTORY_VERTEX_REPRESENTATION_PATH = TorchBase + "/trajectory_vertex.txt";
        TRAJECTORY_EDGE_REPRESENTATION_PATH = TorchBase + "/trajectory_edge.txt";

        TRAJECTORY_VERTEX_REPRESENTATION_PATH_PARTIAL = TorchBase + "/trajectory_vertex_partial.txt";
        TRAJECTORY_EDGE_REPRESENTATION_PATH_PARTIAL = TorchBase + "/trajectory_edge_partial.txt";
        TRAJECTORY_START_END_TIME_PARTIAL = TorchBase + "/trajectory_time_partial.txt";

        //for Torch_Porto.db
        DB_URL = DB_PREFIX+ TorchBase +"/db/"+ baseDir + ".db";

        RAW_TRAJECTORY_INDEX = TorchBase + "/raw_trajectories";
        GRID_INDEX = TorchBase + "/grid_vertex.idx";RTREE_INDEX = TorchBase + "/rtree_raw.idx";

        modifyForWinOS();
    }

    public void update(String prefix) {
        this.prefix = prefix;
        hopperURI = this.prefix + hopperURI;
        metaURI = this.prefix + metaURI;
        ID_VERTEX_LOOKUP = this.prefix + ID_VERTEX_LOOKUP;
        ID_EDGE_LOOKUP = this.prefix + ID_EDGE_LOOKUP;
        ID_EDGE_RAW = this.prefix + ID_EDGE_RAW;
        EDGE_INVERTED_INDEX = this.prefix + EDGE_INVERTED_INDEX;
        VERTEX_INVERTED_INDEX = this.prefix + VERTEX_INVERTED_INDEX;
        TRAJECTORY_VERTEX_REPRESENTATION_PATH = this.prefix + TRAJECTORY_VERTEX_REPRESENTATION_PATH;
        TRAJECTORY_EDGE_REPRESENTATION_PATH = this.prefix + TRAJECTORY_EDGE_REPRESENTATION_PATH;
        TRAJECTORY_VERTEX_REPRESENTATION_PATH_PARTIAL = this.prefix + TRAJECTORY_VERTEX_REPRESENTATION_PATH_PARTIAL;
        TRAJECTORY_EDGE_REPRESENTATION_PATH_PARTIAL = this.prefix + TRAJECTORY_EDGE_REPRESENTATION_PATH_PARTIAL;
        TRAJECTORY_START_END_TIME_PARTIAL = this.prefix + TRAJECTORY_START_END_TIME_PARTIAL;
        GRID_INDEX = this.prefix + GRID_INDEX;
        RTREE_INDEX = this.prefix + RTREE_INDEX;

        DB_URL = DB_PREFIX + this.prefix + DB_URL.split(":")[2];
        System.err.println(DB_URL);
        RAW_TRAJECTORY_INDEX = this.prefix + "/raw_trajectories";

        modifyForWinOS();
    }

    private void modifyForWinOS(){
        boolean isWindow = System.getProperty("os.name").contains("win");
        if (!isWindow) return;

        prefix = prefix.replace("/","\\");
        hopperURI = hopperURI.replace("/","\\");
        metaURI = metaURI.replace("/","\\");
        ID_VERTEX_LOOKUP = ID_VERTEX_LOOKUP.replace("/","\\");
        ID_EDGE_LOOKUP = ID_EDGE_LOOKUP.replace("/","\\");
        ID_EDGE_RAW = ID_EDGE_RAW.replace("/","\\");
        EDGE_INVERTED_INDEX = EDGE_INVERTED_INDEX.replace("/","\\");
        VERTEX_INVERTED_INDEX = VERTEX_INVERTED_INDEX.replace("/","\\");
        TRAJECTORY_VERTEX_REPRESENTATION_PATH = TRAJECTORY_VERTEX_REPRESENTATION_PATH.replace("/","\\");
        TRAJECTORY_EDGE_REPRESENTATION_PATH = TRAJECTORY_EDGE_REPRESENTATION_PATH.replace("/","\\");
        TRAJECTORY_VERTEX_REPRESENTATION_PATH_PARTIAL = TRAJECTORY_VERTEX_REPRESENTATION_PATH_PARTIAL.replace("/","\\");
        TRAJECTORY_EDGE_REPRESENTATION_PATH_PARTIAL = TRAJECTORY_EDGE_REPRESENTATION_PATH_PARTIAL.replace("/","\\");
        TRAJECTORY_START_END_TIME_PARTIAL = TRAJECTORY_START_END_TIME_PARTIAL.replace("/","\\");
        GRID_INDEX = GRID_INDEX.replace("/","\\");
        RTREE_INDEX = RTREE_INDEX.replace("/","\\");
        RAW_TRAJECTORY_INDEX = RAW_TRAJECTORY_INDEX.replace("/","\\");
    }

}
