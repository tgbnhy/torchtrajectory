package au.edu.rmit.bdm.clustering.trajectory.kpaths;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import au.edu.rmit.bdm.Torch.base.FileSetting;
import au.edu.rmit.bdm.clustering.trajectory.TrajectoryMtree;

/*
 * this class will mainly optimize the class process on assignment and refinement based on the Yinyang k-means
 * reference paper: Yinyang K-means: A drop-in replacement of the classic K-means with consistent speedup
 */
public class Yinyang extends Process {
	private Thread runingThread;//multi thread
	private String threadName;
	TrajectoryMtree centroidindex;//the tree used to group the centroid in the first
	
	protected Map<Integer, Double> center_drift = null;
	protected double[] group_drift = null;
	int numFilCenter=0;
	protected int numFilGroup=0;
	protected int numFilWholGroup=0;
	protected int numeMovedTrajectories=0;
	protected int indexFil=0;
	int numCompute=0;
	
	public Yinyang(String datapath) {
		super(datapath);
		threadName = datapath;
		iterationStops = true;
	}
	
	public Yinyang(String []datapath) {
		super(datapath);
	}

	public Yinyang(FileSetting setting, int trajNumber){
		super(setting, trajNumber);
	}
	// the kpath should be put here
	public void run() {
		System.out.println("Running " + threadName);
		long startTime = System.nanoTime();
		while (dataOut) {//there is data still from the pool
			double nowTime = (System.nanoTime()-startTime)/1000000000.0;
			if(nowTime>=slidingwindow){
				if(readingdata == true || !dataEnough)
					continue;
				
				iterationStops = false;//
				numFilCenter=0;
				numFilGroup=0; numFilWholGroup=0; numeMovedTrajectories=0; indexFil=0; numCompute=0;
				System.out.println("new clustering starts");
				
				interMinimumCentoridDis = new double[k];
				innerCentoridDis = new double[k][];
				traLength = new HashMap<>();
				edgeInfo = new HashMap<>();//this will be built later for nantong
				
				for(int idx: datamap.keySet()) {
					traLength.put(idx, datamap.get(idx).length);
				}
				edgeIndex = new HashMap<>();
				edgeHistogram = new HashMap<>();
				trajectoryHistogram = new HashMap<>();
				
				CENTERS = new ArrayList<>();
				int temp = trajectoryNumber;
				System.out.println("size "+trajectoryNumber);
				if (edgeIndex.isEmpty()) {
					createTrajectoryHistogram(datamap, datamap.size()); // build inverted index if there is not index				
				}
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("DDHHmmss");
				folder = sdf.format(cal.getTime());
				String new_folder = mapv_path + folder;
				boolean creat_folder = new File(new_folder).mkdirs();
		//		System.out.println("ccccccccccc");
		//		if (!creat_folder)
		//			return;
				
				initializeClustersIncrease1(k, 10);
		//		yinyangkPath(k, folder);
		//		runrecord.printLog();
				iterationStops = false;
				while(temp == trajectoryNumber) {//sleep 0.1s if the data not changed
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void start() {
		System.out.println("Starting " + threadName);
		if (runingThread == null) {
			runingThread = new Thread(this, threadName);
			runingThread.start();
		}
	}
	
	/*
	 * divide k clusters into t groups when k not equals to t
	 */
	protected void groupInitialClusters(int t, int k) {
		group = new HashMap<>();
		centerGroup = new HashMap<>();
		if(t==k) {// when k is small, we do not divide into too many groups
			for(int i = 0;i<k; i++) {
				ArrayList<Integer> a = new ArrayList<>();
				a.add(i);
				group.put(i, a);
				centerGroup.put(i, i);
			}
		}else {
			int capacity = k/(2*t);//to control the number of trajectories
			centroidindex = new TrajectoryMtree(capacity);
			//run M-tree will be better.
		}
	}
	/*
	public String getCenterString() {
		String a = "";
		for(ClusterPath cc : Process.CENTERS){
			a += Integer.toString(cc.getTrajectoryID())+"_";
		}
		return a;
	}*/
	
	/*
	 * update the upper bound of trajectory
	 */
	void updateUpperBound(Map<Integer, double[]> trajectoryBounds, int traid, double bestvalue) {
		double [] bounds = trajectoryBounds.get(traid);
		bounds[0] = bestvalue;
		trajectoryBounds.put(traid, bounds);
	}
	
	/*
	 * update the lower bound of trajectory toward group i
	 */
	protected void updateSingleLowerBound(Map<Integer, double[]> trajectoryBounds, int traid, int group_i, double newbound) {
		double [] bounds = trajectoryBounds.get(traid);
		bounds[group_i+2] = newbound;
		trajectoryBounds.put(traid, bounds);
	}
	
	/*
	 * compute the drift between current center and previous center
	 */
	protected void computeDrift(int k, int t) {
		group_drift = new double[t];
		if(center_drift.isEmpty())//if it is the first time to compute
			for(int i=0; i<k; i++) {
				int [] alist =  CENTERS.get(i).getTrajectoryData();;
				int [] blist = PRE_CENS.get(i).getTrajectoryData();
				double dis = computeRealDistance(alist, blist, 0);
				center_drift.put(i, dis);
			}
		//choose the minimum one as the group
		for(int group_i = 0; group_i < t; group_i++) {
			ArrayList<Integer> centers = group.get(group_i);
			double max_drift = 0;
			for(int centerid: centers) {
				if(max_drift<center_drift.get(centerid)) {
					max_drift = center_drift.get(centerid);
				}
			}
			group_drift[group_i] = max_drift;
		}
	}
	
	/*
	 * compute the real distance
	 */
	protected double computeRealDistance(int []tra, int[] clustra, int idx) {
		if(tra==null) {
			long startTime = System.nanoTime();							
			tra = datamap.get(idx);//  read the trajectory data
			long endtime = System.nanoTime();
			runrecord.addIOTime((endtime-startTime)/1000000000.0);
		}
		numCompute++;
		long Time1 = System.nanoTime();
		double min_dist = Intersection(tra, clustra, tra.length, clustra.length);
		long Time2 = System.nanoTime();
		runrecord.addsimiComputationTime((Time2 - Time1) / 1000000000.0);
		return min_dist;
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
	 * update the histogram when tra moves from oldcenter to the newcenter
	 * ? we can accumulate based on empty set rather than the full
	 */
	public void accumulateHistogramGuava(int []tra, int idx, int newCenter, int oldCenter) {
		if(tra==null) {//never scan
			tra = datamap.get(idx);// the trajectory data is read in the first time in this iteration
		}
		ClusterPath newCluster = CENTERS.get(newCenter);//update the entry in min_id						
		newCluster.updateHistorgramGuava(tra, idx);// add the new trajectory in
		ClusterPath oldCluster = CENTERS.get(oldCenter);
		oldCluster.removeHistorgramGuava(tra, idx);//remove the old trajectory out
	}
	
	
	/*
	 * update the trajectories in each clusters
	 */
	public void updateCenters(Map<Integer, ArrayList<Integer>> idxNeedsIn, Map<Integer, ArrayList<Integer>> idxNeedsOut) {
		long Time1 = System.nanoTime();
		for(int idx: idxNeedsIn.keySet()) {
			ArrayList<Integer> idxs = idxNeedsIn.get(idx);
			ClusterPath newCluster = CENTERS.get(idx);
			newCluster.mergeTrajectoryToCluster(idxs);
		}
		for(int idx: idxNeedsOut.keySet()) {
			ArrayList<Integer> idxs = idxNeedsOut.get(idx);
			ClusterPath newCluster = CENTERS.get(idx);
			newCluster.removeTrajectoryToCluster(idxs);
		}
		long Time2 = System.nanoTime();
		runrecord.addHistorgramTime((Time2-Time1)/1000000000.0);			
	}
	
	public double getMinimumLowerbound(double [] bounds, int groupNumber) {
		double lowerboud = Double.MAX_VALUE;
		for(int group_j=0; group_j<groupNumber; group_j++) {//get the minimum lower bound of all group
			double lowerboud_temp = Math.abs(bounds[group_j+2] - group_drift[group_j]);
			if(lowerboud_temp<lowerboud)
				lowerboud = lowerboud_temp;
		}
		return lowerboud;
	}
	
	public boolean checkInvertedIndex(Set<Integer> candilist, int idx) {
		long startTime1 = System.nanoTime();	
		boolean indexcheck = candilist.contains(idx);
		long endtime1 = System.nanoTime();
		runrecord.addIOTime((endtime1-startTime1)/1000000000.0);
		return indexcheck;
	}
	
	/*
	 * compute the distance between any two centers
	 */
	public void computeInterCentorid(int k, ArrayList<ClusterPath> Center, Map<Integer, int[]> clustData) {
		for(int i=0; i<k; i++) {
			innerCentoridDis[i] = new double[k];
			int []a = clustData.get(i);		
			double min = Double.MAX_VALUE;
			for(int j=0; j<k; j++) {				
				if(i!=j) {
					int []b = clustData.get(j);
					double distance = Intersection(a, b, a.length, b.length);
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
	/*
	 *  assign based on previous center to save time on IO and computation
	 *  group can be eliminated
	 */
	public void assignByTriangleFeaturesGroup(int k, int groupNumber) {
		Set<Integer> candidateofAllclusters = new HashSet<Integer>();
		Map<Integer, int[]> clustData = new HashMap<Integer, int[]>();
		Map<Integer, ArrayList<Integer>> idxNeedsIn = new HashMap<>();//it stores all the idxs of trajectories that move in
		Map<Integer, ArrayList<Integer>> idxNeedsOut = new HashMap<>();
		int centerMinlength = Integer.MAX_VALUE;
		int minLengthCenterid=0;
		
		long startTime2 = System.nanoTime();								
		for (int j = 0; j < k; j++) {
			long startTime1 = System.nanoTime();
			Set<Integer> candilist = CENTERS.get(j).creatCandidateList(edgeIndex, datamap);//generate the candidate list			
			Collections.addAll(candidateofAllclusters, candilist.toArray(new Integer[0]));
			long endtime1 = System.nanoTime();
			runrecord.addIOTime((endtime1-startTime1)/1000000000.0);
			int []clustra = CENTERS.get(j).getTrajectoryData();
			
			clustData.put(j, clustra);
			if(clustra.length<centerMinlength) {// get the minimum length
				centerMinlength = clustra.length;
				minLengthCenterid = j;// the center with minimum length
			}
		}
		long endtime = System.nanoTime();
		System.out.println("Index build time cost: "+(endtime-startTime2)/1000000000.0+"s");
		int movedtrajectory = 0;
		computeInterCentorid(k, CENTERS, clustData);//compute the inter centroid bound martix		
		for (int group_i = 0; group_i < groupNumber; group_i++) {//check each group
		//	ArrayList<Integer> centers = group.get(group_i);
		//	for (int centerID:centers)
			int centerID = group_i;
			{//check each center in the group
				int center_length = clustData.get(centerID).length;				
				Set<Integer> tralist = CENTERS.get(centerID).getClusterTrajectories();				
				for (int idx : tralist) { // check every trajectory in the center to assign which integrate the group filtering and local filtering				
					int[] tra = null;
					int tralength = traLength.get(idx); // the length of trajectory is read
					double min_dist = Double.MAX_VALUE;// to record the best center's distance
					int newCenterId = centerID;//initialize as the original center
					if (!checkInvertedIndex(candidateofAllclusters, idx)) { // if it is never contained by any list, we can assign it to the cluster with minimum length
						min_dist = Math.max(tralength, centerMinlength);
						double min_dist1 = Math.max(tralength, center_length);
						if(min_dist1>min_dist) {//change to other center 	
							newCenterId = minLengthCenterid;							
						}
						indexFil+=groupNumber;
					} else {//check whether we need to change the center by comparing the bounds						
						double [] bounds = trajectoryBounds.get(idx);
						double lowerbound = getMinimumLowerbound(bounds, groupNumber);	// bound from drift					
						Set<Integer> canlist = CENTERS.get(centerID).getCandidateList();	
						int[] clustra = clustData.get(centerID);
						if (checkInvertedIndex(canlist, idx)) {
							long startTime = System.nanoTime();						
							tra = datamap.get(idx);//  read the trajectory data
							endtime = System.nanoTime();
							runrecord.addIOTime((endtime-startTime)/1000000000.0);
							min_dist = computeRealDistance(tra, clustra, idx);//compute the distance with new center						
						}else {// do not need to read as no overlap
							min_dist = Math.max(tralength, clustra.length);
						}
						double newupperbound = min_dist;// tighten the upper bound
						newCenterId = centerID;
						double centroidBound = interMinimumCentoridDis[centerID]/2.0;
						lowerbound = Math.max(lowerbound, centroidBound);
						if(lowerbound < newupperbound){//cannot not pass the group filtering
							for(int group_j=0; group_j<groupNumber; group_j++) {
								if( group_j == group_i)//skip current group
									continue;
								double localbound = Math.max((bounds[group_j+2]-group_drift[group_j]), innerCentoridDis[centerID][group_j]/2.0);
								if( localbound < min_dist) {//the groups that cannot pass the filtering of bound and inverted index									
								//	ArrayList<Integer> centerCandidates = group.get(group_j);
									double second_min_dist_local = Double.MAX_VALUE;
								//	for(int center_j: centerCandidates) 
									int center_j = group_j;
									{// goto the local filtering on center in a group, by checking the candidate list and bounds												
										canlist = CENTERS.get(center_j).getCandidateList();// get the candidate list of each cluster										
										clustra = clustData.get(center_j);
										double dist = 0;
										if (checkInvertedIndex(canlist, idx)) {
											dist = computeRealDistance(tra, clustra, idx);
										} else {
											indexFil++;
											dist = Math.max(tralength, clustra.length);
										}
										if (min_dist > dist) {
											min_dist = dist; // maintain the one with min distance, and second min distance
											newCenterId = center_j;
										}
										if(second_min_dist_local>dist) {
											second_min_dist_local = dist;
										}
									}
									updateSingleLowerBound(trajectoryBounds, idx, group_j, second_min_dist_local);
								}else {
									numFilGroup++;
									updateSingleLowerBound(trajectoryBounds, idx, group_j, bounds[group_j+2] - group_drift[group_j]);
								}
							}
						}else {
							numFilWholGroup++;
						}
					}													
					if(newCenterId!=centerID) {// the trajectory moves to other center, this should be counted into the time of refinement.
						movedtrajectory++;
						numeMovedTrajectories++;
						long Time1 = System.nanoTime();		
						ArrayList<Integer> idxlist;
						if(idxNeedsIn.containsKey(newCenterId))
							idxlist = idxNeedsIn.get(newCenterId);
						else
							idxlist = new ArrayList<Integer>();
						idxlist.add(idx);
						idxNeedsIn.put(newCenterId, idxlist);// temporal store as we cannot add them the trajectory list which will be scanned later, batch remove later
						if(idxNeedsOut.containsKey(centerID))
							idxlist = idxNeedsOut.get(centerID);
						else
							idxlist = new ArrayList<Integer>();
						idxlist.add(idx);
						idxNeedsOut.put(centerID, idxlist);// temporal store, batch remove later								
						accumulateHistogramGuava(tra, idx, newCenterId, centerID);	// update the histogram directly
						long Time2 = System.nanoTime();
						runrecord.addHistorgramTime((Time2-Time1)/1000000000.0);
					}						
				}
			}
		}
		System.out.println(movedtrajectory);
		long Time1 = System.nanoTime();		
		updateCenters(idxNeedsIn, idxNeedsOut);
		long Time2 = System.nanoTime();
		runrecord.addHistorgramTime((Time2-Time1)/1000000000.0);
	}
	
	/*
	 * we do not check every candidate, but use the inverted index to assign in batch
	 * the initial center should not intersect with each other
	 */
	public void firstAssignmentWithInvertedIndex(int k) {
		Random rand = new Random();
		//assign based on index, 
		ArrayList<Integer> assignedTra = new ArrayList<>();
		TreeMap<Integer, ArrayList<Integer>> lengthCluster = new TreeMap<>();
		for (int j = 0; j < k; j++) {
			ClusterPath newCluster = CENTERS.get(j);
			int clusterLen = newCluster.getTrajectoryData().length;	//read the cluster trajectory data in advance
			ArrayList<Integer> samelengthCluster;
			if(lengthCluster.containsKey(clusterLen)) {
				samelengthCluster = lengthCluster.get(clusterLen);
			}else {
				samelengthCluster = new ArrayList<>();
			}
			samelengthCluster.add(j);
			lengthCluster.put(clusterLen, samelengthCluster);			
			Set<Integer> candilist = newCluster.creatCandidateList(edgeIndex, datamap);//generate the candidate list
			for(int idx: candilist) {
				int[] tra = datamap.get(idx);//the trajectory data is read
				CENTERS.get(j).updateHistorgramGuava(tra, idx); //update the edge histogram using every new trajectory
			}
			newCluster.mergeTrajectoryToCluster(new ArrayList<Integer>(candilist));
			assignedTra.addAll(candilist);//already assigned
		}
		
		int []lengthArr = lengthCluster.keySet().stream().mapToInt(i -> i).toArray();
		for(int idx=0; idx<trajectoryNumber; idx++) {		//for the candidates that do not intersect, we can assign randomly based on length.
			if(assignedTra.contains(idx)) {
				continue;
			}
			int[] tra = datamap.get(idx);//the trajectory data is read
			int tralength = traLength.get(idx);
			int lengthid = Util.findClosest(lengthArr, tralength); //find the closest length in the keyset
			ArrayList<Integer> cenlist = lengthCluster.get(lengthid);
			int randomid = rand.nextInt(cenlist.size());
			int min_id = cenlist.get(randomid);
			CENTERS.get(min_id).updateHistorgramGuava(tra, idx); //update the edge histogram using every new trajectory
			CENTERS.get(min_id).addTrajectoryToCluster(idx);
		}
	}
	
	
	/*
	 *  assign based on previous center to save time on IO based on inverted index only
	 */
	public void assignAccumulateInvertedindex(int k, int groupNumber) {
		Set<Integer> candidateofAllclusters = new HashSet<Integer>();
		Map<Integer, int[]> clustData = new HashMap<Integer, int[]>();
		Map<Integer, ArrayList<Integer>> idxNeedsIn = new HashMap<>();//it stores all the idxs of trajectories that move in
		Map<Integer, ArrayList<Integer>> idxNeedsOut = new HashMap<>();
		int centerMinlength = Integer.MAX_VALUE;
		int minLengthCenterid=0;
		for (int j = 0; j < k; j++) {
			Set<Integer> candilist = CENTERS.get(j).creatCandidateList(edgeIndex, datamap);//generate the candidate list
		//	candidateofAllclusters.addAll(candilist);// merge it to a single list, the candidate list
			Collections.addAll(candidateofAllclusters, candilist.toArray(new Integer[0]));
			int[] clustra = CENTERS.get(j).getTrajectoryData();
			
			clustData.put(j, clustra);
			if(clustra.length<centerMinlength) {// get the minimum length
				centerMinlength = clustra.length;
				minLengthCenterid = j;// the center with minimum length
			}
		}
		
		for (int centerID =0 ;centerID<k;  centerID++) {//check each center in the group
				int center_length = clustData.get(centerID).length;				
				Set<Integer> tralist = CENTERS.get(centerID).getClusterTrajectories();				
				for (int idx : tralist) { // check every trajectory in the center to assign which integrate the group filtering and local filtering
					int[] tra = null;
					int tralength = traLength.get(idx); // the length of trajectory is read
					double min_dist = Double.MAX_VALUE;// to record the best center's distance
					int newCenterId = centerID;//initialize as the original center
					if (!candidateofAllclusters.contains(idx)) { // if it is never contained by any list, we can assign it to the cluster with minimum length
						min_dist = Math.max(tralength, centerMinlength);
						newCenterId = minLengthCenterid;
						double min_dist1 = Math.max(tralength, center_length);
						if(min_dist1>min_dist) {//change to other center	
							newCenterId = minLengthCenterid;							
						}
					} else {//check whether we need to change the center by comparing the bounds
							for(int center_j=0; center_j<k; center_j++) {// goto the local filtering on center in a group, by checking the candidate list and bounds								
								Set<Integer> canlist = CENTERS.get(center_j).getCandidateList();// get the candidate list of each cluster										
								int[] clustra = clustData.get(center_j);
								double dist = 0;
								if (canlist.contains(idx)) {																								
									dist = computeRealDistance(tra, clustra, idx);
								} else {
									dist = Math.max(tralength, clustra.length);
								}								
								if (min_dist > dist) {
									min_dist = dist; // maintain the one with min distance, and second min distance
									newCenterId = center_j;
								}
							}			
					}
					long Time1 = System.nanoTime();					
					if(newCenterId!=centerID) {// the trajectory moves to other center						
						ArrayList<Integer> idxlist;
						if(idxNeedsIn.containsKey(newCenterId))
							idxlist = idxNeedsIn.get(newCenterId);
						else
							idxlist = new ArrayList<Integer>();
						idxlist.add(idx);
						idxNeedsIn.put(newCenterId, idxlist);// temporal store as we cannot add them the trajectory list which will be scanned later, batch remove later
						if(idxNeedsOut.containsKey(centerID))
							idxlist = idxNeedsOut.get(centerID);
						else
							idxlist = new ArrayList<Integer>();
						idxlist.add(idx);
						idxNeedsOut.put(centerID, idxlist);// temporal store, batch remove later
						accumulateHistogramGuava(tra, idx, newCenterId, centerID);	// update the histogram directly				
					}				
					long Time2 = System.nanoTime();
					runrecord.addHistorgramTime((Time2-Time1)/1000000000.0);
					updateUpperBound(trajectoryBounds, idx, min_dist);
				}
			}
		updateCenters(idxNeedsIn, idxNeedsOut);
	}
	
	/*
	 * the yinyang algorithm
	 */
	public int yinyangkPath(int k, String folder, Set<Integer> candidateset) {
        int groupNumber = k;
        trajectoryBounds = new HashMap<>();
        center_drift = new HashMap<Integer, Double>();
		groupInitialClusters(groupNumber, k); //	Step 1: divide k centroid into t groups
		singleKpath(k, 0, true, groupNumber, folder, candidateset); // 	Step 2, generate the initial center
		computeDrift(k, groupNumber);// Step 3.1 compute the drift using PRE_CENS and CENTERS
		int t = 1;		
	//	runrecord.clear();
		for(; t < TRY_TIMES; t++){
			if(graphPathExtraction){
				printCluterTrajectory(k, t, folder);
			}else {
				printCluterTraID(k, t, folder);
			}
			long startTime1 = System.nanoTime();		
	//		assignAccumulateInvertedindex(k, groupNumber);	// Step 3.2, 3.3: assign to each group	
			assignByTriangleFeaturesGroup(k, groupNumber);
			long endtime = System.nanoTime();
			runrecord.addAssignmentTime((endtime-startTime1)/1000000000.0);
			System.out.println("assign time cost: "+(endtime-startTime1)/1000000000.0+"s");
			long startTime = System.nanoTime();
			double overallDis = 0;	        
			for(int i=0; i<k; i++) {
				double drfit = 0;
				if(graphPathExtraction){
					drfit = CENTERS.get(i).extractNewPathFrequency(forwardGraph, backwardGraph, i);// test the optimal
				}else {
					drfit = CENTERS.get(i).extractNewPathGuava(datamap, runrecord, traLength, trajectoryHistogram); //update the centroid of each cluster
				}
				center_drift.put(i, drfit);
				overallDis += CENTERS.get(i).getSumDistance();
			}
			computeDrift(k, groupNumber);// 	Step 3.1 compute the drift using PRE_CENS and CENTERS
			endtime = System.nanoTime();
			runrecord.addRefinementTime((endtime-startTime)/1000000000.0);			
			System.out.println("iteration "+(t+1)+", the sum distance is "+overallDis+", time cost: "+(endtime-startTime1)/1000000000.0+"s\n");
			if(timeToEnd()) {//all center does not change any more
				runrecord.setIterationtimes(t+1);
				break;//convergence
			}
		}		
		System.out.println("\n#Filtered (groups, by index, moved centers), #computation: ("+(numFilGroup+numFilWholGroup*groupNumber)+", "+ indexFil+", "+numeMovedTrajectories+", "+numCompute+")");
		return t;
	}

	@Override
	public int[] clustering(Set<Integer> trajIds, int k){
		this.k = k;
		CENTERS = new ArrayList<>();
		interMinimumCentoridDis = new double[k];
		innerCentoridDis = new double[k][];

		try {
			Set<Integer> transformed = new HashSet<>();
			for (Integer trans : trajIds)
				transformed.add(search2ClusterLookup.get(trans));

			initializeClustersIncrease(k, 100);
			yinyangkPath(k, null, transformed);

			runrecord.printLog();
			int[] results = new int[k];
			for (int i = 0; i < k; i++)
				results[i] = cluster2SearchLookup.get(CENTERS.get(i).getTrajectoryID());

			return results;
		} catch (Exception e) {
			logger.error("error when perform clustering: " + e.getMessage());
			e.printStackTrace();
		}

		return new int[0];
	}

	@Override
	public int[] clustering(int[] trajIds, int k){
		this.k = k;
		CENTERS = new ArrayList<>();
		interMinimumCentoridDis = new double[k];
		innerCentoridDis = new double[k][];

		try {
			Set<Integer> transformed = new HashSet<>();
			for (Integer trans : trajIds)
				transformed.add(search2ClusterLookup.get(trans));

			initializeClustersIncrease(k, 100);
			yinyangkPath(k, null, transformed);

			runrecord.printLog();
			int[] results = new int[k];
			for (int i = 0; i < k; i++)
				results[i] = cluster2SearchLookup.get(CENTERS.get(i).getTrajectoryID());

			return results;
		} catch (Exception e) {
			System.err.println("error when perform clustering: " + e.getMessage());
			e.printStackTrace();
		}

		return new int[0];
	}
}
