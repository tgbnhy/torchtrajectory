package au.edu.rmit.bdm.clustering.trajectory.kpaths;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import au.edu.rmit.bdm.Torch.base.FileSetting;
import au.edu.rmit.bdm.clustering.trajectory.TrajectoryMtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * static kpath is used for clustering all the taxi trips, so we can find the k-paths for designing bus routes, this is not a real time problem, so we can 
 */
public class Process extends Thread {

	Logger logger = LoggerFactory.getLogger(Process.class);
	// stores the clusters
	protected ArrayList<ClusterPath> CENTERS ; // it stores the k clusters
	ArrayList<ClusterPath> PRE_CENS; // it stores the previous k clusters
	
	// the parameters
	protected int TRY_TIMES = Integer.valueOf(LoadProperties.load("try_times"));//iteration times
	String mapv_path = LoadProperties.load("vis_path");
	String mapv_path_traclu_sigmod07 = LoadProperties.load("TraClus");
	int frequencyThreshold = Integer.valueOf(LoadProperties.load("frequencyThreshold"));
	int streamingDuration = Integer.valueOf(LoadProperties.load("streamingDuration"));
	int streamEdges = Integer.valueOf(LoadProperties.load("streamEdges"));
	protected RunLog runrecord = new RunLog(); 
	ArrayList<Integer> cluslist;
	ArrayList<int[]> centroids;
	int trajectoryNumber;// the number of trajectories in the dataset
	String folder;
	protected int k = 10;
	boolean dataEnough;
	boolean dataOut;
	boolean iterationStops;
	boolean readingdata;
	String datafile;
	int traNumber;
	String edgefile;
	String graphfile;
	int slidingwindow=10;//a window to control the inverted index, 
	
	//for Yinyang and bound computation
	protected  Map<Integer, double[]> trajectoryBounds = null;// build a lower bound list with all groups and upper bound for each trajectory, 0: upper bound, 1: global lower bound, 2~: lower bound with all different group	
	double[][] trajectoryBoundA =null;
	protected Map<Integer, ArrayList<Integer>> group = null;// group id, centers id belong to this group
	Map<Integer, Integer> centerGroup = null;//center id, group id
	
	protected double[][] innerCentoridDis;//stores the distance between every two centorids
	protected double[] interMinimumCentoridDis;//store the distance to nearest neighbor of each centorid
	
	//for storage
	protected Map<Integer, int[]> datamap; // the trajectory dataset
	protected Map<Integer, Integer> traLength; // the trajectory dataset
	protected Map<Integer, Integer> trajectoryHistogram;//the histogram of each trajectory
	protected Map<Integer, List<Integer>> edgeIndex;// the index used for similarity search
	protected Map<Integer, Integer> edgeHistogram;// the index used for similarity search
	protected Map<Integer, String> edgeInfo;// the points information
	Map<Integer, Integer> edgeType;

	Map<Integer, Integer> search2ClusterLookup;
	Map<Integer, Integer> cluster2SearchLookup;

	//for graph
	protected HashMap<Integer, ArrayList<Integer>> forwardGraph = new HashMap<Integer, ArrayList<Integer>>();//the linked edge whose start is the end of start
	protected HashMap<Integer, ArrayList<Integer>> backwardGraph = new HashMap<Integer, ArrayList<Integer>>();//the linked edge whose start is the end of start
	protected ArrayList<int[]> centoridData = new ArrayList<>();//initialize the centroid
	HashMap<String, Integer> road_types;
	
	//Mtree index 
	TrajectoryMtree mindex = new TrajectoryMtree();
	boolean mtreebuild = false;
	boolean graphPathExtraction = false;// a sign used to set whether we use the optimization
	
	public Process(String datapath) {
		trajectoryNumber=0;
	}
	
	public Process(String []args) {
		datafile = args[0];
		k = Integer.valueOf(args[1]);
		trajectoryNumber = Integer.valueOf(args[2]);
		edgefile = args[3];
		graphfile = args[4];

		try {
			init();
		}catch (IOException e){
			logger.error("IO exception in clustering module. {}", e.getMessage());
		}
	}

	public Process(){

	}

