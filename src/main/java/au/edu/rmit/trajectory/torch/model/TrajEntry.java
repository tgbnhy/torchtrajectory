package au.edu.rmit.trajectory.torch.model;

/**
 * Classes implemented TrajEntry could be added to Trajectory
 * @see Trajectory
 */
public interface TrajEntry {

    /**
     * @return id of that trajectory entry
     */
    String getId();

    /**
     * @return double value represents latitude of the entry
     */
    double getLat();

    /**
     * @return double value represents longitude of the entry
     */
    double getLng();
}
