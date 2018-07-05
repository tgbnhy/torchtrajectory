package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.model.TrajEntry;

import java.util.List;

public interface Query {

    /**
     * For query of type windowQuery, param passed in should be an object of type SearchWindow as to specify the search range.<p>
     * For query of type TopKQuery, param passed in should be an object of type Integer as to specify the number of results retrieved.<p>
     * For the rest query types, null value is expected.
     *
     * @param param A SearchWindow object indicates the range to search on.
     * @return A list of trajectories meets the specific query requirement.
     * @throws IllegalStateException if the passed object type is not expected for an particular query, exception will be thrown.
     */
    QueryResult execute(Object param);

    /**
     * If search on the map-matched trajectory set, the query trajectory will also be converted to map-matched trajectory.<p>
     * If search on the raw trajectory set, the query trajectory will be unchanged.
     *
     * @param raw the query trajectory
     * @param <T> any class extends TrajEntry could be added in the list passed in.
     * @return true if the query trajectory mapped successfully, or no map-matching is required.
     *         false if the query trajectory cannot be mapped properly.
     */
    <T extends TrajEntry> boolean prepare(List<T> raw);
}
