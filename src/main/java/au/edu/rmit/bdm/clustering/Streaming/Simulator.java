package au.edu.rmit.bdm.clustering.Streaming;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class Simulator {

    ConcurrentLinkedDeque<Tuple> cachedList;
    private BufferedReader reader;
    private int window;
    private AtomicInteger cur;
    private int startFrom;
    private Tuple preT;
    private int speedupFactor;

    /**
     * (startFrom+1) should be dividable by speedupFactor
     */
    Simulator(int startFrom, int window, int speedupFactor) throws IOException {
        this.speedupFactor = speedupFactor;
        cachedList = new ConcurrentLinkedDeque<>();
        reader = new BufferedReader(new FileReader("time_car_edge.txt"));
        this.window = window;
        cur = new AtomicInteger(0);

        //preload the list
        String line;
        while ((line = reader.readLine()) !=null){
            Tuple t = Tuple.genFromCSV(line);
            if (t.timestemp >= startFrom){
                preT = t;
                break;
            }else{
                cachedList.addLast(t);
            }
        }

        this.startFrom = startFrom / speedupFactor;
        cur.set(this.startFrom);
    }

    public void start(){
        ExecutorService thread = Executors.newSingleThreadExecutor();
        thread.execute(()->{
            boolean isEmpty = false;
            long startTime = System.currentTimeMillis();

            while (!isEmpty){

                while ((System.currentTimeMillis() - startTime) / 1000 < cur.intValue() - startFrom + 1){
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    isEmpty = update();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                cur.incrementAndGet();
                System.out.println(cachedList.size());
            }
            thread.shutdown();
            try {
                thread.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean update() throws IOException {

        if (cur.intValue()*speedupFactor - window + 1 > 0) {
            int discardTo = cur.intValue()*speedupFactor - window;
            while (cachedList.getFirst()!=null && cachedList.getFirst().timestemp <= discardTo) {
                cachedList.removeFirst();
            }
        }

        if (preT != null)
            cachedList.addLast(preT);

        String line ;
        int counter = 0;
        while (true) {
            counter++;
            if ((line = reader.readLine()) == null)
                return true;

            Tuple t = Tuple.genFromCSV(line);
            if (t.timestemp > cur.intValue() * speedupFactor) {
                preT = t;
                System.err.println(counter);
                return false;
            } else {
                cachedList.addLast(t);

            }
        }
    }
}