	public Process(FileSetting setting, int trajNumber){
		datafile = setting.TRAJECTORY_EDGE_REPRESENTATION_PATH_PARTIAL;
		trajectoryNumber = trajNumber;

		edgefile = setting.ID_EDGE_RAW;
		graphfile = setting.ID_EDGE_LOOKUP;

		search2ClusterLookup = new HashMap<>();
		cluster2SearchLookup = new HashMap<>();

		try {
			init();
		}catch (IOException e){
			logger.error("IO exception in clustering module. {}", e.getMessage());
		}
	}
	
	// stop the iteration when the clusters do not change compared with last time
	protected boolean timeToEnd() {
	//	if (PRE_CENS == null)
	//		return false;
		for (ClusterPath cc : CENTERS) {
			if(cc.getCenterChanged()==true) {
				return false;
			}
		}
		return true;
	}

	public void loadData(String path, int number, String edgePath) throws IOException{
		int idx=0;
		int gap = number/k;
		Random rand = new Random();
		int counter = 0;
		readRoadNetwork(edgePath);
		try {
			Scanner in = new Scanner(new BufferedReader(new FileReader(path)));					
			while (in.hasNextLine()) {// load the trajectory dataset, and we can efficiently find the trajectory by their id.
				String str = in.nextLine();
				String strr = str.trim();
				String[] abc = strr.split("\t");
				String[] vertexSeries = abc[1].split(",");
				Integer id = Integer.parseInt(abc[0]);
				cluster2SearchLookup.put(idx, id);
				search2ClusterLookup.put(id, idx);

				int[] vertexes = new int[vertexSeries.length];
				for(int t=0; t < vertexSeries.length; t++) {
					vertexes[t] = Integer.valueOf(vertexSeries[t]);
					int edgeID = vertexes[t];
					if(edgeIndex.containsKey(edgeID)) {
						List<Integer> lists = edgeIndex.get(edgeID);
						lists.add(idx);					//enlarge the lists
						edgeIndex.put(edgeID, lists);
					}else {
						ArrayList<Integer> lists = new ArrayList<Integer>();
						lists.add(idx);
						edgeIndex.put(edgeID, lists);
					}
					if(edgeHistogram.containsKey(edgeID)) {
						edgeHistogram.put(edgeID, edgeHistogram.get(edgeID)+1);
					}else {
						edgeHistogram.put(edgeID, 1);
					}
				}
				Arrays.sort(vertexes);// this sort the array
				if(mtreebuild) {//build the mtree
					mindex.buildMtree(vertexes, idx);//create the M-tree
					System.out.println(idx);
					if(idx==counter && centoridData.size()<k) {// initialize the centroid
						centoridData.add(vertexes);
						System.out.print(vertexes.length+", ");
						counter += rand.nextInt(gap);
					//	counter += 100;
						ClusterPath cl = new ClusterPath(vertexes, 0);
						CENTERS.add(cl);
					}
					idx++;
				}else {
					traLength.put(idx, vertexSeries.length);
					datamap.put(idx++, vertexes);
				}
				if(idx>number)
					break;
			}
			in.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("the trajectory dataset is loaded");
	//	System.out.println("the M-tree is built");
		System.out.println("the frequency histogram of edge is built");
		System.out.println("the inverted index of edge is built");
		
		if(mtreebuild) {
			mindex.buildHistogram();//build the histogram
			mindex.writeMtree(mapv_path);//write to the disk
		}
	}
	
	void readRoadNetwork(String edgePath) {
		road_types = new HashMap<>();
		edgeType = new HashMap<>();
		int type=0;
		try {
			Scanner in = new Scanner(new BufferedReader(new FileReader(edgePath)));
			while (in.hasNextLine()) {		// load the geo-information of all the edges in the graph
				String str = in.nextLine();
				String strr = str.trim();
				String[] abc = strr.split(";");
				edgeInfo.put(Integer.valueOf(abc[0]), abc[1]+","+abc[2]);
				if(abc.length>7) {
					 int roadType = 0;
					 if(!road_types.containsKey(abc[6])) {
						 road_types.put(abc[6], type);//we build the edge histogram
						 roadType = type++;
					 }
					 else{
						 roadType = road_types.get(abc[6]);
					 }
					 edgeType.put(Integer.valueOf(abc[0]), roadType);
				}
			}
			in.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("the edge information is loaded");		
	}
	
	/*
	 * conclude the edge information
	 */
	void concludeCenter() {
		int a[]  = new int[road_types.size()+1];
		for(int t = 0; t < k; t++) {
			int []trajectory = CENTERS.get(t).getTrajectoryData();
			for(int i=0; i<trajectory.length; i++) {
				if(edgeType.containsKey(trajectory[i])) {
					int edgeTypes = edgeType.get(trajectory[i]);
					a[edgeTypes]++;
				}else {
					a[road_types.size()]++;
				}
			}
		}
/*		System.out.println();
		for(String id: road_types.keySet()) {
			if(a[road_types.get(id)]>0)
				System.out.println(id+" "+road_types.get(id)+" "+a[road_types.get(id)]);
		}
		
		System.out.println();
		for(String id: road_types.keySet()) {
			if(a[road_types.get(id)]==0)
				System.out.println(id+" "+road_types.get(id)+" "+a[road_types.get(id)]);
		}*/
	}
	
	/*
	 * build inverted index on the EDGE, key value stored using Mapdb
	 */
	public void createTrajectoryHistogram(Map<Integer, int[]> datamap, int trajectoryNumber) {	
		//compute the frequency for each trajectory
		for(int idx:datamap.keySet()) {	//scan each trajectory
			int[] tra = datamap.get(idx);
			int tra_fre =0;
			for(int t=0; t<tra.length; t++) {	//scan each edge
				tra_fre += edgeHistogram.get(tra[t]); //the frequency is the sum of edge frequency in each trajectory.
			}
			trajectoryHistogram.put(idx, tra_fre);
		}		
		System.out.println("the frequency histogram of trajectories is built");
		System.out.println("==============================================================\n");
	}

	
	/*
	 * initialize the k clusters by randomly choosing from existing trajectories
	 */
	void initializeClustersRandom(int k) {
		Random rand = new Random();
		for(int t=0; t<k; t++) {
			int  n = rand.nextInt(trajectoryNumber) + 1;
			int[] cluster = datamap.get(n);
			ClusterPath cl = new ClusterPath(cluster, n);
			CENTERS.add(cl);		
		}
	}
	
	/*
	 * initialize the k clusters by choosing from existing trajectories incrementally
	 */
	void initializeClustersIncrease(int k, int delta) {
		int n = 0;
		for(int t=0; t<k; t++) {
			n += delta;
			int[] cluster = datamap.get(n);
			System.out.print(cluster.length+",");
			ClusterPath cl = new ClusterPath(cluster, n);
			CENTERS.add(cl);		
		}
		System.out.println();
	}
	
	/*
	 * initialize the k clusters by choosing from existing trajectories incrementally
	 */
	void initializeClustersIncrease1(int k, int delta) {
		int n = 0;
		ArrayList<Integer> keys = new ArrayList<>(datamap.keySet());
		for(int t=0; t<k; t++) {
			n += delta;
			int[] cluster = datamap.get(keys.get(n));
			ClusterPath cl = new ClusterPath(cluster, keys.get(n));
			CENTERS.add(cl);		
		}
	}
	
	/*
	 * initialize the k clusters by choosing from existing trajectories which have high frequency and do not intersect with each other
	 */
	void initializeClustersHighFrequency(int k, int range) {
		Random rand = new Random();
		//sort the trajectoryHistogram by value decreasingly, choose the top 1000 or more randomly.
		Map<Integer, Integer> sortedMap = trajectoryHistogram.entrySet().stream()
			    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
			    .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
			                              (e1, e2) -> e1, LinkedHashMap::new));
		
		ArrayList<Integer> keyset = new ArrayList<Integer>(sortedMap.keySet());
		ArrayList<Integer> allEdges = new ArrayList<Integer>();
		for(int t=0; t<k; t++) {
			int n = rand.nextInt(range) + 1;
			int idx = keyset.get(n);
			int[] cluster = datamap.get(idx);
			for(int i=0; i<cluster.length; i++) {
				if(allEdges.contains(i)) {
					t--;
					continue;
				}
			}
			Collections.addAll(allEdges, Arrays.stream(cluster).boxed().toArray(Integer[]::new));
			ClusterPath cl = new ClusterPath(cluster, idx);
			CENTERS.add(cl);
		}
	}
	
