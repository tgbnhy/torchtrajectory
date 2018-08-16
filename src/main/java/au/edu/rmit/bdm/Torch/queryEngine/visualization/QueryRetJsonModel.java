package au.edu.rmit.bdm.Torch.queryEngine.visualization;

import au.edu.rmit.bdm.Torch.queryEngine.query.QueryResult;

import java.util.ArrayList;
import java.util.List;

class QueryRetJsonModel {

    final String queryType;
    final boolean mappingSucceed;
    final TrajJsonModel raw;
    final TrajJsonModel mapped;
    final int retSize;
    final List<TrajJsonModel> ret;

    QueryRetJsonModel(QueryResult queryResult){
        this.queryType = queryResult.queryType;
        this.mappingSucceed = queryResult.mappingSucceed;
        this.raw = queryResult.rawQuery == null ? null : new TrajJsonModel(queryResult.rawQuery);
        this.mapped = queryResult.mappedQuery == null ? null : new TrajJsonModel(queryResult.mappedQuery);
        this.retSize = queryResult.retSize;
        this.ret = new ArrayList<>(queryResult.resolvedRet.size());
        ret.addAll(Formater.model(queryResult.resolvedRet));
    }

    QueryRetJsonModel(QueryResult queryResult,int maximum){

        this.queryType = queryResult.queryType;
        this.mappingSucceed = queryResult.mappingSucceed;
        this.raw = queryResult.rawQuery == null ? null : new TrajJsonModel(queryResult.rawQuery);
        this.mapped = queryResult.mappedQuery == null ? null : new TrajJsonModel(queryResult.mappedQuery);
        this.retSize = queryResult.retSize;
        this.ret = new ArrayList<>(retSize > maximum ? maximum : retSize);
        if (queryResult.retSize > maximum)
            ret.addAll(Formater.model(queryResult.resolvedRet.subList(0,maximum)));
        else
            ret.addAll(Formater.model(queryResult.resolvedRet));
    }

}
