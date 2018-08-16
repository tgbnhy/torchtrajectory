package au.edu.rmit.bdm.Torch.mapMatching.io;

import au.edu.rmit.bdm.Torch.mapMatching.MapMatching;

public class TorPipeTest {


    public static void main(String[] args){

        MapMatching pipe = MapMatching.getBuilder().build("BEIJING/beijing_raw_test.txt","map-data/Beijing.osm.pbf");
        pipe.start();

    }
}