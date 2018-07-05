package au.edu.rmit.trajectory.torch.queryEngine.model;

import java.util.Set;

/**
 * For internal use.
 */
public interface QueryProperties {

    String getPreferedIndex();

    String getSimilarityMeasure();

    Set<String> getQueryUsed();

    boolean dataUsed();

}
