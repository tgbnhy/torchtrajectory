package au.edu.rmit.bdm;

import au.edu.rmit.bdm.TTorch.base.Instance;
import au.edu.rmit.bdm.TTorch.base.Torch;
import au.edu.rmit.bdm.TTorch.base.db.DBManager;
import au.edu.rmit.bdm.TTorch.base.helper.MemoryUsage;
import au.edu.rmit.bdm.TTorch.base.invertedIndex.EdgeInvertedIndex;
import au.edu.rmit.bdm.TTorch.base.invertedIndex.VertexInvertedIndex;
import au.edu.rmit.bdm.TTorch.base.model.*;
import au.edu.rmit.bdm.TTorch.mapMatching.model.TowerVertex;
import au.edu.rmit.bdm.TTorch.queryEngine.Engine;
import au.edu.rmit.bdm.TTorch.queryEngine.query.QueryResult;
import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class Test {

    static Logger logger = LoggerFactory.getLogger(Test.class);
    public static void main(String[] args)throws IOException{
//        MapMatching mm = MapMatching.getBuilder().build("Resources/porto_raw_trajectory.txt","Resources/porto.osm.pbf");
//        mm.start();

        List<List<TrajEntry>> queries = read();
        Engine engine = Engine.getBuilder().resolveResult(false).preferedIndex(Torch.Index.LEVI).preferedSimilarityMeasure(Torch.Algorithms.EDR).build();
        QueryResult ret = engine.findTopK(queries.get(1), 5);
        System.out.println(ret.mappingSucceed);
        QueryResult r = engine.resolve(ret.idArray);
        System.out.println(r.retSize);
//
//        streetNameLookup();
//        getAfew();
//        genEdgeInvertedIndex();
//        genVertexInvertedIndex();
//        addLenToEdgeLookuptable();
//        initGH();
    }

    private static void buildStreetNameLookupDBfromFile() throws IOException {
        Map<String, String> lookup = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(Instance.fileSetting.ID_EDGE_RAW));
        String line;

        while((line = reader.readLine())!=null){
            String[] tokens = line.split(";", -1);
            int lastIdx = tokens.length - 1;
            String name = tokens[lastIdx];
            String id = tokens[0];

            if (name.length() == 0) continue;
            lookup.merge(name, id, (a, b) -> a + "," + b);
        }

        DBManager db= DBManager.getDB();
        db.buildTable(Instance.fileSetting.EDGENAME_ID_TABLE, true);
        for (Map.Entry<String, String> entry: lookup.entrySet())
            db.insert(Instance.fileSetting.EDGENAME_ID_TABLE, entry.getKey(), entry.getValue());

        db.closeConn();
    }

    private static void streetNameLookup() throws IOException {
        Map<String, String> nameIdLookup = new HashMap<>();

        BufferedReader reader = new BufferedReader(new FileReader(Instance.fileSetting.ID_EDGE_RAW));
        String line;
        while((line = reader.readLine())!=null){
            String[] tokens = line.split(";", -1);
            String id = tokens[0];
            String name = tokens[tokens.length - 1];
            if (name.length()!=0)
                nameIdLookup.put(name, id);
            if (name.equals("Largo 5 de Outubro"))
                System.out.println(id);
        }


        System.out.println(nameIdLookup.get("Largo 5 de Outubro"));

    }

    private static void toDB(){
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("cannot load mysql driver");
            System.exit(1);
        }

    }

    private static List<List<TrajEntry>> read() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("T-Torch/query.txt"));
        List<List<TrajEntry>> list = new ArrayList<>(3);

        String line;
        while((line = reader.readLine())!=null){

            String[] temp = line.split("\t");
            String trajId = temp[0];
            String trajContent = temp[1];

            trajContent = trajContent.substring(2, trajContent.length() - 2); //remove head "[[" and tail "]]"
            String[] trajTuples = trajContent.split("],\\[");
            List<TrajEntry> query = new ArrayList<>();

            String[] latLng;
            for (int i = 0; i < trajTuples.length; i++){

                double lat = 0.;
                double lon = 0.;

                    latLng = trajTuples[i].split(",");
                    lat = Double.parseDouble(latLng[1]);
                    lon = Double.parseDouble(latLng[0]);

                Coordinate node = new Coordinate(lat, lon);

                query.add(node);
            }
            list.add(query);
        }
        return list;
    }

    private static void addLenToEdgeLookuptable() throws IOException{
        BufferedReader edgeReader = new BufferedReader(new FileReader(Instance.fileSetting.ID_EDGE_LOOKUP));
        BufferedReader rawReader = new BufferedReader(new FileReader(Instance.fileSetting.ID_EDGE_RAW));

        List<String> edges = new ArrayList<>(10000);
        String line;
        String raw[];
        double dist;
        while((line = edgeReader.readLine())!=null){
            raw = rawReader.readLine().split(";");
            dist = Double.parseDouble(raw[raw.length - 3]);
            edges.add(line + ";" + dist);
        }

        edgeReader.close();
        rawReader.close();

        BufferedWriter writer = new BufferedWriter(new FileWriter(Instance.fileSetting.ID_EDGE_LOOKUP));
        for (String edge : edges){

            writer.write(edge);
            writer.newLine();
        }

        writer.flush();
        writer.close();
    }

    private static void getAfew() throws IOException {

        BufferedReader edgeReader = new BufferedReader(new FileReader(Instance.fileSetting.TRAJECTORY_EDGE_REPRESENTATION_PATH+".txt"));
        BufferedReader vertexReader = new BufferedReader(new FileReader(Instance.fileSetting.TRAJECTORY_VERTEX_REPRESENTATION_PATH+".txt"));

        List<String> edgeList = new ArrayList<>(200001);
        List<String> vertexList = new ArrayList<>(200001);
        String line1, line2;
        int i = 0;
        while((line1 = edgeReader.readLine()) != null){
            edgeList.add(line1);
            line2 = vertexReader.readLine();
            vertexList.add(line2);
            if (++i % 200000 == 0){
                break;
            }
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(Instance.fileSetting.TRAJECTORY_VERTEX_REPRESENTATION_PATH+"_"+200000+".txt"));
        for (String line : vertexList){
            writer.write(line);
            writer.newLine();
        }
        writer.flush();
        writer.close();

        writer = new BufferedWriter(new FileWriter(Instance.fileSetting.TRAJECTORY_EDGE_REPRESENTATION_PATH+"_"+200000+".txt"));
        for (String line:edgeList){
            writer.write(line);
            writer.newLine();
        }
        writer.flush();
        writer.close();
    }

    private static void genEdgeInvertedIndex() throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(Instance.fileSetting.TRAJECTORY_EDGE_REPRESENTATION_PATH_200000));
        EdgeInvertedIndex edgeInvertedIndex = new EdgeInvertedIndex();

        String line;
        String[] tokens;
        String[] edges;

        MemoryUsage.start();

        int i = 0;
        while((line = bufferedReader.readLine()) != null){

            if (++i % 100000 == 0){
                System.err.println("current progress: "+i);
                MemoryUsage.printCurrentMemUsage("");
                if (i == 200000) break;
            }
            tokens = line.split("\t");
            edges = tokens[1].split(",");

            Trajectory<TrajEntry> t = new Trajectory<>();
            t.id = tokens[0];

            for (String edge : edges)
                t.edges.add(new TorEdge(Integer.parseInt(edge), null, null, 0));


            edgeInvertedIndex.index(t);
        }
        MemoryUsage.printCurrentMemUsage("");

        edgeInvertedIndex.saveCompressed(Instance.fileSetting.EDGE_INVERTED_INDEX);
    }

    private static void genVertexInvertedIndex() throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(Instance.fileSetting.TRAJECTORY_VERTEX_REPRESENTATION_PATH_200000));
        VertexInvertedIndex vertexInvertedIndex= new VertexInvertedIndex();

        String line;
        String[] tokens;
        String[] vertices;

        MemoryUsage.start();

        int i = 0;
        while((line = bufferedReader.readLine()) != null){

            if (++i % 100000 == 0){
                System.err.println("current progress: "+i);
                MemoryUsage.printCurrentMemUsage("");
                if (i == 200000) break;
            }
            tokens = line.split("\t");
            vertices = tokens[1].split(",");

            Trajectory<TrajEntry> t = new Trajectory<>();
            t.id = tokens[0];

            for (String vertex : vertices)
               t.add(new TowerVertex(0,0, Integer.valueOf(vertex)));


            vertexInvertedIndex.index(t);
        }
        MemoryUsage.printCurrentMemUsage("");

        vertexInvertedIndex.saveCompressed(Instance.fileSetting.VERTEX_INVERTED_INDEX);
    }

    private static void initGH(){
        GraphHopper hopper = new GraphHopperOSM();
        hopper.setDataReaderFile("Resources/Porto.osm.pbf");
        hopper.setGraphHopperLocation(Instance.fileSetting.hopperURI);
        FlagEncoder vehicle = new CarFlagEncoder();
        hopper.setEncodingManager(new EncodingManager(vehicle));
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();
    }
}
