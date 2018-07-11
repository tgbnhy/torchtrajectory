package au.edu.rmit.trajectory.torch.base.model;

public class Coordinate implements TrajEntry{

    public final double lat;
    public final double lng;

    public Coordinate(double lat, double lng){
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    public int getId() {
        return -1;
    }

    @Override
    public double getLat() {
        return lat;
    }

    @Override
    public double getLng() {
        return lng;
    }
}
