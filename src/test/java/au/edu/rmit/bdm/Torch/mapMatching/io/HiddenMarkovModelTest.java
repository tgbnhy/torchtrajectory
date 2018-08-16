package au.edu.rmit.bdm.Torch.mapMatching.io;

//import au.edu.rmit.bdm.Torch.mapMatching.algorithm.Mapper;
//import au.edu.rmit.bdm.Torch.base.Torch;
//import au.edu.rmit.bdm.Torch.mapMatching.algorithm.Mappers;
//import au.edu.rmit.bdm.Torch.mapMatching.model.TorVertex;
//import au.edu.rmit.bdm.Torch.base.model.TrajNode;
//import au.edu.rmit.bdm.Torch.base.model.Trajectory;
//import com.graphhopper.util.StopWatch;

public class HiddenMarkovModelTest {

//    static List<Trajectory<TrajNode>> rawTrajs;
//    static Mapper mapper;
//
////    @BeforeClass
//    public static void prepare() {
//        TrajReader torReader = new TrajReader();
//        rawTrajs = torReader.readBatch(new File("BEIJING/beijing_raw.txt"), null);
//        mapper = Mappers.getMapper("map-data/Beijing.osm.pbf",Torch.vehicle.CAR, Torch.Algorithms.HMM);
//    }
//
//
////    @Test
//    public void test1() {
//        int len = rawTrajs.size();
//        for (int i = 0; i < len; i += len / 4) {
//            Trajectory<TrajNode> rawTraj = rawTrajs.getList(i);
//            Trajectory<TorVertex> trajectory = mapper.match(rawTraj);
//            System.out.println("rawTraj: " + rawTraj.toString());
//            System.out.println("rawTraj size: " + rawTraj.size());
//            System.out.println("mapmatched traj: " + trajectory.toString());
//            System.out.println("mapmatched traj size: " + trajectory.size());
//        }
//    }
//
//    /**
//     * - graphhopper mapmatching module takes 145 seconds to process 100 trajectories
//     * - there are 10 trajectories that cannot be mapped to the graph.
//     * - normally map-matched trajectory sizes is of 7-10 times larger then raw trajectory.
//     */
//    public static void main(String[] args){
//        TrajReader torReader = new TrajReader();
//        rawTrajs = torReader.readBatch(new File("BEIJING/beijing_raw.txt"), null);
//        mapper = Mappers.getMapper("map-data/Beijing.osm.pbf", Torch.vehicle.CAR, Torch.Algorithms.HMM);
//
//        int len = rawTrajs.size();
//        System.out.println("total trajectories: "+len);
//        StopWatch watch = new StopWatch();
//        watch.start();
//        int counter = 0;
//        for (int i = 200; i < 300; i ++) {
//            Trajectory<TrajNode> rawTraj = rawTrajs.getList(i);
//            System.out.println("rawTraj size: " + rawTraj.size());
//            try {
//                Trajectory<TorVertex> trajectory = mapper.match(rawTraj);
//                System.out.println("mapmatched traj size: " + trajectory.size());
//                System.out.println();
//            }catch (Exception e){ counter++;}
//        }
//        watch.stop();
//        System.out.println("broken trajectories: "+ counter);
//        System.out.println("time for algorithm 100 trajectories: "+watch.getSeconds());
//    }
}