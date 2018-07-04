package au.edu.rmit.trajectory.torch.base.model;

import au.edu.rmit.trajectory.torch.base.model.TorPoint;

public class LightPoint extends TorPoint {

    public LightPoint(int id, double lat, double lng){
        super(id, lat, lng);
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
