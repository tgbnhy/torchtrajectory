package au.edu.rmit.bdm.Torch.clustering.kpaths;

import java.io.IOException;
import java.util.Properties;

/*
 * read the parameters from file
 */
class LoadProperties{

	static String load(String p){
		Properties prop = new Properties();
		try {
			prop.load(LoadProperties.class.getClassLoader().getResourceAsStream("conf.properties"));
			return prop.getProperty(p);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}