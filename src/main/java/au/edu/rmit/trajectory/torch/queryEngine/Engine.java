package au.edu.rmit.trajectory.torch.queryEngine;

import au.edu.rmit.trajectory.torch.base.Torch;
import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.queryEngine.query.QueryResult;
import au.edu.rmit.trajectory.torch.queryEngine.model.SearchWindow;
import au.edu.rmit.trajectory.torch.queryEngine.model.QueryProperties;
import au.edu.rmit.trajectory.torch.queryEngine.query.Query;
import au.edu.rmit.trajectory.torch.queryEngine.query.QueryPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Engine {
    private QueryPool pool;
    private static final Logger logger = LoggerFactory.getLogger(Engine.class);

    private Engine(QueryProperties props){
        pool = new QueryPool(props);
    }

    /**
     * top K similarity search could either be performed over map-matched or raw data-set
     *
     * @param raw
     * @return
     */
    public QueryResult findTopK(List<? extends TrajEntry> raw, int k){

        Query topK = pool.get(Torch.QueryType.TopK);
        topK.prepare(raw);
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
        pathQ.prepare(raw);
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
        strictPathQ.prepare(raw);
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

    public static class Builder implements QueryProperties {

        private static Builder builder = new Builder();
        private String similarityMeasure = Torch.Algorithms.DTW;
        private String preferedIndex = Torch.Index.EDGE_INVERTED_INDEX;
        private boolean useRaw = false;
        private Set<String> queryUsed = new HashSet<>();

        private Builder(){}
        private Builder(Builder builder){
            this.similarityMeasure = builder.similarityMeasure;
            this.preferedIndex = builder.preferedIndex;

            // if user does not specify what kind of query will be used,
            // we initialize all supported queries.
            queryUsed.addAll(builder.queryUsed);
            if (queryUsed.size() == 0){
                queryUsed.add(Torch.QueryType.TopK);
                queryUsed.add(Torch.QueryType.RangeQ);
                queryUsed.add(Torch.QueryType.PathQ);
            }
        }

        public static Builder getBuilder(){
            return builder;
        }

        public Builder preferedSimilarityMeasure(String similarityMeasure){
            if (!similarityMeasure.equals(Torch.Algorithms.DTW) &&
                    !similarityMeasure.equals(Torch.Algorithms.Hausdorff) &&
                    !similarityMeasure.equals(Torch.Algorithms.Frechet))
                throw new IllegalStateException("checkout supported index type options at Torch.Algorithms");

            this.similarityMeasure = similarityMeasure;
            return this;
        }

        public Builder setPreferedIndex(String index){
            if (!index.equals(Torch.Index.EDGE_INVERTED_INDEX)&&
                    !index.equals(Torch.Index.LEVI))
                throw new IllegalStateException("checkout supported index type options at Torch.Index");
            preferedIndex = index;
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
            queryUsed.add(queryType);
            return this;
        }

        /**
         * Method to instantiate Engine object.
         * <p>
         * @return object of type Engine
         */
        public Engine build(){
            QueryProperties props = new Builder(builder);
            return new Engine(props);
        }



        @Override
        public String similarityMeasure() {
            return similarityMeasure;
        }

        @Override
        public boolean dataUsed() {
            return useRaw;
        }

        @Override
        public String preferedIndex() {
            return preferedIndex;
        }


        @Override
        public Set<String> queryUsed() {
            return queryUsed;
        }
    }
}
