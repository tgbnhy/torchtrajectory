package au.edu.rmit.bdm.Torch.clustering.streaming;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/*
 * it stores the inverted index for each edge
 */
public class streamEdge {
	TreeMap<Integer, ArrayList<Integer>> timeindex;//sorted timestamp, the carid list
	int id;//the id of each edge
	public streamEdge(int id) {
		this.id = id;
		timeindex = new TreeMap<>();
	}
	
	public void addCars(int cartime, int[] carsid) {
		ArrayList<Integer> traidlist;
		if(timeindex.containsKey(cartime)) {
			traidlist = timeindex.get(cartime);
		}else {
			traidlist = new ArrayList<>();
		}
		for(int j=0; j<carsid.length; j++) {
			traidlist.add(Integer.valueOf(carsid[j]));
		}
		timeindex.put(cartime, traidlist);
	}
	
	/*
	 * delete expired data which will not be used for data and index within in the sliding window
	 */
	public void removeExprired(int expiredtime, int formerexpired, Map<Integer, ArrayList<Integer>> dataset_remove) {
		if(!timeindex.isEmpty())
		for(int timeid:timeindex.keySet()) {
			if(timeid < expiredtime && timeid >= formerexpired) {
				ArrayList<Integer> cars = timeindex.get(timeid);
				for(int carid: cars) {
					ArrayList<Integer> edgeids = null;
					if(dataset_remove.containsKey(carid))
						edgeids= dataset_remove.get(carid);
					else
						edgeids = new ArrayList<>();
					edgeids.add(id);
					dataset_remove.put(carid, edgeids);
				}
			}
		}
	}
}
