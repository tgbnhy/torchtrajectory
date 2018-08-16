package au.edu.rmit.bdm.Torch.mapMatching.model;

import java.util.*;

/**
 * An tower vertex is the vertex that is either at the joint position of the roads, or at the end of a road.
 * It is used for routing on graph and defining edges.
 */
public class TowerVertex extends TorVertex {

    // the id is aligned to graph-hopper tower node id
    public final int id;

    //only tower points have adjacent points
    private Map<TowerVertex, Double> adjacentTowerVertices = new HashMap<>();

    public TowerVertex(double lat, double lng, int id) {
        super(lat, lng, true);
        this.id = id;
    }



    public Iterator<TowerVertex> adjIterator() {
        return adjacentTowerVertices.keySet().iterator();
    }

    public void addAdjPoint(TowerVertex vertex, double dist) {
        if (!this.adjacentTowerVertices.containsKey(vertex) && !vertex.equals(this)) {
            this.adjacentTowerVertices.put(vertex, dist);
        }
    }

    public double getAdjDistance(TowerVertex vertex) {
        return this.adjacentTowerVertices.get(vertex);
    }

    @Override
    public String toString(){
        return "{ "+ String.valueOf(id)+": " + lat + ", " + lng + '}';
    }

    @Override
    public int getId() {
        return id;
    }
}
