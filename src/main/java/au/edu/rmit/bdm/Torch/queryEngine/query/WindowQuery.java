package au.edu.rmit.bdm.Torch.queryEngine.query;

import au.edu.rmit.bdm.Torch.base.Index;
import au.edu.rmit.bdm.Torch.base.Torch;
import au.edu.rmit.bdm.Torch.base.WindowQueryIndex;
import au.edu.rmit.bdm.Torch.base.model.TrajEntry;
import au.edu.rmit.bdm.Torch.queryEngine.model.SearchWindow;

import java.util.List;

class WindowQuery extends QueryImpl {

    private WindowQueryIndex index;

    WindowQuery(WindowQueryIndex index, TrajectoryResolver resolver){
        super(null, resolver);
        this.index = index;
    }

    @Override
    public QueryResult execute(Object windowRange) {
        if (!(windowRange instanceof SearchWindow))
            throw new IllegalStateException(
                    "parameter passed to windowQuery should be of type SearchWindow, " +
                    "which indicates the range to search within");

        SearchWindow window = (SearchWindow) windowRange;
        List<String> trajIds = index.findInRange(window);
        return resolver.resolve(Torch.QueryType.RangeQ, trajIds, null, null);
    }

    @Override
    public boolean prepare(List<? extends TrajEntry> raw) {
        return true;
    }

    @Override
    public void updateIdx(Index idx) {
        if (!(idx instanceof WindowQueryIndex))
            throw new IllegalStateException("the index do not support windowQuery");

        index = (WindowQueryIndex) idx;
    }
}
