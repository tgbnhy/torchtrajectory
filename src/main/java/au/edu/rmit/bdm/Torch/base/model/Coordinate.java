package au.edu.rmit.bdm.Torch.base.model;

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
    public double getLat(){
        return lat;
    }

    @Override
    public double getLng(){
        return lng;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return hashCode() == o.hashCode();
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(this.lat);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lng);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "{" + this.lat + ", " + lng + '}';
    }
}
