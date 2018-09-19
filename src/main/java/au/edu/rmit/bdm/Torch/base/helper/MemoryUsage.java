package au.edu.rmit.bdm.Torch.base.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal use.
 * For conveniently monitoring memory usage
 */
public class MemoryUsage {

    private static Logger logger = LoggerFactory.getLogger(MemoryUsage.class);
    private static long memoryUsage = 0;
    private static boolean debug = true;

    public static void start(){
        if (!debug) return;
        memoryUsage = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
    }

    public static void printCurrentMemUsage(String location){
        if (!debug) return;

        long curUsedMem = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        long _memUsed = curUsedMem-memoryUsage;     //metric byte
        double memUsed = _memUsed/ 1024. / 1024.;       //metric mega byte
        logger.debug("current memory usage {} is {}", location, memUsed);
    }
}
