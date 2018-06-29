package au.edu.rmit.trajectory.torch;

import au.edu.rmit.trajectory.torch.mapMatching.MapMatching;

/**
 * Parameters used in T-Torch
 */
public interface Torch {

    /**
     * Map-matching algorithms currently supported by T-Torch
     *
     * @see MapMatching.Builder#setMapMatchingAlgorithm(String) ;
     */
    interface Algorithms{

        /** * * * * * * * * * * * *
         * map matching algorithm *
         * * * * * * * * * * * * */

        String HMM = "hidden_markov_model_1";
        String HMM_PRECOMPUTED = "hidden_markov_model_2";


        /** * * * * * * * * * * * *
         *  similarity algorithm  *
         * * * * * * * * * * * * */


    }

    /**
     * It is used by graph-hopper to specify what kind of graph to build.
     * For instance, different graph data will be loaded from the same *.osm.pbf based on the parameter you set.
     *
     * @see MapMatching.Builder#setVehicleType(String)
     */
    interface vehicleType{
        String CAR = "car";
        String FOOT = "foot";
        String BIKE = "bike";
        String MOTOCYCLE = "moto";
    }

    /**
     * Internal use.
     * This is for system settings.
     */
    interface Props {
        String TORCH_META_PREFIX = "T-Torch/TorchMeta";
        String HOPPER_META = "T-Torch/HopperMeta";

        String ID_VERTEX_LOOKUP = TORCH_META_PREFIX +"/id_vertex.txt";
        String ID_EDGE_LOOKUP = TORCH_META_PREFIX +"/id_edge.txt";
        String ID_EDGE_RAW = TORCH_META_PREFIX +"/id_edge_raw.txt";

        String TRAJECTORY_VERTEX_REPRESENTATION_PATH = TORCH_META_PREFIX +"/trajectory_vertex.txt";
        String TRAJECTORY_EDGE_REPRESENTATION_PATH = TORCH_META_PREFIX +"/trajectory_edge.txt";

        String EDGE_INVERTED_INDEX = TORCH_META_PREFIX+"/index/edgeInvertedIdx";
        String VERTEX_INVERTED_INDEX=  TORCH_META_PREFIX+"/index/vertexInvertedIdx";

        String SEPARATOR = ",";
        String SEPARATOR2 = ";";
    }
}
