package au.edu.rmit.trajectory.torch.base.invertedIndex;

import au.edu.rmit.trajectory.torch.base.Torch;
import org.junit.Test;

public class InvertedIndexTest {

    @Test
    public void test1(){
        EdgeInvertedIndex edgeInvertedIndex = new EdgeInvertedIndex();
        edgeInvertedIndex.build(Torch.URI.EDGE_INVERTED_INDEX);
        System.out.println(edgeInvertedIndex);
    }
}