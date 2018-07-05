package au.edu.rmit.trajectory.torch.queryEngine.query;

import au.edu.rmit.trajectory.torch.base.model.TrajEntry;
import au.edu.rmit.trajectory.torch.base.model.Trajectory;
import au.edu.rmit.trajectory.torch.mapMatching.algorithm.Mapper;

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
}
