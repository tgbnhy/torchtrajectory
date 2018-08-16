package au.edu.rmit.bdm.Torch;

import com.github.davidmoten.geo.GeoHash;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FootFlagEncoder;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.PointList;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class GeneralTest {

    private static final Logger logger = LoggerFactory.getLogger(GeneralTest.class);
    /**
     * test if PointList fetchWayGeometry(int mode); with base node and adjacent node is consistent.
     *
     * result: fetchWayGeometry(int mode) is ordered from base node to adjacent node.
     *
     */
    @Test
    public void test2() {
        GraphHopper hopper = new GraphHopperOSM();
        hopper.setDataReaderFile("map-data/Beijing.osm.pbf");
        hopper.setGraphHopperLocation("./target/beijingmapmatchingtest");
        CarFlagEncoder encoder = new CarFlagEncoder();
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();

        Graph g = hopper.getGraphHopperStorage();
        NodeAccess accessor = g.getNodeAccess();
        EdgeExplorer explorer = g.createEdgeExplorer();

        boolean allRight = true;

        for (int i = 1; i < g.getNodes();i+=300){
            EdgeIterator iter= explorer.setBaseNode(i);
            iter.next();
            int base = iter.getBaseNode();
            int adjacent = iter.getAdjNode();
            double baseLat = accessor.getLatitude(base);
            double baseLng = accessor.getLongitude(base);
            double adjLat = accessor.getLatitude(adjacent);
            double adjLng = accessor.getLongitude(adjacent);

            PointList pl = iter.fetchWayGeometry(3);
            System.out.println("size "+ pl.getSize());
            if (pl.getLatitude(0) != baseLat ||
                    pl.getLongitude(0) != baseLng ||
                    pl.getLatitude(pl.getSize() - 1) != adjLat ||
                    pl.getLongitude(pl.getSize() - 1) != adjLng) {
                System.err.println("not equal");
                allRight = false;
            }
        }
        System.out.println("all right? "+allRight);

    }

    /**
     * test relations of base node and forward, backward of edge.
     *
     * result: - hopper.setEncodingManager(new EncodingManager(encoder)); defines what kind of edges is indexed.
     *           e.g. if carEncorder is set, only car edges are readBatch and indexed from osm file.
     *         - boolean value returned by isForward(FlagEncoder encoder), isBackward(FlagEncoder encoder)
     *           relies on the base node.
     *         - if isForward(FlagEncoder encoder), isBackward(FlagEncoder encoder) both yields true, the edge is a bidirectional way.
     *           if one yields true while the other not, the way is a single direction way.
     *           if both yields false, the way is unavailable( maybe under construct).
     */
    @Test
    public void test1() {
        GraphHopper hopper = new GraphHopperOSM();
        hopper.setDataReaderFile("map-data/Beijing.osm.pbf");
        hopper.setGraphHopperLocation("./TorchMeta/HopperMeta_car");
        CarFlagEncoder encoder = new CarFlagEncoder();
        FootFlagEncoder flagEncoder = new FootFlagEncoder();
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();

        Graph g = hopper.getGraphHopperStorage();
        EdgeExplorer explorer = g.createEdgeExplorer();
        EdgeIterator iterator = g.getAllEdges();

        int dual_direction = 0;
        int single_direction = 0;
        int illigal = 0;
        int total = 0;

        while(iterator.next()){
            total++;
            if (iterator.isBackward(flagEncoder) == iterator.isForward(flagEncoder)){
                if (!iterator.isForward(flagEncoder)){
                    System.err.println("double false. edge hash: "+ iterator.getEdge());
                    illigal++;
//                    System.out.println("isForward for foot: "+ iterator.isForward(flagEncoder));
//                    System.out.println("isBackward for foot: "+ iterator.isBackward(flagEncoder));

//                    int base = iterator.getBaseNode();
//                    int adj = iterator.getAdjNode();
//
//                    EdgeIterator temp = explorer.setBaseNode(adj);
//                    while(temp.next()){
//                        if (temp.getAdjNode() == base){
//                            System.out.println("\n");
//                            System.out.println("isForward: " + iterator.isForward(encoder));
//                            System.out.println("isBackward: " + iterator.isBackward(encoder));
//                            System.out.println("reverse");
//                            System.out.println("isForward: " + temp.isForward(encoder));
//                            System.out.println("isBackward: " + temp.isBackward(encoder));
//                        }
//                    }
                }else{
                    dual_direction++;
                }
            }else{
                single_direction++;

            }
        }

        System.out.println("single direction: "+ single_direction);
        System.out.println("dual direction: "+ dual_direction);
        System.out.println("cannot pass by car: "+ illigal);
        System.out.println("total: "+ total);
    }

    /**
     * find total number of tower points and piller points for car edges.
     *
     * total number of tower nodes: 54406
     * total number of pillar nodes: 94116
     */
    @Test
    public void test3(){
        GraphHopper hopper = new GraphHopperOSM();
        hopper.setDataReaderFile("Resources/Porto.osm.pbf");
        hopper.setGraphHopperLocation("./TorchMeta/HopperMeta_car");
        CarFlagEncoder encoder = new CarFlagEncoder();
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();

//        System.out.println("total number of tower nodes: "+hopper.getGraphHopperStorage().getNodes());
//
//        EdgeIterator iter = hopper.getGraphHopperStorage().getAllEdges();
//        int total_pillar = 0;
//        while (iter.next()){
//            total_pillar += iter.fetchWayGeometry(0).size();
//        }
//
//        System.out.println("total number of pillar nodes: "+total_pillar);
    }


    /**
     * beijing dataset
     *
     * investigate how the edge are defined in graphhopper
     *
     * 11 vertexes out of 54406 tower vertexes that are both pillar and tower.
     * means there are about 11 edges that contain tower vertex in the middle.
     */
    @Test
    public void test4(){
        GraphHopper hopper = new GraphHopperOSM();
        hopper.setDataReaderFile("map-data/Beijing.osm.pbf");
        hopper.setGraphHopperLocation("./TorchMeta/HopperMeta_car");
        CarFlagEncoder encoder = new CarFlagEncoder();
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();
        System.out.println("total number of tower nodes: "+hopper.getGraphHopperStorage().getNodes());

        Graph g = hopper.getGraphHopperStorage();
        NodeAccess nodeAccess = g.getNodeAccess();
        EdgeIterator iter = g.getAllEdges();

        Set<String> set = new HashSet<>();
        for (int i = 0; i < g.getNodes(); ++i) {
            set.add(GeoHash.encodeHash(nodeAccess.getLatitude(i), nodeAccess.getLongitude(i)));
        }
        int counter = 0;
        while(iter.next()){
            PointList l = iter.fetchWayGeometry(0);
            for (int i = 0; i < l.size(); i++){
                if (set.contains(GeoHash.encodeHash(l.getLat(i), l.getLongitude(i))))
                    counter++;
            }
        }

        System.err.println("counter: "+counter);
    }

    /**
     * beijing dataset
     * total number of tower nodes: 54406
     * total number of ways: 76412
     * there are 525 ways have the same beginning vertex and ending vertex.
     */
    @Test
    public void test6(){
        GraphHopper hopper = new GraphHopperOSM();
        hopper.setDataReaderFile("map-data/Beijing.osm.pbf");
        hopper.setGraphHopperLocation("./TorchMeta/HopperMeta_car");
        CarFlagEncoder encoder = new CarFlagEncoder();
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();
        System.out.println("total number of tower nodes: " + hopper.getGraphHopperStorage().getNodes());

        Graph g = hopper.getGraphHopperStorage();

        NodeAccess nodeAccess = g.getNodeAccess();
        EdgeIterator iter = g.getAllEdges();
        System.err.println("total number of edges " + ((AllEdgesIterator) iter).getMaxId());
        iter.next();
        PointList l = iter.fetchWayGeometry(3);
        for (int i = 0; i < l.size(); i++){
            System.err.println(l.getLatitude(i) +","+ l.getLongitude(i));
        }
        System.err.println("distance: "+iter.getDistance());
        System.err.println("is backward: "+iter.isForward(encoder));
        System.err.println("is forward: " + iter.isBackward(encoder));
//        Set<String> set = new HashSet<>();
//
//        int counter = 0;
//        int counter_total = 0;
//        while(iter.next()){
//            counter_total++;
//            PointList l = iter.fetchWayGeometry(3);
//            String hash = GeoHash.encodeHash(l.getLatitude(0),l.getLongitude(0))+GeoHash.encodeHash(l.getLatitude(l.size()-1), l.getLongitude(l.size() - 1));
//            if (set.contains(hash)) {
//                System.err.println(l.toString());
//                System.out.println("----");
//                counter++;
//            }
//            set.indexAll(hash);
//        }
//        System.err.println("total number of ways: " + counter_total);
//        System.err.println("counter: "+counter);
    }
}
