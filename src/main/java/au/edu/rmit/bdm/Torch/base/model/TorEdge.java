package au.edu.rmit.bdm.Torch.base.model;

import au.edu.rmit.bdm.Torch.base.helper.GeoUtil;
import au.edu.rmit.bdm.Torch.base.Torch;
import au.edu.rmit.bdm.Torch.mapMatching.model.PillarVertex;
import au.edu.rmit.bdm.Torch.mapMatching.model.TorVertex;
import au.edu.rmit.bdm.Torch.mapMatching.model.TowerVertex;

import java.util.*;

/**
 * An edge models a segment of the road. it is defined by tower points on the two side.
 * An edge also have information such as direction, length... as the road has.
 */
public class TorEdge{

    private static int idGenerator = 0;

    /**
     * First and last point on edge.
     */
    public TowerVertex baseVertex, adjVertex;

    /**
     * a sum of distance between each point with its adjacent points on edge.
     * metric: meters
     * */
    private double length = Double.MIN_VALUE;

    /**
     * other points on the edge.
     * */
    private List<PillarVertex> pillarVertexes = new LinkedList<>();

    public boolean isForward;

    public boolean isBackward;

    /**
     * for database
     */
    private String latitudes;

    /**
     * for database
     */
    private String longtitudes;

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * for LEVI, representing the edge position in a trajectory
     */
    private transient int position;

    /**
     * for database
     */
    public final int id;

    public TorEdge() {
        idGenerator++;
        id = idGenerator;
    }

    public TorEdge(int id){
        this.id = id;
    }

    public TorEdge(int id, TowerVertex t1, TowerVertex t2, double len){
        this.id = id;
        baseVertex = t1;
        adjVertex = t2;
        length = len;
    }

    /**
     * convert information in fields of this instance, to string.
     * Several fields( e.g. latitudes, longitudes) will be filled in this process.
     *
     * @return an instance of string that contains information that are ready to be processed and stored on disk.
     * */
    public String convertToDatabaseForm() {
        StringBuilder latStringBuilder = new StringBuilder();
        latStringBuilder.append(baseVertex.getLat());
        for (TorVertex pillarPoint : pillarVertexes) {
            latStringBuilder.append(",").append(pillarPoint.getLat());
        }
        latStringBuilder.append(",").append(adjVertex.getLat());
        this.latitudes = latStringBuilder.toString();

        StringBuilder lonStringBuilder = new StringBuilder();
        lonStringBuilder.append(baseVertex.getLng());
        for (TorVertex pillarPoint : pillarVertexes) {
            lonStringBuilder.append(",").append(pillarPoint.getLat());
        }
        lonStringBuilder.append(",").append(adjVertex.getLng());
        this.longtitudes = lonStringBuilder.toString();
        getLength();

        StringBuilder res = new StringBuilder();
        return res.append(this.id).append(Torch.SEPARATOR_1)
                .append(this.latitudes).append(Torch.SEPARATOR_1)
                .append(this.longtitudes).append(Torch.SEPARATOR_1)
                .append(this.length).append(Torch.SEPARATOR_1)
                .append(this.isForward).append(Torch.SEPARATOR_1)
                .append(this.isBackward).append(Torch.SEPARATOR_1).toString();
    }

//    /**
//     *
//     * after reading data and form an edge with the data in form of string, we need to further process it to
//     * make the edge ready to be used.
//     *
//     * */
//    public void convertFromDatabaseForm() {
//        String[] lats = this.latitudes.split(",");
//        String[] lons = this.longtitudes.split(",");
//        int last = lats.length - 1;
//        this.baseVertex = new TowerVertex(Double.parseDouble(lats[0]), Double.parseDouble(lons[0]));
//        this.adjVertex = new TowerVertex(Double.parseDouble(lats[last]), Double.parseDouble(lons[last]));
//        this.pillarVertexes.clear();
//        for (int i = 1; i < last; ++i) {
//            this.pillarVertexes.indexAll(new PillarVertex(Double.parseDouble(lats[i]), Double.parseDouble(lons[i]), this));
//        }
//
//        getLength();
//    }

    public List<PillarVertex> getPillarVertexes() {
        return this.pillarVertexes;
    }

    public void addPillarVertex(PillarVertex pVertex) {
        pVertex.edge = this;
        this.pillarVertexes.add(pVertex);
    }

    /**
     * @see #length
     * */
    public double getLength() {
        if (length == Double.MIN_VALUE) {
            TorVertex pre = baseVertex;
            length = 0.0;
            for (TorVertex pillarPoint : pillarVertexes) {
                length += GeoUtil.distance(pre, pillarPoint);
                pre = pillarPoint;
            }
            length += GeoUtil.distance(pre, adjVertex);
        }
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }


    public static String getKey(TorVertex p1, TorVertex p2) {
        return p1.hash +p2.hash;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TorEdge TorEdge = (TorEdge) o;
        boolean flag = true;
        if (pillarVertexes.size() == TorEdge.pillarVertexes.size() && pillarVertexes.size() > 0) {
            flag = pillarVertexes.get(0).equals(TorEdge.pillarVertexes.get(0));
        }
        return Double.compare(TorEdge.length, length) == 0 &&
                isForward == TorEdge.isForward &&
                isBackward == TorEdge.isBackward &&
                baseVertex.equals(TorEdge.baseVertex) &&
                adjVertex.equals(TorEdge.adjVertex) &&
                pillarVertexes.size() == TorEdge.pillarVertexes.size() &&
                flag;
    }

    @Override
    public int hashCode() {
        if (length == Double.MIN_VALUE) {
            getLength();
        }
        if (pillarVertexes.size() > 0)
            return Objects.hash(length, baseVertex, adjVertex, pillarVertexes.get(0), isForward, isBackward);
        return Objects.hash(length, baseVertex, adjVertex, isForward, isBackward);
    }

    @Override
    public String toString(){
        return String.valueOf(id);
    }
}
