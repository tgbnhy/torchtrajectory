package au.edu.rmit.trajectory;

import au.edu.rmit.trajectory.torch.mapMatching.MapMatching;

public class Test {
    public static void main(String[] args){
        MapMatching mm = MapMatching.getBuilder().build("Resources/porto_raw_trajectory.txt","Resources/porto.osm.pbf");
        mm.start();
    }
}