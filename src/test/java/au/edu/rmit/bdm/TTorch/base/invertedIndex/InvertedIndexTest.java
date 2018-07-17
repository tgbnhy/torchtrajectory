package au.edu.rmit.bdm.TTorch.base.invertedIndex;

import me.lemire.integercompression.*;
import me.lemire.integercompression.differential.*;
import org.junit.Test;

import java.util.Arrays;

public class InvertedIndexTest {


    //@Test
    public void superSimpleExample(){
        IntegratedIntCompressor iic = new IntegratedIntCompressor();
        int[] data = new int[2342351];
        for(int k = 0; k < data.length; ++k)
            data[k] = k;
        System.out.println("Compressing "+data.length+" integers using friendly interface");
        int[] compressed = iic.compress(data);
        int[] recov = iic.uncompress(compressed);
        System.out.println("compressed from "+data.length*4/1024+"KB to "+compressed.length*4/1024+"KB");
        if(!Arrays.equals(recov,data)) throw new RuntimeException("bug");
    }

    //@Test
    public void basicExample() {
        int[] data = new int[2342351];
        System.out.println("Compressing "+data.length+" integers in one go");
        // data should be sorted for best
        //results
        for(int k = 0; k < data.length; ++k)
            data[k] = k;
        // Very important: the data is in sorted order!!! If not, you
        // will get very poor compression with IntegratedBinaryPacking,
        // you should use another CODEC.

        // next we compose a CODEC. Most of the processing
        // will be done with binary packing, and leftovers will
        // be processed using variable byte
        IntegratedIntegerCODEC codec =  new
                IntegratedComposition(
                new IntegratedBinaryPacking(),
                new IntegratedVariableByte());
        // output vector should be large enough...
        int [] compressed = new int[data.length+1024];
        // compressed might not be large enough in some cases
        // if you get java.lang.ArrayIndexOutOfBoundsException, try
        // allocating more memory

        /**
         *
         * compressing
         *
         */
        IntWrapper inputoffset = new IntWrapper(0);
        IntWrapper outputoffset = new IntWrapper(0);
        codec.compress(data,inputoffset,data.length,compressed,outputoffset);
        // got it!
        // inputoffset should be at data.length but outputoffset tells
        // us where we are...
        System.out.println("compressed from "+data.length*4/1024+"KB to "+outputoffset.intValue()*4/1024+"KB");
        // we can repack the data: (optional)
        compressed = Arrays.copyOf(compressed,outputoffset.intValue());

        /**
         *
         * now uncompressing
         *
         * This assumes that we otherwise know how many integers
         * have been compressed, or we can bound it (e.g., you know that
         * will never need to decore more than 2000 integers).
         * See basicExampleHeadless for a
         * more general case where you can manually manage the compressed
         * array size.
         */
        int[] recovered = new int[data.length];
        IntWrapper recoffset = new IntWrapper(0);
        codec.uncompress(compressed,new IntWrapper(0),compressed.length,recovered,recoffset);
        if(Arrays.equals(data,recovered))
            System.out.println("data is recovered without loss");
        else
            throw new RuntimeException("bug"); // could use assert
        System.out.println();
    }

    /**
     * This is an example to show you can compress unsorted integers
     * as long as most are small.
     */
    @Test
    public void unsortedExample() {
        final int N = 1333333;
        int[] data = new int[N];
        // initialize the data (most will be small
        for(int k = 0; k < N; k+=1) data[k] = 3;
        // throw some larger values
        for(int k = 0; k < N; k+=5) data[k] = 100;
        for(int k = 0; k < N; k+=533) data[k] = 10000;
        int[] compressed = new int [N+1024];// could need more
        IntegerCODEC codec =  new
                Composition(
                new FastPFOR(),
                new VariableByte());
        // compressing
        IntWrapper inputoffset = new IntWrapper(0);
        IntWrapper outputoffset = new IntWrapper(0);
        codec.compress(data,inputoffset,data.length,compressed,outputoffset);
        System.out.println("compressed unsorted integers from "+data.length*4/1024+"KB to "+outputoffset.intValue()*4/1024+"KB");
        // we can repack the data: (optional)
        compressed = Arrays.copyOf(compressed,outputoffset.intValue());

        int[] recovered = new int[N];
        IntWrapper recoffset = new IntWrapper(0);
        codec.uncompress(compressed,new IntWrapper(0),compressed.length,recovered,recoffset);
        if(Arrays.equals(data,recovered))
            System.out.println("data is recovered without loss");
        else
            throw new RuntimeException("bug"); // could use assert
        System.out.println();

    }

    @Test
    public void unsortedExample2() {
        final int N = 1333333;
        int[] data = new int[N];
        // initialize the data (most will be small
        for(int k = 0; k < N; k+=1) data[k] = 3;
        // throw some larger values
        for(int k = 0; k < N; k+=5) data[k] = 100;
        for(int k = 0; k < N; k+=533) data[k] = 10000;
        IntCompressor compressor = new IntCompressor();

        // compressing

        int[] compressed = compressor.compress(data);
        System.out.println("compressed unsorted integers from "+data.length*4/1024+"KB to "+ compressed.length*4/1024+"KB");

        int[] recovered = compressor.uncompress(compressed);

        if(Arrays.equals(data,recovered))
            System.out.println("data is recovered without loss");
        else
            throw new RuntimeException("bug"); // could use assert
        System.out.println();

    }
}