package au.edu.rmit.trajectory.torch;

import au.edu.rmit.trajectory.torch.model.*;

import java.util.List;

/**
 * The contract for map-matching mapping implementations
 */
public interface Mapper {

    /**
     * The API is used for converting a single raw trajectory to map-matched trajectory.
     *
     * @param in RawTrajectory to be mapped
     *
     * @return MappedTrajectory Trajectory defined by a list of vertices
     * @throws Exception If raw trajectory cannot be projected properly, Exception will be thrown.
     */
    Trajectory<TowerVertex>  match(Trajectory<? extends TrajEntry> in) throws Exception;

    /**
     * The API is used for converting a list of raw trajectories to map-matched trajectories.
     * Note that there could be case that a minority of trajectories could not be mapped properly due to various reason( osm map quality, gps error, etc.)
     *       These trajectories would be excluded during the process. As the result, in.size() may not be the same as mappedTrajectories.size(),
     *       but for those properly mapped trajectories, the id for each of these trajectories remain unchanged.
     * @param in A list of rawTrajectories to be mapped
     * @return Trajectories defined by a list of vertices
     *
     *
     */
    <T extends TrajEntry> List<Trajectory<TowerVertex>> batchMatch(List<Trajectory<T>> in);

}
