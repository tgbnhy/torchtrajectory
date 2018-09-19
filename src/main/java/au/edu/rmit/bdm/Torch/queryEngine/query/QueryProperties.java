package au.edu.rmit.bdm.Torch.queryEngine.query;

import au.edu.rmit.bdm.Torch.base.Torch;

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
    public boolean resolveAll;
    public String baseDir = "Torch";
    public String uriPrefix = "";
    public boolean isNantong = false;

    public QueryProperties(QueryProperties properties) {
        init();
        this.similarityMeasure = properties.similarityMeasure;
        this.preferedIndex = properties.preferedIndex;
        this.resolveAll = properties.resolveAll;
        this.baseDir = properties.baseDir;
        this.uriPrefix = properties.uriPrefix;
        isNantong = properties.isNantong;

        // if user does not specify what kind of query.txt will be used,
        // we initialize all supported queries.
        this.queryUsed.addAll(properties.queryUsed);
        if (this.queryUsed.size() == 0){
            this.queryUsed.add(Torch.QueryType.TopK);
            this.queryUsed.add(Torch.QueryType.RangeQ);
            this.queryUsed.add(Torch.QueryType.PathQ);
        }
    }

    public QueryProperties init() {
        similarityMeasure = Torch.Algorithms.DTW;
        preferedIndex = Torch.Index.EDGE_INVERTED_INDEX;
        baseDir = "Torch";
        uriPrefix = "";
        useRaw = false;
        resolveAll = true;
        queryUsed = new HashSet<>();
        isNantong = false;
        return this;
    }
}
