package au.edu.rmit.bdm.Torch.base;

import au.edu.rmit.bdm.Torch.mapMatching.MapMatching;

/**
 * Parameters used in T-Torch
 */
public interface Torch {

    String SEPARATOR_1 = ";";
    String SEPARATOR_2 = "\t";
    String TIME_SEP = " | ";

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
        String LCSS = "LCSS";                 // Longest Common Sub-Sequence
        String EDR = "EDR";                   // Edit Distance On Real Sequence
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
}
