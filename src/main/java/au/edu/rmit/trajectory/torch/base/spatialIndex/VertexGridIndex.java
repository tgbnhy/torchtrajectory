package au.edu.rmit.trajectory.torch.base.spatialIndex;


import au.edu.rmit.trajectory.torch.base.Index;
import au.edu.rmit.trajectory.torch.base.Torch;
import au.edu.rmit.trajectory.torch.base.helper.GeoUtil;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.mapMatching.model.TowerVertex;
import au.edu.rmit.trajectory.torch.queryEngine.model.SearchWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Vertex Grid Index( VGI) for LEVI
 * It is for indexing all the points on virtual graph into the grid.
 *
 * @author forrest0402
 */
public class VertexGridIndex extends HashMap<Integer, Collection<Integer>> implements Index {

    public boolean loaded = false;
    private static Logger logger = LoggerFactory.getLogger(VertexGridIndex.class);

    private final String INDEX_FILE_POINT = "dataStructure/ComplexRoadGridIndex.TowerVertex.idx";

    private final String INDEX_FILE_GRID_ID = "dataStructure/ComplexRoadGridIndex.hash.idx";

    private final static String SEPRATOR = ";";

    private float lowerLat = Float.MAX_VALUE, leftLng = Float.MAX_VALUE,
            upperLat = -Float.MAX_VALUE, rightLng = -Float.MAX_VALUE,
            deltaLat, deltaLon, tileLen;

    int horizontalTileNumber, verticalTileNumber;

    private Map<Integer, TowerVertex> allPointMap;
    private Map<Integer, Tile> tileInfo;

    /**
     * Attention: crossing 0 is not supported such as (-50,100) (50,-100), the answer may be incorrect
     *
     * @param allPointMap key for TorVertex hash, value for instance of TorVertex itself.
     *                    note that the points is tower points, which means not only it have virtual points on trajectory,
     *                    but all points on virtual graph).
     * @param lenOfTile  the granularity of a tile (meter)
     */
    public VertexGridIndex(Map<Integer, TowerVertex> allPointMap, float lenOfTile) {
        this.allPointMap = allPointMap;
        this.tileLen = lenOfTile;
        tileInfo = new HashMap<>();
    }
    

    /**
     * Attention: crossing 0 is not supported such as (-50,100) (50,-100), the answer may be incorrect
     */
    //todo
    @Override
    public boolean build(String path) {
        
        logger.info("build spatial vertexGridIndex");
        //if (load(path)) return true;
        
        _build();
        loaded = true;
        //saveUncompressed();
        logger.info("grid index build complete");
        return true;
    }
    
    private void _build(){

        // find bounding box for all points
        lowerLat = Float.MAX_VALUE;
        leftLng = Float.MAX_VALUE;
        upperLat = -Float.MAX_VALUE;
        rightLng = -Float.MAX_VALUE;
        Collection<TowerVertex> allTrajEntrys = allPointMap.values();

        for (TowerVertex point : allTrajEntrys) {
            if (point.lat < lowerLat) lowerLat = (float) point.lat;
            if (point.lat > upperLat) upperLat = (float) point.lat;
            if (point.lng < leftLng) leftLng = (float) point.lng;
            if (point.lng > rightLng) rightLng = (float) point.lng;
        }

        //create grid
        double horizontal_span = GeoUtil.distance(upperLat, upperLat, leftLng, rightLng);  //horizontal width of the grid
        double vertical_span = GeoUtil.distance(upperLat, lowerLat, leftLng, leftLng);    //vertical width of the grid

        this.horizontalTileNumber = (int) (horizontal_span / tileLen);
        this.verticalTileNumber = (int) (vertical_span / tileLen);
        this.deltaLat = (upperLat - lowerLat) / this.verticalTileNumber;
        this.deltaLon = (rightLng - leftLng) / this.horizontalTileNumber;

        logger.info("start to insert points, grid location: (lowerLat,leftLng)=({},{}), (upperLat,rightLng)=({},{}), (deltaLat,deltaLon,horizontalTileNumber,verticalTileNumber)=({},{},{},{})  grid size: {}*{}={}, point size: {}", lowerLat, leftLng, upperLat, rightLng, deltaLat, deltaLon, horizontalTileNumber, verticalTileNumber, this.horizontalTileNumber, this.verticalTileNumber, this.horizontalTileNumber * this.verticalTileNumber, allTrajEntrys.size());

        //key for tile id, value for point id list
        Map<Integer, Set<Integer>> grid = new HashMap<>(this.horizontalTileNumber * this.verticalTileNumber + 1);

        for (TowerVertex point : allTrajEntrys)
            insert(point, grid);

        this.putAll(grid);

        computeTileInfo();
    }

