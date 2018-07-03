package au.edu.rmit.trajectory.torch.base.model;

public class Coordinate extends TorPoint{

    public Coordinate(double lat, double lng){
        super(lat,lng);
    }

    @Override
    public int getId() {
        return id;
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
