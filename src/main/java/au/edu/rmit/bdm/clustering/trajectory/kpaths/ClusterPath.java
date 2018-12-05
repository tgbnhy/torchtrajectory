package au.edu.rmit.bdm.clustering.trajectory.kpaths;
import java.util.*;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
/*
 * this store the information of each cluster
 */
public class ClusterPath {
	protected Set<Integer> clusterTrajectory; // it stores all the idxs of trajectories
	protected VIseries centroid; // store the centorid path
	int iteration = 1000; // the maximum iteration times
	
	// build histogram for edges and length using hashmap and Guava
	protected Multiset<Integer> edgeOcc; 
	protected Multiset<Integer> lengthOcc;
	protected boolean centerChanged=true;
	int minlength, maxlength;
	int []lengthAccu;//for the score computation
	Map<String, Double> checkedList;//store the start, end edges, and the score
    PriorityQueue<Path> queue;
    ArrayList<Integer> sortedFrequency;
    double sumEdgeOcc;
    int pathMinlength;
	
	protected int idx;// it stores the idx of the trajectory which is the centroid.
	protected double sumdistance=0; // the sum distance in this cluster.
	protected Set<Integer> candidateList = new HashSet<>();//built from the inverted index
	protected int []finalPath;
    
	public ClusterPath(int[] cl, int idx1) {		
		clusterTrajectory = new HashSet<Integer>();
		centroid = new VIseries();
		centroid.setVIseries(cl);
		if(cl!=null)
			centroid.length = cl.length;
		else
			centroid.length = 0;
		centroid.idx = idx1;
		this.idx = idx1;
		this.finalPath = cl;
		this.edgeOcc = HashMultiset.create();
		this.lengthOcc = HashMultiset.create();
	}
	
	/*
	 * generate the list id of trajectory that share edge with cluster
	 */
	public Set<Integer> creatCandidateList(Map<Integer, List<Integer>> edgeIndex, Map<Integer, int[]> datamap) {
		if (centerChanged) {//create a new list if changed, otherwise return previous to avoid rebuild
			int[] trajectory = datamap.get(idx);
			candidateList = new HashSet<Integer>();
			for (int edgeid : trajectory) {
				List<Integer> traidlist = edgeIndex.get(edgeid);
				Collections.addAll(candidateList, traidlist.toArray(new Integer[0]));
			}			
		}
		return candidateList;
	}
	
	/*
	 * generate the list id of trajectory that share edge with cluster, or we can ignore this
	 */
	public Set<Integer> creatCandidateListNoDatamap(Map<Integer, List<Integer>> edgeIndex, int[] trajectory) {
		if (centerChanged) {//create a new list if changed, otherwise return previous to avoid rebuild
			candidateList = new HashSet<Integer>();
			for (int edgeid : trajectory) {
				List<Integer> traidlist = edgeIndex.get(edgeid);
				Collections.addAll(candidateList, traidlist.toArray(new Integer[0]));
			}			
		}
		return candidateList;
	}
	
	public Set<Integer> getCandidateList() {
		return candidateList;
	}
	
	/*
	 * add the trajectory into the clusters
	 */
	void mergeTrajectoryToCluster(ArrayList<Integer> index){
		clusterTrajectory.addAll(index);
	}
	
	/*
	 * add the trajectory into the clusters
	 */
	void removeTrajectoryToCluster(ArrayList<Integer> index){
		clusterTrajectory.removeAll(index);
	}
	
	/*
	 * add the trajectory into the clusters
	 */
	void addTrajectoryToCluster(int index){
		clusterTrajectory.add(index);
	}
	
	/*
	 * remove the trajectory into the clusters
	 */
	void removeTrajectoryToCluster(int value){
		if(clusterTrajectory.contains(value)) {
			clusterTrajectory.remove(new Integer(value));
		}
	}
		
	/*
	 * we will try to use the data sketches instead of using hashmap;
	 */
	public void updateHistorgramGuava(int[] tra, int idx) {
		lengthOcc.add(tra.length);
		for(int edge: tra) {
			edgeOcc.add(edge);
		}
	}
	
