package au.edu.rmit.trajectory;

import au.edu.rmit.trajectory.torch.base.Torch;
import au.edu.rmit.trajectory.torch.mapMatching.MapMatching;
import au.edu.rmit.trajectory.torch.queryEngine.Engine;

public class Test {
    public static void main(String[] args){
//        MapMatching mm = MapMatching.getBuilder().build("Resources/porto_raw_trajectory.txt","Resources/porto.osm.pbf");
//        mm.start();

//        Engine engine = Engine.Builder.getBuilder().
//                addQuery(Torch.QueryType.PathQ).
//                preferedSimilarityMeasure(Torch.Algorithms.DTW).
//                build();
        System.out.println(1);
    }
}
