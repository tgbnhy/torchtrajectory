package au.edu.rmit.trajectory.torch.base.persistance;

import au.edu.rmit.trajectory.torch.base.model.Coordinate;
import au.edu.rmit.trajectory.torch.base.model.TorPoint;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.QueueLong;

import java.util.*;

import static org.mapdb.QueueLong.Node.*;

public class TrajectoryMap{

    private DB db;
    private BTreeMap<String, String> map;
    private static final int COMMIT_FREQUENCY = 999999;
    private int counter = 0;

    public TrajectoryMap(String path){
        db = DBMaker.fileDB(path).make();
        map = db.treeMap("0", SERIALIZER.STRING, SERIALIZER.STRING).createOrOpen();
    }

    public <T extends TrajEntry> void save(Trajectory<T> t){
        map.put(t.id, t.toString());
        if (++counter == COMMIT_FREQUENCY){
            counter = 0;
            db.commit();
        }
    }

    public <T extends TrajEntry> void saveAll(List<Trajectory<T>> list){
        for (Trajectory<T> t : list){
            save(t);
        }
    }

    public Trajectory<TrajEntry> get(String id){
        return Trajectory.generate(id, db.get(id));
    }


    public List<Trajectory<TrajEntry>> getList(Collection<String> idSet) {
        List<Trajectory<TrajEntry>> ret = new ArrayList<>(idSet.size());
        for (String id : idSet){
            ret.add(get(id));
        }
        return ret;
    }

    public Map<String, Trajectory<TrajEntry>> getMap(Collection<String> idSet) {
        Map<String, Trajectory<TrajEntry>> ret = new HashMap(idSet.size());
        for (String id : idSet){
            ret.put(id, get(id));
        }
        return ret;
    }

    public void cleanUp(){
        db.commit();
        db.close();
    }

}