	/*
	 * we will try to use the data sketches instead of using hashmap;
	 */
	void removeHistorgramGuava(int[] tra, int idx) {
		lengthOcc.remove(tra.length, 1);
		for(int edge: tra) {
			edgeOcc.remove(edge, 1);
		}
	}
	
	/*
	 * we will try to use the data sketches instead of using hashmap;
	 */
	public void updateHistorgramGuava(Multiset<Integer> edgeH, Multiset<Integer> lengthH) {
		edgeOcc.addAll(edgeH);
		lengthOcc.addAll(lengthH);
	}
	
	/*
	 * we will try to use the data sketches instead of using hashmap;
	 */
	public void removeHistorgramGuava(Multiset<Integer> edgeH, Multiset<Integer> lengthH) {
		edgeOcc.removeAll(edgeH);
		lengthOcc.removeAll(lengthH);
	}
	
	/*
	 * return the sum distance in the cluster
	 */
	public double getSumDistance() {
		return sumdistance;
	}
	
	/*
	 * return the clusters
	 */
	VIseries getClusterPath() {
		return centroid;
	}
	
	/*
	 * return the trajectory array of this cluster
	 */
	public Set<Integer> getClusterTrajectories() {
		return clusterTrajectory;
	}
	
	public int getTrajectoryID() {
		return idx;
	}
	
	public int[] getTrajectoryData() {
		return finalPath;
	}
	
	public boolean getCenterChanged() {
		return centerChanged;
	}
	
	//for bound computing
	int getFirstNSum(ArrayList<Integer> sortedFrequency, int number) {
		int sum = 0;
		for(int i=0; i<number; i++) {
			sum+=sortedFrequency.get(i);
		}
		return sum;
	}
	
	public void accumulateLenghtOcc(){				
		ArrayList<Integer> sortedTralen = new ArrayList<Integer>(lengthOcc.elementSet());
		Collections.sort(sortedTralen);		//sorted length increasingly
		if (lengthOcc.isEmpty()) {
			System.out.println("A cluster is empty now due to bad random initialization");
			return;
		}
		minlength = sortedTralen.get(0);
		maxlength = sortedTralen.get(sortedTralen.size()-1);
		lengthAccu = new int[maxlength-minlength+1];
		sumEdgeOcc = 0;		
		sortedFrequency = new ArrayList<Integer>();
		for(int edge:edgeOcc.elementSet()) {//reverse way
			sortedFrequency.add(edgeOcc.count(edge));
			sumEdgeOcc += edgeOcc.count(edge);		//sorted frequency
		}
		Collections.sort(sortedFrequency, Collections.reverseOrder());		
		for(int length=minlength; length<=maxlength; length++) {
			int occ = 0;
			for(int i=length; i>minlength; i--) {
				if(lengthOcc.contains(i))
					occ += lengthOcc.count(i);
			}
			lengthAccu[length-minlength] = occ;
		}
	}
	
	/*
	 * We use Multiset in the google library to update the histogram in a faster way by scanning each trajectory
	 */
	double extractNewPathGuava(Map<Integer, int[]> datamap, RunLog runrecord, Map<Integer, Integer> traLengthmap, Map<Integer, Integer> trajectoryHistogram) {
		int min = Integer.MAX_VALUE;
		int traid = 0 ;
		accumulateLenghtOcc();
		for(int traidx: clusterTrajectory) {		//read each trajectory in this cluster, it is slow as we need to read every trajectory, we can construct a weighted graph and choose the route			
			int traLength = traLengthmap.get(traidx);
			int bound = Math.min(trajectoryHistogram.get(traidx), getFirstNSum(sortedFrequency, traLength));
			int sumLength = edgeOcc.size();
			int i=minlength;
			while(i <= traLength) {
				sumLength += lengthAccu[i - minlength];
				i++;
			}						
			if((sumLength - bound) >= min) {
				continue;// skip reading the trajectory data
			}		
			int[] tra = datamap.get(traidx);
			int sumFrequency = 0;
			for(int edge: tra) {		//compute the vertex frequency of each trajectory
				sumFrequency += edgeOcc.count(edge);
			}			
			int sumDis = sumLength - sumFrequency;
			if(min>sumDis) {//choose the one which can minimize the sum of distance
				min = sumDis;
				traid = traidx;
			}
		}
		if(idx==traid)
			centerChanged = false;//center does not change;
		sumdistance = min;
		int [] a = datamap.get(traid);
	//	System.out.println(edgeOcc.size()+" "+min+" "+a.length);
		double drift = 0;
		if(centerChanged)
			drift = Intersection(finalPath, a, finalPath.length, a.length);
		centroid.setVIseries(a);
		centroid.idx = traid;		
		idx = traid;
		finalPath = a;
		return drift;
	}
	
