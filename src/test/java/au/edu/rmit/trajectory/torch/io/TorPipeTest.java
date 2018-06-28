package au.edu.rmit.trajectory.torch.io;

import au.edu.rmit.trajectory.torch.mapping.MapMatching;

public class TorPipeTest {


    public static void main(String[] args){

        MapMatching pipe = MapMatching.getBuilder().build("BEIJING/beijing_raw_test.txt","map-data/Beijing.osm.pbf");
        pipe.start();

    }
}