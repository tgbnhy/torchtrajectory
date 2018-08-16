package au.edu.rmit.bdm.Torch.queryEngine.model;

import au.edu.rmit.bdm.Torch.base.model.Coordinate;

public class Circle implements Geometry{
    public final Coordinate center;
    public final int radius;            //meters

    public Circle(Coordinate center, int radius) {
        this.center = center;
        this.radius = radius;
    }

    //tile len is 100 meters
//    boolean isOverlap(Tile tile) {
//        double dist_lat = Math.abs(GeoUtil.distance(this.center.lat, tile.center.lat, 0, 0));
//        double dist_lng = Math.abs(GeoUtil.distance(0, 0, this.center.lng, tile.center.lng));
//
//        if (dist_lat > 50 + radius) return false;
//        if (dist_lng > 50 + radius) return false;
//
//        if (dist_lat <= 50) return true;
//        if (dist_lng <= 50) return true;
//
//        double cornerDist = Math.pow((dist_lat - 50)/2,2) + Math.pow((dist_lng - 50)/2, 2);
//        return  cornerDist <= Math.pow(radius, 2);
//    }
}
