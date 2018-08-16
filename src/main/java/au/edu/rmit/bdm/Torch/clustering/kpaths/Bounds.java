package au.edu.rmit.bdm.Torch.clustering.kpaths;



import java.util.ArrayList;
import java.util.Map;

/*
 * this class mainly computes the bounds for pruning
 */
public class Bounds extends Process{

	public Bounds(String datapath) {
		super(datapath);
	}
	
	/*
	 * compute the distance between any two centers
	 */
	public static void computeInterCentorid(int k, ArrayList<ClusterPath> Center, Map<Integer, int[]> clustData) {
		for(int i=0; i<k; i++) {
			innerCentoridDis[i] = new double[k];
			int []a = clustData.get(i);		
			double min = Double.MAX_VALUE;
			for(int j=0; j<k; j++) {				
				if(i!=j) {
					int []b = clustData.get(j);
					double distance = Yinyang.computeRealDistance(a, b, 0);
					innerCentoridDis[i][j] = distance;
					if(distance<min) {
						min = distance;
					}					
				}
			}
			//when i=k-1, it will be max, a bug needed to solve
			interMinimumCentoridDis[i] = min;
		}
	}
}
