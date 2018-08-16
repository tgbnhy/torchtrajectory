package au.edu.rmit.bdm.Torch.queryEngine.query;

import au.edu.rmit.bdm.Torch.base.model.TrajEntry;
import au.edu.rmit.bdm.Torch.base.model.Trajectory;
import au.edu.rmit.bdm.Torch.mapMatching.algorithm.Mapper;

import java.util.List;

abstract class QueryImpl implements Query{
    protected List<TrajEntry> raw;
    protected Trajectory<TrajEntry> mapped;
    protected Mapper mapper;
    protected TrajectoryResolver resolver;

    protected QueryImpl(Mapper mapper, TrajectoryResolver resolver){
        this.mapper = mapper;
        this.resolver = resolver;
    }

    @Override
    public boolean prepare(List<? extends TrajEntry> raw) {
        this.raw = (List<TrajEntry>)raw;
        Trajectory<TrajEntry> t = new Trajectory<>();
        t.addAll(raw);

        try {
            mapped = (Trajectory<TrajEntry>)(Object)mapper.match(t);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean prepare(String streetName){
        throw new RuntimeException("The method is not intended to be invoked by the query");
    }
}
