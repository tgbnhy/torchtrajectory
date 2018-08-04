package au.edu.rmit.bdm.TTorch.queryEngine;

import au.edu.rmit.bdm.TTorch.base.Instance;
import au.edu.rmit.bdm.TTorch.base.Torch;
import au.edu.rmit.bdm.TTorch.base.model.Coordinate;
import au.edu.rmit.bdm.TTorch.base.model.TrajEntry;
import au.edu.rmit.bdm.TTorch.queryEngine.query.*;
import au.edu.rmit.bdm.TTorch.queryEngine.model.SearchWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Engine class contains high level APIs to query on trajectory data-set
 */
public class Engine {
    private QueryPool pool;
    private static final Logger logger = LoggerFactory.getLogger(Engine.class);

    private Engine(QueryProperties props){
        pool = new QueryPool(props);
    }

    /**
     * API for finding top-k most similar trajectories with the given query.<p>
     *
     * The subroutine will first map convert trajectory to map-matched trajectory,
     * which the similarity search algorithm performed on. If it can not be converted,
     * {@code QueryResult} indicates error with be returned.
     * @see QueryResult#mappingSucceed
     *
     * @param raw A list of points representing the query.
     *            T-Torch provides your simple class {@code Coordinate}
     *            But you can use any class type which implements TrajEntry interface.
     *            only longitude and latitude is required.
     *
     * @param k number of results to be returned.
     * @return qualified trajectories modeled by QueryResult
     */
    public QueryResult findTopK(List<? extends TrajEntry> raw, int k){

        Query topK = pool.get(Torch.QueryType.TopK);
        if (!topK.prepare(raw))
            return QueryResult.genFailedRet(Torch.QueryType.TopK, raw, "Cannot map raw trajectory properly");
        return topK.execute(k);
    }

    /**
     *  API for loosen path query.<p>
     *
     *  Given a raw trajectory as query, the subroutine will first map convert trajectory to map-matched trajectory,
     *  and then find all the trajectories in data-set that at least has a same edge( road segment) with the query trajectory.
     *  Same as previous, If the raw query can not be map-matched, {@code QueryResult} will indicates error with be returned.
     *
     * @param raw A list of points representing the query.
     *            T-Torch provides your simple class {@code Coordinate}
     *            But you can use any class type which implements TrajEntry interface.
     *            only longitude and latitude is required.
     * @return qualified trajectories modeled by QueryResult
     */
    public QueryResult findOnPath(List<? extends TrajEntry> raw){
        Query pathQ = pool.get(Torch.QueryType.PathQ);
        if(!pathQ.prepare(raw))
            return QueryResult.genFailedRet(Torch.QueryType.PathQ, raw, "");
        return pathQ.execute(false);
    }

    /**
     *  API for loosen path query.<p>
     *
     *  Given street name, the subroutine will first find the edges that consists of the street
     *  and then find all the trajectories in data-set that at least has a same edge( road segment) with the query trajectory.
     *  Same as previous, If the raw query can not be map-matched, {@code QueryResult} will indicates error with be returned.
     *
     * @param streetName Name of the street.
     * @return qualified trajectories modeled by QueryResult
     */
    public QueryResult findOnPath(String streetName){
        Query pathQ = pool.get(Torch.QueryType.PathQ);
        if(!pathQ.prepare(streetName))
            return QueryResult.genFailedRet(Torch.QueryType.PathQ, null,"Cannot find the street of name "+streetName);
        return pathQ.execute(false);
    }

    /**
     *  API for strict path query.<p>
     *
     *  Given a raw trajectory as query, the subroutine will first map convert trajectory to map-matched trajectory,
     *  and then find all the trajectories in data-set that contain all same edges( road segment) with the query trajectory.
     *  Same as previous, If the raw query can not be map-matched, {@code QueryResult} will indicates error with be returned.
     *
     * @param raw A list of points representing the query.
     *            T-Torch provides your simple class {@code Coordinate}
     *            But you can use any class type which implements TrajEntry interface.
     *            only longitude and latitude is required.
     * @return qualified trajectories modeled by QueryResult
     */
    public QueryResult findOnStrictPath(List<? extends TrajEntry> raw){
        Query strictPathQ = pool.get(Torch.QueryType.PathQ);
        if (!strictPathQ.prepare(raw))
            return QueryResult.genFailedRet(Torch.QueryType.PathQ, raw,"Cannot map raw trajectory properly");
        return strictPathQ.execute(true);
    }

    /**
     *  API for strict path query.<p>
     *
     *  Given a street name as query, the subroutine will first find the edges that consists of the street
     *  and then find all the trajectories in data-set that contain all same edges( road segment) with the query trajectory.
     *  Same as previous, If the raw query can not be map-matched, {@code QueryResult} will indicates error with be returned.
     *
     * @param streetName Name of the street
     * @return Qualified trajectories modeled by QueryResult
     */
    public QueryResult findOnStrictPath(String streetName){
        Query strictPathQ = pool.get(Torch.QueryType.PathQ);
        if (!strictPathQ.prepare(streetName))
            return QueryResult.genFailedRet(Torch.QueryType.PathQ, null,"Cannot find the street of name "+streetName);
        return strictPathQ.execute(true);
    }

