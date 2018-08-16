package au.edu.rmit.bdm.Torch.queryEngine.query;

import au.edu.rmit.bdm.Torch.base.Torch;
import au.edu.rmit.bdm.Torch.base.model.TrajEntry;
import au.edu.rmit.bdm.Torch.base.model.Trajectory;
import au.edu.rmit.bdm.Torch.queryEngine.model.TimeInterval;
import au.edu.rmit.bdm.Torch.queryEngine.visualization.Formater;
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

    public final boolean isResolved;
    public final int[] idArray;
    public final TimeInterval[] intervals;
    public final boolean mappingSucceed;
    public final String queryType;
    public final List<TrajEntry> rawQuery;
    public final List<TrajEntry> mappedQuery;
    public final List<Trajectory<TrajEntry>> resolvedRet;
    public final int retSize;
    public final String failReason;

    private QueryResult(String queryType, List<? extends TrajEntry> raw, String failReason){
        this.mappingSucceed = false;
        this.failReason = failReason;
        this.queryType = queryType;
        this.resolvedRet = new ArrayList<>();
        this.rawQuery = raw == null ? null : (List<TrajEntry>) raw;
        this.mappedQuery = null;
        retSize = 0;

        isResolved = false;
        idArray = new int[0];
        this.intervals = null;

    }

    private QueryResult(String queryType, List<Trajectory<TrajEntry>> ret, List<TrajEntry> rawQuery, List<TrajEntry> mappedQuery){
        this.mappingSucceed = true;
        failReason = null;
        this.queryType = queryType;
        this.resolvedRet = ret;
        this.rawQuery = rawQuery;
        this.mappedQuery = mappedQuery;
        retSize = ret.size();
        idArray = new int[ret.size()];
        for (int i = 0; i < ret.size(); i++)
            idArray[i] = Integer.parseInt(ret.get(i).id);
        this.intervals = null;
        isResolved = true;
    }

    private QueryResult(String queryType, int[] ids, List<TrajEntry> rawQuery, List<TrajEntry> mappedQuery){
        this.mappingSucceed = true;
        failReason = null;
        this.queryType = queryType;
        this.idArray = ids;
        this.intervals = null;
        this.rawQuery = rawQuery;
        this.mappedQuery = mappedQuery;
        resolvedRet = new ArrayList<>();
        retSize = idArray.length;
        isResolved = false;
    }

    QueryResult(List<Trajectory<TrajEntry>> ret){
        isResolved = true;
        this.mappingSucceed = false;
        failReason = null;
        this.queryType = null;
        this.idArray = null;
        this.rawQuery = null;
        this.mappedQuery = null;
        this.intervals = null;
        resolvedRet = ret;
        retSize = ret.size();
    }

    public QueryResult(String queryType, TimeInterval[] intervals, List<TrajEntry> rawQuery, List<TrajEntry> mappedQuery) {
        this.mappingSucceed = true;
        failReason = null;
        this.queryType = queryType;
        this.idArray = null;
        this.intervals = intervals;
        this.rawQuery = rawQuery;
        this.mappedQuery = mappedQuery;
        resolvedRet = new ArrayList<>();
        retSize = intervals.length;
        isResolved = false;
    }

    public static QueryResult genResolvedRet(String queryType, List<Trajectory<TrajEntry>> ret, List<TrajEntry> rawQuery, List<TrajEntry> mappedQuery){
        return new QueryResult(queryType, ret, rawQuery, mappedQuery);
    }

    public static QueryResult genUnresolvedRet(String queryType, int[] ids, List<TrajEntry> rawQuery, List<TrajEntry> mappedQuery){
        return new QueryResult(queryType, ids, rawQuery, mappedQuery);
    }

    public static QueryResult genUnresolvedRet(String queryType, TimeInterval[] intervals, List<TrajEntry> rawQuery, List<TrajEntry> mappedQuery){
        return new QueryResult(queryType, intervals, rawQuery, mappedQuery);
    }

    public static QueryResult genFailedRet(String queryType, List<? extends TrajEntry> raw, String reason){
        return new QueryResult(queryType, raw, reason);
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
        return resolvedRet;
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
     * - key: resolvedRet
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
        return Formater.toMapVJSON(rawQuery);
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
        return Formater.toMapVJSON(mappedQuery);
    }

    public String getRetMapVformat(){
        if (resolvedRet.isEmpty()) return "[]";
        return Formater.toMapVJSON(resolvedRet);
    }
}