	/* A greedy algorithm when the optimal result does not change any more and better than last result, 
	 * we will stop exploring when the length is close to the maximum length it can be
	 */
	public double extractNewPathFrequency(
			HashMap<Integer, ArrayList<Integer>> forwardGraph, 
			HashMap<Integer, ArrayList<Integer>> backwardGraph, int clusterid) {
		checkedList = new HashMap<>();
		queue = new PriorityQueue<Path>();
		ArrayList<Integer> optimal = new ArrayList<>();
		accumulateLenghtOcc();
		double optimalScore = Double.MAX_VALUE;

		if(finalPath!=null) {
			centerChanged = false;
			optimalScore = computeScore(finalPath);//initialized to min score using last centroid
			for(int a:finalPath)
				optimal.add(a);
			double lowerbound = estimateLowerbound(optimalScore, optimal.size());
			if(optimalScore > lowerbound) {
				Path aPath = new Path(optimal, optimalScore, lowerbound);
				queue.add(aPath);
			}
		}
		
		for(int edge: edgeOcc.elementSet()) {//initialize the edges as paths
			ArrayList<Integer> arrayList = new ArrayList<>();
			arrayList.add(edge);
			double score = computeScore(arrayList);
			double lowerbound = estimateLowerbound(score, 1);
			if(optimalScore > lowerbound) {
				Path aPath = new Path(arrayList, score, lowerbound);
				queue.add(aPath);
			}
		}
		
		int cou= 0;
		
		while(!queue.isEmpty()) {
			cou++;
			Path candidate = queue.poll();
			double lowerbound = candidate.getLowerbound();// compute the score	
			double score = candidate.getScore();
			if(lowerbound > optimalScore || cou>=iteration) {//termination as all possible has been checked
				break;
			}			
			ArrayList<Integer> Can = candidate.getPath();
			int start = Can.get(0);
			int end = Can.get(Can.size() - 1);// the last edge			
			if(backwardGraph.containsKey(start)) {
			ArrayList<Integer> backAppend = backwardGraph.get(start);
			if(backAppend!=null)
			for (int ids : backAppend) {			
				if (edgeOcc.contains(ids) && !Can.contains(ids)) {
					ArrayList<Integer> newCan = new ArrayList<>(Can);
					newCan.add(0, ids);	//insert to the beginning					
					if(forwardGraph.containsKey(end)) {
						ArrayList<Integer> startAppend1 = forwardGraph.get(end);// no circle
						if(startAppend1!=null && startAppend1.contains(ids)) {
							continue;
						}
					}		
					double newscore = computeScoreWithPrevious(ids, score, newCan.size());
					if (newscore < optimalScore) {
						centerChanged = true;//the center has changed
						optimalScore = newscore;
						optimal = newCan;
					}
					lowerbound = estimateLowerbound(newscore, newCan.size());
					String signature = signaturePath(ids, end);
					if (neverChecked(signature, lowerbound)) {
						continue;
					}
					if (lowerbound < optimalScore) {
						Path newpath = new Path(newCan, newscore, lowerbound);
						queue.add(newpath);
					}
				}
			}
			}
			
			if(forwardGraph.containsKey(end)) {
			ArrayList<Integer> startAppend = forwardGraph.get(end);
			if(startAppend!=null)
			for (int ids : startAppend) {
				if (edgeOcc.contains(ids) && !Can.contains(ids)) {//connected and no repetitive edges
					ArrayList<Integer> newCan = new ArrayList<>(Can);
					newCan.add(ids);				
					if(forwardGraph.containsKey(ids)) {
						ArrayList<Integer> startAppend1 = forwardGraph.get(ids);
						if(startAppend1!=null && startAppend1.contains(start)) {
							continue;
						}
					}				
					double newscore = computeScoreWithPrevious(ids, score, newCan.size());
					if (newscore < optimalScore) {
						centerChanged = true;
						optimalScore = newscore;
						optimal = newCan;
					}
					lowerbound = estimateLowerbound(newscore, newCan.size());
					String signature = signaturePath(start, ids);
					if (neverChecked(signature, lowerbound))	//repetitive path
						continue;
					if (lowerbound < optimalScore) {//if this path is promising
						Path newpath = new Path(newCan, newscore, lowerbound);
						queue.add(newpath);
					}
				}
			}
			}
		}		
		sumdistance = optimalScore;
	//	System.out.println(optimalScore+" "+optimal.size());
		Collections.sort(optimal);
		int[] centoridData = optimal.stream().mapToInt(i -> i).toArray();
		double drift=0;
		if(finalPath!=null && centerChanged)
			drift = Intersection(finalPath, centoridData, finalPath.length, centoridData.length);
		centroid.setVIseries(centoridData);
		finalPath = centoridData;
		return drift;
	}
	
