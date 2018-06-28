package au.edu.rmit.trajectory.torch.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * a trajectory has an id and a list of nodes.
 * the nodes of the trajectory should have GPS coordinate, which is enforced by TrajEntry.
 */
public class Trajectory<T extends TrajEntry> extends LinkedList<T> {

    public int id;
    public boolean hasTime;
    public List<TorEdge> edges = new ArrayList<>();

    public Trajectory(){}
    public Trajectory(int id, boolean hasTime){
        this.hasTime = hasTime;
        this.id = id;
    }

    public Trajectory(boolean hasTime){
        this.hasTime = hasTime;
    }

    @Override
    public String toString(){

        StringBuilder builder = new StringBuilder();
        builder.append("[");

        boolean isFirst = true;

        for (T entry : this){

            if (!isFirst)
                builder.append(",");
            isFirst = false;

            builder.append("[").append(entry.getLat()).append(",").append(entry.getLng());
            if (hasTime) {builder.append(",").append(((TrajNode)entry).getTime());}
            builder.append("]");
        }

        builder.append("]");

        return builder.toString();
    }

}
