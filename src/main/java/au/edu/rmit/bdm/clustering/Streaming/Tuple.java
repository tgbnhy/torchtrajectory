package au.edu.rmit.bdm.clustering.Streaming;

public class Tuple {
    public final int timestemp;
    public final int carId;
    public final int edgeId;

    public Tuple(int timestemp, int carId, int edgeId){
        this.timestemp = timestemp;
        this.carId = carId;
        this.edgeId = edgeId;
    }

    public static Tuple genFromCSV(String line){
        String[] tokens = line.split(",");
        return new Tuple(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
    }

    @Override
    public String toString(){
        return timestemp + "";
    }
}