	/*
	 *  print the cluster ids and generate the trajectory into file for mapv visualization
	 */
	public int[] printCluterTraID(int k, int iteration, String folder) {		
		int []clusterid = new int[k];
		cluslist = new ArrayList<Integer>();
		for(int t = 0; t < k; t++) {
			System.out.print(CENTERS.get(t).getTrajectoryID()+",");
			clusterid[t] = CENTERS.get(t).getTrajectoryID();
		}
		System.out.println();
		return clusterid;
	}
	
	/*
	 *  print the cluster ids and generate the trajectory into file for mapv visualization
	 */
	public void printCluterTrajectory(int k, int iteration, String folder) {		

		centroids = new ArrayList<>();
		for(int t = 0; t < k; t++) {
			System.out.print(CENTERS.get(t).getTrajectoryData().length+",");
			centroids.add(CENTERS.get(t).getTrajectoryData());
		}
		System.out.println();
	//	mapv.generateClusterPath1(datamap, edgeInfo, centroids, output);
	}
	
	/*
	 * assign by building the histogram again
	 */
	public ArrayList<ClusterPath> assignRebuildInvertedindex(int k, ArrayList<ClusterPath> new_CENTERS, boolean yinyang, 
			int groupnumber, Set<Integer> candidateset) {
		Set<Integer> candidateofAllclusters = new HashSet<Integer>();
		Map<Integer, int[]> clustData = new HashMap<Integer, int[]>();
		int minlength = Integer.MAX_VALUE;
		int min_length_id=0;
		for (int j = 0; j < k; j++) {
			Set<Integer> candilist = CENTERS.get(j).creatCandidateList(edgeIndex, datamap);//generate the candidate list
			candidateofAllclusters.addAll(candilist);// merge it to a single list
			int[] clustra = CENTERS.get(j).getTrajectoryData();
			
			clustData.put(j, clustra);
			if(clustra.length<minlength) {// get the minimum length
				minlength = clustra.length;
				min_length_id = j;
			}
		}

		System.err.println(candidateset);
		for (int idx:candidateset) {

			long Time1 = System.nanoTime();
			int[] tra = datamap.get(idx);//the trajectory data is read	
			long Time2 = System.nanoTime();
			runrecord.addIOTime((Time2-Time1)/1000000000.0);			
			double min_dist = Double.MAX_VALUE;
			int min_id = 1;		
			double [] bounds = null;
			if(yinyang) {//create the bounds for pruning in the first iterations.
				bounds = new double[groupnumber+2];
				Arrays.fill(bounds, Double.MAX_VALUE);
			}			
			if(!candidateofAllclusters.contains(idx)) {//if it is never contained by any list, we can assign it to the cluster with minimum length
				min_dist = Math.max(tra.length, minlength);
				min_id = min_length_id;
				if(yinyang) {// initialize the lower bound
					for (int j = 0; j < k; j++) {
						int length = clustData.get(j).length;
						double dist =  Math.max(tra.length, length);
						int groupNumber=centerGroup.get(j);
						if(j==min_length_id)
							continue;//jump this best value
						if(dist<bounds[groupNumber+2]) {
							bounds[groupNumber+2] = dist;
						}
					}
				}
			}else {
				for (int j = 0; j < k; j++) {
					Set<Integer> canlist = CENTERS.get(j).getCandidateList();// get the candidate list of each cluster
					double dist = 0;
					int[] clustra = clustData.get(j);
					if(!canlist.contains(idx))						// it is not contained
						dist = Math.max(tra.length, clustra.length);
					else {			
						dist = Intersection(tra, clustra, tra.length, clustra.length);
					}
					if (min_dist > dist) {
						min_dist = dist; // maintain the one with min distance
						min_id = j;
					}					
					if(yinyang) {// initialize the lower bound
						int groupid=centerGroup.get(j);
						if(dist<bounds[groupid+2]) {
							bounds[groupid+2] = dist;
						}
					}
				}
			}			
			if(yinyang) {// for initialize the bounds
				int groupid=centerGroup.get(min_id);
				bounds[groupid+2] = Double.MAX_VALUE;// set the optimal group as max distance as we do not need to need to consider this group
				bounds[0] = min_dist;// initialize the upper bound
				trajectoryBounds.put(idx, bounds);//the initial bound
			}
			ClusterPath newCluster = new_CENTERS.get(min_id);
			Time1 = System.nanoTime();
			newCluster.updateHistorgramGuava(tra, idx); //update the edge histogram using every new trajectory
			Time2 = System.nanoTime();
			runrecord.addHistorgramTime((Time2-Time1)/1000000000.0);							
			newCluster.addTrajectoryToCluster(idx);	// update the new trajectory to this cluster.
		}
		return new_CENTERS;
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
	 * single k-path operation
	 */
	public double singleKpath(int k, double overallDis, boolean yinyang, int groupnumber, String folder, 
			Set<Integer> candidateset) {
		if(yinyang)
			printCluterTraID(k, 1, folder);
		PRE_CENS = new ArrayList<ClusterPath>(CENTERS);		//maintain current centers for judging convergence						
		ArrayList<ClusterPath> new_CENTERS = new ArrayList<ClusterPath>(); // it stores the k clusters
		for(int i=0; i<k; i++) {
			ClusterPath newCluster = new ClusterPath(CENTERS.get(i).getClusterPath().getVIseries(), CENTERS.get(i).getTrajectoryID());
			new_CENTERS.add(newCluster);
		}
		long startTime1 = System.nanoTime();
		CENTERS = assignRebuildInvertedindex(k, new_CENTERS, yinyang, groupnumber, candidateset);	//update the CENTERS
		long endtime = System.nanoTime();
		runrecord.addAssignmentTime((endtime-startTime1)/1000000000.0);
		
		long startTime = System.nanoTime();
		for(int i=0; i<k; i++) {// generate the new centroid for each cluster
			if(graphPathExtraction)
				CENTERS.get(i).extractNewPathFrequency(forwardGraph, backwardGraph, i);// test the optimal		
			else {
				CENTERS.get(i).extractNewPathGuava(datamap, runrecord, traLength, trajectoryHistogram); 
			}
			overallDis += CENTERS.get(i).getSumDistance();
		}
		endtime = System.nanoTime();
		runrecord.addRefinementTime((endtime-startTime)/1000000000.0);
		
		if(yinyang)
			System.out.println("iteration 1, the sum distance is "+overallDis+", time cost: "+(endtime-startTime1)/1000000000.0+"s\n");		
		return overallDis;
	}
	
	/*
	 * conduct the clustering, we are using the Lloyd's algorithm
	 */
	public int kPath(int k, String folder) {
		int t = 0;
		for(; t < TRY_TIMES; t++){
			printCluterTraID(k, t, folder);
			double overallDis = 0;
			overallDis = singleKpath(k, overallDis, false, 0, folder, datamap.keySet());
			System.out.println("iteration "+(t+1)+", the sum distance is "+overallDis);
			if(timeToEnd()) {
				System.out.println("\nIteration stops now");
				runrecord.setIterationtimes(t+1);
				break;//convergence
			}
		}
		return t;
	}
	
	public void testStreamkPath() {
		StreamKpath datareading = new StreamKpath("dataReading");
		datareading.start();
		Yinyang streamkpath = new Yinyang("kpath");
		streamkpath.start();
	}

	
	public void init() throws IOException {
		CENTERS = new ArrayList<ClusterPath>();	
		interMinimumCentoridDis = new double[k];
		innerCentoridDis = new double[k][];
		datamap = new HashMap<>();// a btree map for easy search is created or read		
		traLength = new HashMap<>();
		edgeInfo = new HashMap<>();	
		edgeIndex = new HashMap<>();
		edgeHistogram = new HashMap<>();
		trajectoryHistogram = new HashMap<>();
		loadData(datafile, trajectoryNumber, edgefile);	// load the data and create index
		createTrajectoryHistogram(datamap, trajectoryNumber);  // build inverted index if there is not index
		
		Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("DDHHmmss");
        folder = sdf.format(cal.getTime());
	}

}
