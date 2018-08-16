package au.edu.rmit.bdm.Torch.queryEngine.model;

public class TimeInterval {
    public String id;
    public TorchDate start;
    public TorchDate end;

    public TimeInterval(TorchDate start, TorchDate end){
        this.start = start;
        this.end = end;
    }

    public TimeInterval(String id, TorchDate start, TorchDate end){
        this.id = id;
        this.start = start;
        this.end = end;
    }

    public boolean contains (TimeInterval interval){
        return start.getTimeInMilliSec() < interval.start.getTimeInMilliSec() &&
                end.getTimeInMilliSec() > interval.end.getTimeInMilliSec();
    }

    public boolean joins(TimeInterval interval){
        if (start.getTimeInMilliSec() > interval.start.getTimeInMilliSec() &&
                start.getTimeInMilliSec() < interval. end.getTimeInMilliSec())
             return true;
        return end.getTimeInMilliSec() > interval.start.getTimeInMilliSec() &&
                end.getTimeInMilliSec() < interval.end.getTimeInMilliSec();

    }

}
