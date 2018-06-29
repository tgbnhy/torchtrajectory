package au.edu.rmit.trajectory.torch.queryEngine.model;

public class LightEdge {
    public final int id;
    public final double length;
    public final int position;

    public LightEdge(Integer id, double length, int position) {
        this.id = id;
        this.length = length;
        this.position = position;
    }
}
