package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.Torch;

import java.util.HashSet;
import java.util.Set;

/**
 * For internal use.
 */
public class QueryProperties {

    public QueryProperties(){
        init();
    }
    
    public String similarityMeasure;
    public String preferedIndex;
    public boolean useRaw;
    public Set<String> queryUsed;

    public QueryProperties(QueryProperties properties) {
        init();
        this.similarityMeasure = properties.similarityMeasure;
        this.preferedIndex = properties.preferedIndex;

        // if user does not specify what kind of query will be used,
        // we initialize all supported queries.
        this.queryUsed.addAll(properties.queryUsed);
        if (this.queryUsed.size() == 0){
            this.queryUsed.add(Torch.QueryType.TopK);
            this.queryUsed.add(Torch.QueryType.RangeQ);
            this.queryUsed.add(Torch.QueryType.PathQ);
        }
    }

    public void init() {
        similarityMeasure = Torch.Algorithms.DTW;
        preferedIndex = Torch.Index.EDGE_INVERTED_INDEX;
        useRaw = false;
        queryUsed = new HashSet<>();
    }
}