	/*
	 * the data needs to be sorted before the intersection
	 */
	public int Intersection(int arr1[], int arr2[], int m, int n) {
		int i = 0, j = 0;
		int dist = 0;
		while (i < m && j < n) {
			if (arr1[i] < arr2[j])
				i++;
			else if (arr2[j] < arr1[i])
				j++;
			else
			{
				dist++;
				i++;
				j++;
			}
		}
		return Math.max(m, n)-dist;
	}
	/*
	 * check whether it exists in the checked list
	 */
	public boolean neverChecked(String aString, double lowerbound) {
		if(checkedList.containsKey(aString)) {
			double previous = checkedList.get(aString);
			if(lowerbound >= previous)
				return false;
			else {
				checkedList.put(aString, lowerbound);// add to the list
				return true;
			}
		}else {
			checkedList.put(aString, lowerbound);// add to the list
			return true;
		}
	}
	
	/*
	 * compute the objective score based on the edge histogram and length histogram,
	 */
	public double computeScore(int[] path) {
		double weight = sumEdgeOcc;
		for(int i=0; i<path.length; i++) {
			weight -= edgeOcc.count(path[i]);
		}
		if(path.length>minlength) {
			for(int i=1; i<=Math.min(path.length-minlength,maxlength-minlength); i++)
				weight += lengthAccu[i];
		}
		return weight;
	}
	
	/*
	 * compute the objective score based on the edge histogram and length histogram
	 */
	public double computeScore(ArrayList<Integer> path) {
		int []patharray =  path.stream().mapToInt(i -> i).toArray();
		return computeScore(patharray);
	}
	
	/*
	 * compute the objective score based on the edge histogram and length histogram
	 */
	public double computeScoreWithPrevious(int newedge, double weight, int length) {
		weight -= edgeOcc.count(newedge);
		if(length > minlength)
			weight += lengthAccu[length - minlength];
		return weight;
	}
	
	/*
	 * convert to a short unique string using the start and end edge, and the length, 
	 * this can be used as a dominance table, 
	 */
	public String signaturePath(int a, int b) {
		String key = a+"_"+b;
		return key;
	}
	
	//add the rest heavy edges to compute the lower bound of the score
	public double estimateLowerbound(double score, int length) {
		double lowerbound = score;
		int i=length;
		while (i < minlength && i - length < sortedFrequency.size()) {
			lowerbound -= sortedFrequency.get(i - length);
			i++;
		}
		while (i <= maxlength && (i - length < sortedFrequency.size()) && lengthAccu[i - minlength] < sortedFrequency.get(i - length)) {
			lowerbound += lengthAccu[i - minlength]- sortedFrequency.get(i - length);
			i++;
		}
		return lowerbound;
	}
	
	/*
	 * the maximum length of the final output path
	 */
	public int estimateMax() {
		int i=minlength;
		while(lengthAccu[i - minlength] < sortedFrequency.get(i - minlength)) {
			i++;
		}
		return i;
	}
}
