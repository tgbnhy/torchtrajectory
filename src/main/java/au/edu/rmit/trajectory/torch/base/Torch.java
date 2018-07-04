package au.edu.rmit.trajectory.torch.base;

import au.edu.rmit.trajectory.torch.mapMatching.MapMatching;

/**
 * Parameters used in T-Torch
 */
public interface Torch {

    String SEPARATOR = ";";

    /**
     * Map-matching algorithms currently supported by T-Torch
     *
     * @see MapMatching.Builder#setMapMatchingAlgorithm(String) ;
     */
    interface Algorithms{

        /** * * * * * * * * * * * *
         * map matching algorithm *
         * * * * * * * * * * * * */

        //hidden markov model
        String HMM = "HMM1";
        String HMM_PRECOMPUTED = "HMM2";


        /** * * * * * * * * * * **
         *   similarity measure  *
         * * * * * * * * * * * * */

//        String EUCLIDEAN_DISTANCE = "ED";     // Euclidean Distance
//        String LCSS = "LCSS";                 // Longest Common Sub-Sequence
//        String ERP = "ERP";                   // Edit Distance With Real Penalty
//        String EDR = "ERP";                   // Edit Distance On Real Sequence
        String DTW = "DTW";                   // Dynamic Time Warping
        String Hausdorff = "H";
        String Frechet = "F";
    }

    interface QueryType{

        String RangeQ = "RQ";
        String PathQ = "PQ";
        String TopK = "TK";
    }

    /**
     * It is used by graph-hopper to loading the specific edges and nodes on graph
     * e.g. different graph data will be loaded from the same *.osm.pbf based on the parameter here.
     *
     * @see MapMatching.Builder#setVehicleType(String)
     */
    interface vehicleType{
        String CAR = "car";
        String FOOT = "foot";
        String BIKE = "bike";
        String MOTOCYCLE = "moto";
    }

    interface Index{
        String LEVI = "levi";
        String EDGE_INVERTED_INDEX = "eii";
    }

    /**
     * Internal use for File settings.
     */
    interface URI {
        String TORCH_META_PREFIX = "T-Torch/T-Torch";
        String HOPPER_META = "T-Torch/HopperMeta";
        String META = "T-Torch/meta";
        String ID_VERTEX_LOOKUP = TORCH_META_PREFIX +"/id_vertex.txt";
        String ID_EDGE_LOOKUP = TORCH_META_PREFIX +"/id_edge.txt";
        String ID_EDGE_RAW = TORCH_META_PREFIX +"/id_edge_raw.txt";

        String TRAJECTORY_VERTEX_REPRESENTATION_PATH = TORCH_META_PREFIX +"/trajectory_vertex";
        String TRAJECTORY_EDGE_REPRESENTATION_PATH = TORCH_META_PREFIX +"/trajectory_edge";

        String TRAJECTORY_VERTEX_REPRESENTATION_PATH_200000 = TORCH_META_PREFIX+"/trajectory_vertex_200000.txt";
        String TRAJECTORY_EDGE_REPRESENTATION_PATH_200000 = TORCH_META_PREFIX+"/trajectory_edge_200000.txt";

        String EDGE_INVERTED_INDEX = TORCH_META_PREFIX+"/invertedIndex/edgeInvertedIdx";
        String VERTEX_INVERTED_INDEX=  TORCH_META_PREFIX+"/invertedIndex/vertexInvertedIdx";

        String RAW_TRAJECTORY_INDEX = TORCH_META_PREFIX + "/raw_trajectories";

        String GRID_INDEX = TORCH_META_PREFIX + "/grid_vertex.idx";
        String RTREE_INDEX = TORCH_META_PREFIX + "/rtree_raw.idx";
    }
}
