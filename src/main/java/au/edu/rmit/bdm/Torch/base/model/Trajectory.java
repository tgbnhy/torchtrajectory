package au.edu.rmit.bdm.Torch.base.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * a trajectory has an id and a list of nodes.
 * the nodes of the trajectory should have GPS coordinate, which is enforced by TrajEntry.
 */
public class Trajectory<T extends TrajEntry> extends LinkedList<T> {

    public String id;
    public boolean hasTime;
    public List<TorEdge> edges = new ArrayList<>();

    public Trajectory(){}
    public Trajectory(String id, boolean hasTime){
        this.hasTime = hasTime;
        this.id = id;
    }

    public Trajectory(boolean hasTime){
        this.hasTime = hasTime;
    }


    public static Trajectory<TrajEntry> generate(String id, String trajContent){

        Trajectory<TrajEntry> trajectory = new Trajectory<>(id, false);

        trajContent = trajContent.substring(2, trajContent.length() - 2); //remove head "[[" and tail "]]"
        String[] trajTuples = trajContent.split("],\\[");
        String[] latLng;

        for (int i = 0; i < trajTuples.length; i++){
            latLng = trajTuples[i].split(",");
            Coordinate coordinate = new Coordinate(Double.parseDouble(latLng[0]), Double.parseDouble(latLng[1]));
            trajectory.add(coordinate);
        }

        return trajectory;
    }

//    //real lat, lng representation of trajectory
//    @Override
//    public String toString(){
//
//        StringBuilder builder = new StringBuilder();
//        builder.append("[");
//
//        boolean isFirst = true;
//
//        for (T entry : this){
//
//            if (!isFirst)
//                builder.append(",");
//            isFirst = false;
//
//            builder.append("[").append(entry.getLat()).append(",").append(entry.getLng());
//            if (hasTime) {builder.append(",").append(((TrajNode)entry).getTime());}
//            builder.append("]");
//        }
//
//        builder.append("]");
//
//        return builder.toString();
//    }

}
