package au.edu.rmit.trajectory.torch.queryEngine.model;

import java.util.Set;

/**
 * For internal use.
 */
public interface QueryProperties {

    String preferedIndex();

    String similarityMeasure();

    Set<String> queryUsed();

    boolean dataUsed();

}
