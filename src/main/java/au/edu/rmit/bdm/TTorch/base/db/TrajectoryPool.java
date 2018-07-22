package au.edu.rmit.bdm.TTorch.base.db;

import au.edu.rmit.bdm.TTorch.base.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class TrajectoryPool {

    private Logger logger = LoggerFactory.getLogger(TrajVertexRepresentationPool.class);
    private boolean isMem;
    private Map<String, String[]> memPool;
    private DBManager db;

    String tableName;

    TrajectoryPool(boolean isMem){

        this.isMem = isMem;
        if (!isMem) {
            logger.info("init db version trajectory representation pool");
            db = new DBManager().connect();
            return;
        }

        logger.info("init memory version trajectory representation pool");
        memPool = new HashMap<>();

        loadFromFile((this instanceof TrajVertexRepresentationPool) ?
                Instance.fileSetting.TRAJECTORY_VERTEX_REPRESENTATION_PATH_200000 :
                Instance.fileSetting.TRAJECTORY_EDGE_REPRESENTATION_PATH_200000);
        //read meta properties
    }

    private void loadFromFile(String filePath) {
        try (FileReader fr = new FileReader(filePath);
             BufferedReader reader = new BufferedReader(fr)) {

            String line;
            String[] tokens;
            String trajId;

            while ((line = reader.readLine()) != null) {
                tokens = line.split("\t");
                trajId = tokens[0];
                memPool.put(trajId, tokens[1].split(","));
            }

        } catch (IOException e) {
            logger.error("some critical data is missing, system on exit...");
            System.exit(-1);
        }
    }

    public int[] get(String trajId){

        int[] ret;

        if (isMem){
            String[] trajectory = memPool.get(trajId);
            ret = new int[trajectory.length];
            for (int i = 0; i < ret.length; i++)
                ret[i] = Integer.valueOf(trajectory[i]);
        }else{
            ret = db.get(tableName, trajId);
        }
        return ret;
    }
}
