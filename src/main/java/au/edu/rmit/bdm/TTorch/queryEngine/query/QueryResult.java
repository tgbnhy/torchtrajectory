package au.edu.rmit.bdm.TTorch.queryEngine.query;

import au.edu.rmit.bdm.TTorch.base.Torch;
import au.edu.rmit.bdm.TTorch.base.model.TrajEntry;
import au.edu.rmit.bdm.TTorch.base.model.Trajectory;
import au.edu.rmit.bdm.TTorch.queryEngine.visualization.Formater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The class bundles information you may be interested in together.
 * The QueryResult will be returned uniformly after the particular query.txt has been processed.
 */
public class QueryResult {
    private static final Logger logger = LoggerFactory.getLogger(QueryResult.class);

    public final boolean mappingSucceed;
    public final String queryType;
    public final List<TrajEntry> rawQuery;
    public final List<TrajEntry> mappedQuery;
    public final List<Trajectory<TrajEntry>> ret;
    public final int retSize;

    private QueryResult(String queryType, List<? extends TrajEntry> raw){
        this.mappingSucceed = false;
        this.queryType = queryType;
        this.ret = new ArrayList<>();
        this.rawQuery = (List<TrajEntry>) raw;
        this.mappedQuery = null;
        retSize = 0;
    }

    public static QueryResult genFailedRet(String queryType, List<? extends TrajEntry> raw){
        return new QueryResult(queryType, raw);
    }

    QueryResult(String queryType, List<Trajectory<TrajEntry>> ret, List<TrajEntry> rawQuery, List<TrajEntry> mappedQuery){
        this.mappingSucceed = true;
        this.queryType = queryType;
        this.ret = ret;
        this.rawQuery = rawQuery;
        this.mappedQuery = mappedQuery;
        retSize = ret.size();
    }

    /**
     * Get raw query.txt.
     *
     * @return raw query.txt
     *         or null if the queryType is RangeQuery
     */
    public List<TrajEntry> getRawQuery(){
        return rawQuery;
    }

    /**
     * Get mapped query.txt.
     *
     * @return mapped query.txt.
     *         or null if the query.txt is of type RangeQuery.
     *         or null if the mapping process fails.
     */
    public List<TrajEntry> getMappedQuery() {
        return mappedQuery;
    }

    /**
     * Get trajectories meeting the query.txt constraints.
     * The returned list would be empty if there is no qualified trajectory found.
     *
     * @return qualified trajectories.
     */
    public List<Trajectory<TrajEntry>> getResultTrajectory(){
        return ret;
    }

    /**
     * Get a string of JSON format<p>
     *
     * key-value map:
     *
     * - key: queryType
     * @see Torch.QueryType for possible query.txt types as value
     *
     * - key: mappingSucceed:
     * value: Boolean value indicates if the process of converting raw trajectory to map-matched trajectory succeeds.
     *
     * - key: raw
     * Query in mapV format.
     * Or null if the query.txt is of type rangeQuery
     *
     * - key: mapped
     * mapmatched query.txt in mapV format.
     * Or null if the query.txt is of type rangeQuery
     *
     * - key: retSize
     * value: integer indicates number of qualified trajectories found
     *
     * - key: ret
     * value: array of qualified trajectories in mapV format
     *
     * @return A string of JSON format
     */
    public String toJSON(int maximum){

        return Formater.toMapVJSON(this, maximum);
    }

    public String toJSON(){
        return Formater.toMapVJSON(this);
    }

    /**
     * Get raw query.txt in mapV format
     *
     * @return raw query.txt in mapV format
     *         or null if the query.txt type is RangeQuery
     */
    public String getRawQueryjMapVformat(){
        if (rawQuery == null) return null;
        return Formater.toJSON(rawQuery);
    }

    /**
     * Get mapped query.txt in mapV format
     *
     * @return mapped query.txt in mapV format
     *         or null if the query.txt type is RangeQuery
     *         or null if the raw query.txt is not properly projected.
     */
    public String getMappedQueryMapVformat(){
        if (queryType.equals(Torch.QueryType.RangeQ)) return null;
        if (!mappingSucceed) return null;
        return Formater.toJSON(mappedQuery);
    }

    public String getRetMapVformat(){
        if (ret.isEmpty()) return "[]";
        return Formater.toJSON(ret);
    }
}
