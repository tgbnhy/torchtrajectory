package au.edu.rmit.bdm.TTorch.base.db;

import au.edu.rmit.bdm.TTorch.base.Instance;
import au.edu.rmit.bdm.TTorch.base.Torch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class NameEdgeIdLookup {
    private Logger logger = LoggerFactory.getLogger(NameEdgeIdLookup.class);
    private DBManager db;

    public NameEdgeIdLookup(){
        db = DBManager.getDB();
    }

    public int[] get(String edgeName){
        String content = db.get(Instance.fileSetting.EDGENAME_ID_TABLE, edgeName);
        if (content == null)
            return new int[0];
        String[] temp = content.split(",");
        int[] ret = new int[temp.length];
        for (int i = 0; i < temp.length; i++)
            ret[i] = Integer.valueOf(temp[i]);
        return ret;
    }


}
