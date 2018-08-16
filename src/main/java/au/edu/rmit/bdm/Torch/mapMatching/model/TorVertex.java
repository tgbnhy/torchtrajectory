package au.edu.rmit.bdm.Torch.mapMatching.model;

import au.edu.rmit.bdm.Torch.base.model.Coordinate;
import com.github.davidmoten.geo.GeoHash;

/**
 * TorVertex class models a position on the graph.
 * Following Graph-hopper's naming convention, an vertex can be either an tower vertex or an pillar vertex.
 * Refer to TowerVertex and PillarVertex for more information.
 *
 * @see TowerVertex
 * @see PillarVertex
 */
public abstract class TorVertex extends Coordinate {

    public final String hash;
    public final boolean isTower;

    public TorVertex(double lat, double lng, boolean isTower) {
        super(lat, lng);
        this.isTower = isTower;
        this.hash = GeoHash.encodeHash(lat, lng);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return o.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(lat);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lng);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

}
