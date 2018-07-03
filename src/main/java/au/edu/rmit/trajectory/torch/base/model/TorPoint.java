package au.edu.rmit.trajectory.torch.base.model;

public abstract class TorPoint implements TrajEntry{

    public final int id;
    public final double lat;
    public final double lng;

    protected TorPoint(int id, double lat, double lng){
        this.id = id;
        this.lat = lat;
        this.lng = lng;
    }

    protected TorPoint(double lat, double lng){
        id = 0;
        this.lat = lat;
        this.lng = lng;
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
