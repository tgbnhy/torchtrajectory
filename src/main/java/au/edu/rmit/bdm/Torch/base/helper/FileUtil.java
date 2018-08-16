package au.edu.rmit.bdm.Torch.base.helper;

import java.io.File;

public class FileUtil {

    /**
     * ensure parent dir exists.
     * @param path path of the file
     */
    public static void ensureExistence(String path){
        File f = new File(path);
        if (!f.getParentFile().exists()){
            f.getParentFile().mkdirs();
        }
    }
}
