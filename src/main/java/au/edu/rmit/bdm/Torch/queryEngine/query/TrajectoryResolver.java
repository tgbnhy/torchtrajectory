package au.edu.rmit.bdm.Torch.queryEngine.query;

import au.edu.rmit.bdm.Torch.base.FileSetting;
import au.edu.rmit.bdm.Torch.base.Torch;
import au.edu.rmit.bdm.Torch.base.db.TrajEdgeRepresentationPool;
import au.edu.rmit.bdm.Torch.base.model.Coordinate;
import au.edu.rmit.bdm.Torch.base.model.TrajEntry;
import au.edu.rmit.bdm.Torch.base.model.Trajectory;
import au.edu.rmit.bdm.Torch.queryEngine.model.TimeInterval;
import au.edu.rmit.bdm.Torch.queryEngine.model.TorchDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TrajectoryResolver {

    private Logger logger = LoggerFactory.getLogger(TrajectoryResolver.class);
    private TrajEdgeRepresentationPool trajectoryPool;
    private Map<Integer, String[]> rawEdgeLookup;
    private Map<String, TimeInterval> timeSpanLookup;
    private boolean resolveAll;
    public FileSetting setting;
    public TimeInterval querySpan;
    public boolean contain;

    public TrajectoryResolver( TrajEdgeRepresentationPool trajectoryPool, Map<Integer, String[]> rawEdgeLookup, boolean resolveAll){
        this.trajectoryPool = trajectoryPool;
        this.rawEdgeLookup = rawEdgeLookup;
        this.resolveAll = resolveAll;
    }

    TrajectoryResolver(boolean resolveAll, FileSetting setting){
        this.resolveAll = resolveAll;
        trajectoryPool = new TrajEdgeRepresentationPool(false, setting);
        this.setting = setting;
        rawEdgeLookup = new HashMap<>();
        loadRawEdgeLookupTable();

        timeSpanLookup = new HashMap<>();
        loadTimeSpanLookupTable();

    }

    QueryResult resolve (String queryType, List<String> trajIds, List<TrajEntry> rawQuery, Trajectory<TrajEntry> _mappedQuery) {

        List<TrajEntry> mappedQuery = _mappedQuery;
        if (!queryType.equals(Torch.QueryType.RangeQ))
            mappedQuery = resolveMappedQuery(_mappedQuery);

        logger.info("number of ids before: {}", trajIds.size());

        if (querySpan != null && !queryType.equals(Torch.QueryType.TopK)) {
            Iterator<String> iter = trajIds.iterator();
            if (contain) {
                while (iter.hasNext()) {
                    String id = iter.next();
                    TimeInterval candidate_time_span = timeSpanLookup.get(id);
                    if (!querySpan.contains(candidate_time_span))
                        iter.remove();
                }
            } else {  //join but not contain
                while (iter.hasNext()) {
                    String id = iter.next();
                    TimeInterval candidate_time_span = timeSpanLookup.get(id);
                    if (!querySpan.joins(candidate_time_span))
                        iter.remove();
                }
            }

        }

        logger.info("number of ids after: {}", trajIds.size());

        QueryResult ret;
        if (resolveAll)
            ret = QueryResult.genResolvedRet(queryType, resolveRet(trajIds), rawQuery, mappedQuery);
        else {
            int[] ids = new int[trajIds.size()];
            for (int i = 0; i < trajIds.size(); i++)
                ids[i] = Integer.valueOf(trajIds.get(i));
            ret = QueryResult.genUnresolvedRet(queryType, ids, rawQuery, mappedQuery);
        }
        return ret;
    }

    public boolean meetTimeConstrain(String trajId){
        if (querySpan == null) return true;

        if (contain)
            return querySpan.contains(timeSpanLookup.get(trajId));

        return querySpan.joins(timeSpanLookup.get(trajId));
    }

    private List<TrajEntry> resolveMappedQuery(Trajectory<TrajEntry> mappedQuery) {

        List<TrajEntry> l = new Trajectory<>();
        int queryLen = mappedQuery.edges.size();

        for (int i = 1; i < queryLen; i++) {

            String[] tokens = rawEdgeLookup.get(mappedQuery.edges.get(i).id);
            String[] lats = tokens[0].split(",");
            String[] lngs = tokens[1].split(",");

            for (int j = 0; j < lats.length; j++) {
                l.add(new Coordinate(Double.parseDouble(lats[j]),Double.parseDouble(lngs[j])));
            }
        }
        return l;
    }

    public List<Trajectory<TrajEntry>> resolveResult(int[] ids) {
        List<String> trajIds = new ArrayList<>(ids.length);
        for (int i : ids) trajIds.add(String.valueOf(i));
        return resolveRet(trajIds);
    }



    private List<Trajectory<TrajEntry>> resolveRet(Collection<String> trajIds) {
        List<Trajectory<TrajEntry>> ret = new ArrayList<>(trajIds.size());
        for (String trajId : trajIds){

            int[] edges = trajectoryPool.get(trajId);
            if (edges == null) {
                logger.debug("cannot find trajectory id {}, this should not be happened", trajId);
                continue;
            }

            Trajectory<TrajEntry> t = new Trajectory<>();
            t.id = trajId;

            for (int i = 1; i < edges.length; i++) {

                String[] tokens = rawEdgeLookup.get(edges[i]);
                String[] lats = tokens[0].split(",");
                String[] lngs = tokens[1].split(",");

                for (int j = 0; j < lats.length; j++) {
                    t.add(new Coordinate(Double.parseDouble(lats[j]),Double.parseDouble(lngs[j])));
                }
            }
            ret.add(t);
        }
        return ret;
    }

    private void loadRawEdgeLookupTable() {

        logger.info("load raw edge lookup table");

        try(FileReader fr = new FileReader(setting.ID_EDGE_RAW);
            BufferedReader reader = new BufferedReader(fr)){
            String line;
            String[] tokens;
            int id;
            String lats;
            String lngs;
            while((line = reader.readLine())!=null){
                tokens = line.split(";");
                id = Integer.parseInt(tokens[0]);
                lats = tokens[1];
                lngs = tokens[2];

                rawEdgeLookup.put(id, new String[]{lats, lngs});
            }

        }catch (IOException e){
            throw new RuntimeException("some critical data is missing, system on exit...");
        }
    }

    private void loadTimeSpanLookupTable() {
        logger.info("load time querySpan lookup table");

        try(FileReader fr = new FileReader(setting.TRAJECTORY_START_END_TIME_PARTIAL);
            BufferedReader reader = new BufferedReader(fr)){
            String line;
            String[] tokens;
            String id;
            String[] span;
            String start;
            String end;
            while((line = reader.readLine())!=null){
                tokens = line.split(Torch.SEPARATOR_2);
                id= tokens[0];
                span = tokens[1].split(" \\| ");
                start = span[0];
                end = span[1];

                timeSpanLookup.put(id, buildInterval(id, start, end));
            }

        }catch (IOException e){
            throw new RuntimeException("some critical data is missing, system on exit...");
        }

    }

    private TimeInterval buildInterval(String id, String start, String end) {
        TorchDate startDate = new TorchDate().setAll(start);
        TorchDate endDate = new TorchDate().setAll(end);
        return new TimeInterval(id, startDate, endDate);
    }

    public void setTimeInterval(TimeInterval span, boolean contain) {
        this.querySpan = span;
        this.contain = contain;
    }
}
