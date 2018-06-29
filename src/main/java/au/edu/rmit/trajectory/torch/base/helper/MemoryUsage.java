package au.edu.rmit.trajectory.torch.base.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal use.
 * For conveniently monitoring memory usage
 */
public class MemoryUsage {

    private static Logger logger = LoggerFactory.getLogger(MemoryUsage.class);
    private static long memoryUsage = 0;

    public static void start(){
        memoryUsage = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
    }

    public static void printCurrentMemUsage(String location){
        long curUsedMem = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        long _memUsed = curUsedMem-memoryUsage;     //metric byte
        double memUsed = _memUsed/ 1024. / 1024.;       //metric mega byte
        logger.debug("current memory usage {} is {}", location, memUsed);
    }
}