    void computeTileInfo(){

        int numTiles = horizontalTileNumber * verticalTileNumber;
        for (int i = 1; i < numTiles; i++){
            int tileId = i;
            int temp = tileId % horizontalTileNumber;
            int col = temp == 0 ? horizontalTileNumber : temp;
            int row = (tileId - col)/horizontalTileNumber;


            double upperLat = this.upperLat - deltaLat * row;
            double lowerLat = upperLat - deltaLat;

            double rightLng = this.leftLng + col * deltaLon;
            double leftLng = rightLng - deltaLon;

            Tile tile = new Tile(tileId, upperLat, lowerLat, leftLng, rightLng);
            tileInfo.put(tileId, tile);
        }
    }

    /**
     * for one point, 9 points should be insert
     *
     * @param point
     */
    private void insert(TowerVertex point, Map<Integer, Set<Integer>> grid) {

        if (point.lat < lowerLat || point.lat > upperLat || point.lng < leftLng || point.lng > rightLng) return;

        int tileId = calculateTileID(point.lat, point.lng);
        Set<Integer> pointIDSet = grid.computeIfAbsent(tileId, k -> new HashSet<>());
        pointIDSet.add(point.id);
    }

    int calculateTileID(TrajEntry p) {
        return calculateTileID(p.getLat(), p.getLng());
    }

    int calculateTileID(double lat, double lon) {
        int row = (int) Math.floor((this.upperLat - lat) / this.deltaLat);
        int col = (int) Math.ceil((lon - this.leftLng) / this.deltaLon);
        int tileID = row * horizontalTileNumber + col;
        if (tileID < 0) {
            logger.debug("gridId < 0");
            return 0;
        }
        if (tileID > this.horizontalTileNumber * this.verticalTileNumber) {
            logger.debug("gridId < horizontalTileNumber * verticalTileNumber");
            return this.horizontalTileNumber * this.verticalTileNumber;
        }


        return tileID;
    }

