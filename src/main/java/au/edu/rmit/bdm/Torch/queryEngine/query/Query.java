package au.edu.rmit.bdm.Torch.queryEngine.query;

import au.edu.rmit.bdm.Torch.base.Index;
import au.edu.rmit.bdm.Torch.base.model.TrajEntry;

import java.util.List;

public interface Query {

    /**
     * For query of type windowQuery, param passed in should be an object of type SearchWindow as to specify the search range.<p>
     * For query of type TopKQuery, param passed in should be an object of type Integer as to specify the number of results retrieved.<p>
     * For the rest query.txt types, null value is expected.
     *
     * @param param A SearchWindow object indicates the range to search on.
     * @return A list of trajectories meets the specific query.txt requirement.
     * @throws IllegalStateException if the passed object type is not expected for an particular query.txt, exception will be thrown.
     */
    QueryResult execute(Object param);

    /**
     * If search on the map-matched trajectory set, the query trajectory will also be converted to map-matched trajectory.<p>
     * If search on the raw trajectory set, the query.txt trajectory will be unchanged.
     *
     * @param raw the query.txt trajectory
     * @return true if the query.txt trajectory mapped successfully, or no map-matching is required.
     *         false if the query.txt trajectory cannot be mapped properly.
     */
     boolean prepare(List<? extends TrajEntry> raw);

    /**
     * The API is for path query only.
     *
     * @param streetName The name of the street
     * @return True if at least one edge is found for the street.
     *         False if no edge is found for the street
     */
     boolean prepare(String streetName);

    /**
     *
     * @param idx
     */
    void updateIdx(Index idx);
}
