package au.edu.rmit.trajectory.torch.queryEngine;

import au.edu.rmit.trajectory.torch.base.Torch;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.queryEngine.query.QueryResult;
import au.edu.rmit.trajectory.torch.queryEngine.model.SearchWindow;
import au.edu.rmit.trajectory.torch.queryEngine.query.QueryProperties;
import au.edu.rmit.trajectory.torch.queryEngine.query.Query;
import au.edu.rmit.trajectory.torch.queryEngine.query.QueryPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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
     * Method for finding top-k most similar trajectories with the given query.
     * Subroutine will first mapping
     *
     * @param raw A list of points representing the query.
     *            T-Torch provides your simple class {@code Coordinate}
     *            But you can use any class type which implements TrajEntry interface.
     *            only longitude and latitude is required.
     *
     * @param k number of results to be returned.
     * @return results modeled by QueryResult
     */
    public QueryResult findTopK(List<? extends TrajEntry> raw, int k){

        Query topK = pool.get(Torch.QueryType.TopK);
        if (!topK.prepare(raw))
            return new QueryResult(false);
        return topK.execute(k);
    }

    /**
     * path query could only performs on map-matched data-set.
     *
     * @param raw
     * @return
     */
    public QueryResult findOnPath(List<? extends TrajEntry> raw){
        Query pathQ = pool.get(Torch.QueryType.PathQ);
        if(!pathQ.prepare(raw))
            return new QueryResult(false);
        return pathQ.execute(false);
    }

    /**
     * Strict path query could only performs on map-matched data-set.
     *
     * @param raw
     * @return
     */
    public QueryResult findOnStrictPath(List<? extends TrajEntry> raw){
        Query strictPathQ = pool.get(Torch.QueryType.PathQ);
        if (!strictPathQ.prepare(raw))
            return new QueryResult(false);
        return strictPathQ.execute(true);
    }

    /**
     * Range query could either be performed over map-matched or raw data-set
     *
     * @param window
     * @return
     */
    public QueryResult findInRange(SearchWindow window){

        Query rangeQ = pool.get(Torch.QueryType.RangeQ);
        return rangeQ.execute(window);
    }

    public static class Builder{

        private static Builder builder = new Builder();
        QueryProperties properties = new QueryProperties();

        private Builder(){}

        public static Builder getBuilder(){
            return builder;
        }

        public Builder preferedSimilarityMeasure(String similarityMeasure){
            if (!similarityMeasure.equals(Torch.Algorithms.DTW) &&
                    !similarityMeasure.equals(Torch.Algorithms.Hausdorff) &&
                    !similarityMeasure.equals(Torch.Algorithms.Frechet))
                throw new IllegalStateException("checkout supported index type options at Torch.Algorithms");

            properties.similarityMeasure = similarityMeasure;
            return this;
        }

        public Builder preferedIndex(String index){
            if (!index.equals(Torch.Index.EDGE_INVERTED_INDEX)&&
                    !index.equals(Torch.Index.LEVI))
                throw new IllegalStateException("checkout supported index type options at Torch.Index");
            properties.preferedIndex = index;
            return this;
        }

        /**
         * T-Torch use map-matched trajectories as default data-set.
         * However you could tell it to perform queries over raw trajectories by calling this method.
         */
//        public Builder useRawData(){
//            useRaw = true;
//            return this;
//        }

        /**
         * It is recommended to set this param.
         * <p>
         * The method tells engine what kind of query will be performed at application runtime.
         * If no specified queries is found. The engine will build all of the data to support all kinds of queries,
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
         * Method to instantiate Engine object.
         * <p>
         * @return object of type Engine
         */
        public Engine build(){
            return new Engine(new QueryProperties(properties));
        }
    }
}