    public static Builder getBuilder(){
        return Builder.builder;
    }

    /**
     * API for finding all trajectories passing the given range.<p>
     *
     * Given a square region, the subroutine will find all the trajectories from data-set that across it.
     *
     * @param window a square region modeled by {@code SearchWindow}
     * @return qualified trajectories modeled by QueryResult
     */
    public QueryResult findInRange(SearchWindow window){

        Query rangeQ = pool.get(Torch.QueryType.RangeQ);
        return rangeQ.execute(window);
    }

    public QueryResult findInRange(double lat, double lng, double squareRadius ){

        Query rangeQ = pool.get(Torch.QueryType.RangeQ);
        return rangeQ.execute(new SearchWindow(new Coordinate(lat, lng), squareRadius));
    }

    /**
     * Update the indexes, similarity function used in query process<p>
     *
     * Examples:
     * If you want to replace similarity function Torch.Algorithms.DTW to Torch.Algorithms.Hausdorff
     * put ("simFunc", {@code Torch.Algorithms.Hausdorff}) in.<p>
     * If you want to replace index from LEVI to EdgeInvertedIndex,
     * put ("index", {@code Torch.Index.EDGE_INVERTED_INDEX}) in.
     *
     *
     * @param props Key-value pairs indicate stuff to update
     */
    public void update(String queryType, Map<String, String> props){
        pool.update(queryType, props);
    }

    public QueryResult resolve(int[] idArr){
        return pool.resolve(idArr);
    }

    public static class Builder{

        private static Builder builder = new Builder();
        QueryProperties properties = new QueryProperties().init();

        private Builder(){}

        /**
         * For top K query, call the API to specify what similarity function to use.
         * The default similarity measure is Dynamic Time Wrapping.
         *
         * @param similarityMeasure similarityMeasure to use for Top K retrieval
         * @see Torch.Algorithms for currently supported similarity measure.
         */
        public Builder preferedSimilarityMeasure(String similarityMeasure){

            if (!similarityMeasure.equals(Torch.Algorithms.DTW) &&
                    !similarityMeasure.equals(Torch.Algorithms.Hausdorff) &&
                    !similarityMeasure.equals(Torch.Algorithms.Frechet) &&
                    !similarityMeasure.equals(Torch.Algorithms.LCSS) &&
                    !similarityMeasure.equals(Torch.Algorithms.EDR))

                throw new IllegalStateException("checkout supported index type options at Torch.Algorithms");

            properties.similarityMeasure = similarityMeasure;
            return this;
        }

        /**
         * For some queryType, there are more than one indexes capable to support it.
         * For example, TopK similarity search could be performed over Edge or Vertex,
         * and the corresponding indexes are {@code EdgeInvertedIndex} and {@code LEVI}
         *
         * @param index the index you prefer over others.
         * @see Torch.Index for currently implemented indexes.
         */
        public Builder preferedIndex(String index){
            if (!index.equals(Torch.Index.EDGE_INVERTED_INDEX)&&
                    !index.equals(Torch.Index.LEVI))
                throw new IllegalStateException("checkout supported index type options at Torch.Index");
            properties.preferedIndex = index;
            return this;
        }

        /**
         * Inform search engine the specific query used.<p>
         *
         * If no specified queries is found. The engine will load all of the data and indexes to support all kinds of queries,
         * which is more expensive than build certain data to support the specified queries.
         *
         * @param queryType The query to be prepared.
         * @see Torch.QueryType for valid options.
         */
        public Builder addQuery(String queryType){
            if (!queryType.equals(Torch.QueryType.PathQ) &&
                    !queryType.equals(Torch.QueryType.RangeQ) &&
                    !queryType.equals(Torch.QueryType.TopK))
                throw new IllegalStateException("checkout supported query type options at Torch.QueryType");
            properties.queryUsed.add(queryType);
            return this;
        }

        /**
         * Specify the path to T-Torch data.
         * This is only required if you manually move the T-Torch folder away from its origin position<p>
         *
         * For instance, if the project structure looks like this:
         * - project_name
         *   - foo
         *     - bar
         *       - T-Torch
         * Just pass "foo/bar" as the parameter.
         *
         * @param baseURI
         * @return
         */
        public Builder baseURI(String baseURI){
            if (baseURI.charAt(baseURI.length() - 1) != '/')
                baseURI += "/";

            Instance.fileSetting.update(baseURI);

            return this;
        }

        public Builder resolveResult(boolean resolve){
            properties.resolveAll = resolve;
            return this;
        }


        /**
         * Method to instantiate Engine object.
         * @return object of type Engine
         */
        public Engine build(){
            return new Engine(new QueryProperties(properties));
        }

        /**
         * todo support top K over raw trajectory data-set.
         * T-Torch use map-matched trajectories as default data-set.
         * However you could tell it to perform queries over raw trajectories by calling this method.
         */
//        public Builder useRawData(){
//            useRaw = true;
//            return this;
//        }
    }
}
