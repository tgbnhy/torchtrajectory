package au.edu.rmit.bdm.Torch.base.model;

import au.edu.rmit.bdm.Torch.base.helper.GeoUtil;

public class Tile{
    public final int id;
    public final Coordinate center;
    public final double upperLat;
    public final double lowerLat;
    public final double leftLng;
    public final double rightLng;

    public Tile(int id , double upperLat, double lowerLat, double leftLng, double rightLng){
        this.id = id;
        this.upperLat = upperLat;
        this.lowerLat = lowerLat;
        this.leftLng = leftLng;
        this.rightLng = rightLng;
        center = new Coordinate((upperLat+lowerLat)/2, (leftLng+rightLng)/2);
    }

    public double dist2nearestEdge(TrajEntry p){
        double lat = p.getLat();
        double lng = p.getLng();

        double dist2left = GeoUtil.distance(0,0,lng, leftLng);
        double dist2Right = GeoUtil.distance(0, 0,lng, rightLng);
        double dist2Ceil = GeoUtil.distance(upperLat,lat, 0, 0);
        double dist2floor = GeoUtil.distance(lat, lowerLat, 0,0);


        return Math.min(Math.min(dist2Ceil, dist2floor), Math.min(dist2left, dist2Right));
    }

    @Override
    public int hashCode(){
        return id;
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof Tile)) return false;
        return o.hashCode() == this.hashCode();
    }

}
