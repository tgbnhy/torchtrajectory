package au.edu.rmit.trajectory.torch.queryEngine.model;

import au.edu.rmit.trajectory.torch.mapMatching.model.TorEdge;

import java.util.ArrayList;
import java.util.List;

public class LightEdge {
    public final int id;
    public final double length;
    public final int position;

    public LightEdge(Integer id, double length, int position) {
        this.id = id;
        this.length = length;
        this.position = position;
    }

    public static List<LightEdge> copy(List<TorEdge> edges){
        List<LightEdge> l = new ArrayList<>(edges.size());
        for (TorEdge edge : edges)
            l.add(copy(edge));
        return l;
    }

    public static LightEdge copy(TorEdge edge){
        return new LightEdge(edge.id, edge.getLength(), edge.getPosition());
    }

}
