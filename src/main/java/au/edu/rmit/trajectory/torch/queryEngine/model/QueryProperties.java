package au.edu.rmit.trajectory.torch.queryEngine.model;

import java.util.Set;

/**
 * For internal use.
 */
public abstract class QueryProperties {

    public abstract String getSimilarityMeasure();

    public abstract Set<String> queryUsed();

    public abstract boolean dataUsed();
}
