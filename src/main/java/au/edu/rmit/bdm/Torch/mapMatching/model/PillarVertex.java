package au.edu.rmit.bdm.Torch.mapMatching.model;

import au.edu.rmit.bdm.Torch.base.model.TorEdge;

/**
 * An pillar vertex is the vertex that is neither at the joint position of the roads, nor at the end of a road.
 * It does not contribute much to routing, but it adds accuracy for modeling the real world road rather than a straight line
 * from one end to the other.
 */
public class PillarVertex extends TorVertex {

    public TorEdge edge;
    public double baseAndthisDist = Double.MIN_VALUE;

    public PillarVertex(double lat, double lng, TorEdge edge) {
        super(lat, lng, false);
        this.edge = edge;
    }

    public static PillarVertex generateMiddle(TorVertex v1, TorVertex v2, TorEdge edge){
        double newPillarLat = (v1.lat + v2.lat)/ 2;
        double newPillarLng = (v1.lng + v2.lng)/ 2;

        return new PillarVertex(newPillarLat, newPillarLng, edge);
    }
}