    /**
     * once dataStructure is built, it will be stored in the disk
     * return true if dataStructure can be loaded from disk
     * return false otherwise
     *
     * @return
     */
    private boolean load(String path) {
        File file = new File(INDEX_FILE_POINT);
        String line = null, pointLine = null;
        if (size() == 0 && file.exists()) {

            try (BufferedReader idReader = new BufferedReader(new FileReader(INDEX_FILE_GRID_ID));
                 BufferedReader pointReader = new BufferedReader(new FileReader(INDEX_FILE_POINT))) {
                line = idReader.readLine();
                String[] trajLineArray = line.split(Torch.SEPARATOR);
                this.lowerLat = Float.parseFloat(trajLineArray[0]);
                this.leftLng = Float.parseFloat(trajLineArray[1]);
                this.upperLat = Float.parseFloat(trajLineArray[2]);
                this.rightLng = Float.parseFloat(trajLineArray[3]);
                this.deltaLat = Float.parseFloat(trajLineArray[4]);
                this.deltaLon = Float.parseFloat(trajLineArray[5]);
                this.horizontalTileNumber = Integer.parseInt(trajLineArray[6]);
                this.verticalTileNumber = Integer.parseInt(trajLineArray[7]);
                this.tileLen = Float.parseFloat(trajLineArray[8]);
                logger.info("(lowerLat,leftLng)=({},{}), (upperLat,rightLng)=({},{}), (deltaLat,deltaLon,horizontalTileNumber,verticalTileNumber)=({},{},{},{})  grid size: {}*{}={}, tileLen: {}", lowerLat, leftLng, upperLat, rightLng, deltaLat, deltaLon, horizontalTileNumber, verticalTileNumber, this.horizontalTileNumber, this.verticalTileNumber, this.horizontalTileNumber * this.verticalTileNumber, tileLen);

                int process = 0;
                while ((line = idReader.readLine()) != null) {
                    if (process++ % 10000 == 0)
                        logger.info("counter: {}", process);
                    int gridID = Integer.parseInt(line);
                    pointLine = pointReader.readLine();
                    String[] pointLineArray = pointLine.split(SEPRATOR);
                    List<Integer> pointIDList = new ArrayList<>(pointLineArray.length);
                    put(gridID, pointIDList);
                    for (String aPointLineArray : pointLineArray) {
                        pointIDList.add(Integer.parseInt(aPointLineArray));
                    }
                }
                logger.info("grid size: {}", size());
                logger.info("start to build node dataStructure");

            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        }
        return false;
    }

    private void save() {

        File file = new File(INDEX_FILE_POINT);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        
        try (BufferedWriter idWriter = new BufferedWriter((new OutputStreamWriter(new FileOutputStream(INDEX_FILE_GRID_ID, false), StandardCharsets.UTF_8)));
             BufferedWriter pointWriter = new BufferedWriter((new OutputStreamWriter(new FileOutputStream(INDEX_FILE_POINT, false), StandardCharsets.UTF_8)))) {
            //first write some arguments
            idWriter.write(this.lowerLat + Torch.SEPARATOR);
            idWriter.write(this.leftLng + Torch.SEPARATOR);
            idWriter.write(this.upperLat + Torch.SEPARATOR);
            idWriter.write(this.rightLng + Torch.SEPARATOR);
            idWriter.write(this.deltaLat + Torch.SEPARATOR);
            idWriter.write(this.deltaLon + Torch.SEPARATOR);
            idWriter.write(this.horizontalTileNumber + Torch.SEPARATOR);
            idWriter.write(this.verticalTileNumber + Torch.SEPARATOR);
            idWriter.write(this.tileLen + Torch.SEPARATOR);
            idWriter.newLine();

            PriorityQueue<Integer> gridPriorityQueue = new PriorityQueue<>(Comparator.naturalOrder());
            for (Map.Entry<Integer, Collection<Integer>> gridEntry : entrySet()) {
                Collection<Integer> pointIDSet = gridEntry.getValue();
                if (pointIDSet != null) {
                    //write grid hash
                    idWriter.write(gridEntry.getKey() + "");
                    idWriter.newLine();
                    //write point hash list
                    boolean firstLinePoint = true;
                    gridPriorityQueue.addAll(pointIDSet);
                    while (!gridPriorityQueue.isEmpty()) {
                        int pointID = gridPriorityQueue.poll();
                        if (firstLinePoint)
                            firstLinePoint = false;
                        else
                            pointWriter.write(SEPRATOR);
                        pointWriter.write(pointID + "");
                    }
                    pointWriter.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Collection<Integer> pointsInWindow(SearchWindow window) {

        int pos = calculateTileID(window.middle);
        int leftUpperID = calculateTileID(window.upperLat, window.leftLng);
        int rightUpperID = calculateTileID(window.upperLat, window.rightLng);
        int leftLowerID = calculateTileID(window.lowerLat, window.leftLng);

        logger.debug("pos: {}", pos);
        logger.debug("leftUpperID: {}", leftUpperID);
        logger.debug("rightUpperID: {}", rightUpperID);
        logger.debug("leftLowerID: {}", leftLowerID);

        //getList all the vertices in the range.
        Set<Integer> vertices = new HashSet<>();

        for (int i = leftUpperID; i <= rightUpperID; ++i) {
            int id = i;
            int numRows = (leftLowerID - leftUpperID) / horizontalTileNumber + 1;

            while (numRows > 0) {

                Collection<Integer> verticesOnTile = get(id);
                if (verticesOnTile != null) {
                    vertices.addAll(verticesOnTile);
                }

                id += horizontalTileNumber;
                --numRows;
            }
        }

        //refine
        Iterator<Integer> iter = vertices.iterator();
        while (iter.hasNext()){
            Integer vertexId = iter.next();
            TowerVertex point = allPointMap.get(vertexId);
            if (point.lng > window.rightLng ||
                    point.lng < window.leftLng||
                    point.lat > window.upperLat ||
                    point.lat < window.lowerLat)
                iter.remove();
        }

        return vertices;
    }

    public double findBound(TrajEntry queryPoint, int round) {

        int tileId = calculateTileID(queryPoint);
        Tile t = tileInfo.get(tileId);
        double dist2nearestEdge = t.dist2nearestEdge(queryPoint);
        double radius = dist2nearestEdge + tileLen * round;

        return -radius;
    }

    void incrementallyFind(TrajEntry point, int round, Set<Integer> vertices) {

        int tileID = calculateTileID(point);

        if (round == 0){
            vertices.addAll(get(tileID));
            return;
        }

        int upperLeftPos = tileID;
        int upperRightPos = tileID;
        int lowerLeftPos = tileID;
        int lowerRightPos = tileID;

        while (round-- > 0) {
            upperLeftPos = computeUpperLeft(upperLeftPos);
            upperRightPos = computeUpperRight(upperRightPos);
            lowerLeftPos = computeLowerLeft(lowerLeftPos);
            lowerRightPos = computeLowerRight(lowerRightPos);
        }

        for (int i = upperLeftPos; i < upperRightPos; ++i) {
            Collection<Integer> idList = get(i);
            if (idList != null)
                vertices.addAll(idList);
        }
        for (int i = upperRightPos; i < lowerRightPos; i += this.horizontalTileNumber) {
            Collection<Integer> idList = get(i);
            if (idList != null)
                vertices.addAll(idList);
        }
        for (int i = lowerRightPos; i < lowerLeftPos; --i) {
            Collection<Integer> idList = get(i);
            if (idList != null)
                vertices.addAll(idList);
        }
        for (int i = lowerLeftPos; i < upperLeftPos; i -= this.horizontalTileNumber) {
            Collection<Integer> idList = get(i);
            if (idList != null)
                vertices.addAll(idList);
        }
    }

    private int computeUpperLeft(int pos) {
        if (pos % this.horizontalTileNumber == 0) {
            if (pos < this.horizontalTileNumber) return pos;
            int ans = pos - this.horizontalTileNumber;
            return ans;
        } else {
            if (pos < this.horizontalTileNumber) return pos - 1;
            int ans = pos - this.horizontalTileNumber - 1;
            return ans;
        }
    }

    private int computeUpperRight(int pos) {
        if (pos % (this.horizontalTileNumber - 1) == 0) {
            if (pos < this.horizontalTileNumber) return pos;
            int ans = pos - this.horizontalTileNumber;
            return ans;
        } else {
            if (pos < this.horizontalTileNumber) return pos + 1;
            int ans = pos - this.horizontalTileNumber + 1;
            return ans;
        }
    }

    private int computeLowerLeft(int pos) {
        if (pos % this.horizontalTileNumber == 0) {
            if (pos >= this.horizontalTileNumber * (this.verticalTileNumber - 1)) return pos;
            int ans = pos + this.horizontalTileNumber;
            return ans;
        } else {
            if (pos >= this.horizontalTileNumber * (this.verticalTileNumber - 1)) return pos - 1;
            int ans = pos + this.horizontalTileNumber - 1;
            return ans;
        }
    }

    private int computeLowerRight(int pos) {
        if (pos % (this.horizontalTileNumber - 1) == 0) {
            if (pos >= this.horizontalTileNumber * (this.verticalTileNumber - 1)) return pos;
            int ans = pos + this.horizontalTileNumber;
            return ans;
        } else {
            if (pos >= this.horizontalTileNumber * (this.verticalTileNumber - 1)) return pos + 1;
            int ans = pos + this.horizontalTileNumber + 1;
            return ans;
        }
    }

    class Tile{
        int id;
        double upperLat;
        double lowerLat;
        double leftLng;
        double rightLng;

        Tile(int id , double upperLat, double lowerLat, double leftLng, double rightLng){
            this.id = id;
            this.upperLat = upperLat;
            this.lowerLat = lowerLat;
            this.leftLng = leftLng;
            this.rightLng = rightLng;
        }

        double dist2nearestEdge(TrajEntry p){
            double lat = p.getLat();
            double lng = p.getLng();

            double dist2left = GeoUtil.distance(0,0,lng, leftLng);
            double dist2Right = GeoUtil.distance(0, 0,lng, rightLng);
            double dist2Ceil = GeoUtil.distance(upperLat,lat, 0, 0);
            double dist2floor = GeoUtil.distance(lat, lowerLat, 0,0);


            return Math.min(Math.min(dist2Ceil, dist2floor), Math.min(dist2left, dist2Right));
        }

        @Override
        public int hashCode(){
            return id;
        }

        @Override
        public boolean equals(Object o){
            if (!(o instanceof Tile)) return false;
            return o.hashCode() == this.hashCode();
        }

    }
}
