package au.edu.rmit.bdm.TTorch.mapMatching.io;

import au.edu.rmit.bdm.TTorch.mapMatching.MapMatching;

public class TorPipeTest {


    public static void main(String[] args){

        MapMatching pipe = MapMatching.getBuilder().build("BEIJING/beijing_raw_test.txt","map-data/Beijing.osm.pbf");
        pipe.start();

    }
}