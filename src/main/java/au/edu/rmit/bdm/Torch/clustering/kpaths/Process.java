package au.edu.rmit.bdm.Torch.clustering.kpaths;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import au.edu.rmit.bdm.Torch.base.Instance;
import au.edu.rmit.bdm.Torch.clustering.TrajectoryMtree;

/*
 * static kpath is used for clustering all the taxi trips, so we can find the k-paths for designing bus routes, this is not a real time problem, so we can 
 */
public class Process extends Thread {
	// stores the clusters
	protected static ArrayList<ClusterPath> CENTERS ; // it stores the k clusters
	static ArrayList<ClusterPath> PRE_CENS; // it stores the previous k clusters
	
	// the parameters
	protected static int TRY_TIMES = Integer.valueOf(LoadProperties.load("try_times"));//iteration times
	static String mapv_path = LoadProperties.load("vis_path");
	static String mapv_path_traclu_sigmod07 = LoadProperties.load("TraClus");
	static int frequencyThreshold = Integer.valueOf(LoadProperties.load("frequencyThreshold"));
	static int streamingDuration = Integer.valueOf(LoadProperties.load("streamingDuration"));
	static int streamEdges = Integer.valueOf(LoadProperties.load("streamEdges"));
	protected static RunLog runrecord = new RunLog(); 
	static ArrayList<Integer> cluslist;
	static ArrayList<int[]> centroids;
	static int trajectoryNumber;// the number of trajectories in the dataset
	static String folder;
	protected static int k;
	static boolean dataEnough;
	static boolean dataOut;
	static boolean iterationStops;
	static boolean readingdata;
	static String datafile;
	static int traNumber;
	static String edgefile;
	static String graphfile;
	static int slidingwindow=10;//a window to control the inverted index, 
	
	//for Yinyang and bound computation
	protected static Map<Integer, double[]> trajectoryBounds = null;// build a lower bound list with all groups and upper bound for each bdm, 0: upper bound, 1: global lower bound, 2~: lower bound with all different group
	static double[][] trajectoryBoundA =null;
	protected static Map<Integer, ArrayList<Integer>> group = null;// group id, centers id belong to this group
	static Map<Integer, Integer> centerGroup = null;//center id, group id
	
	protected static double[][] innerCentoridDis;//stores the distance between every two centorids
	protected static double[] interMinimumCentoridDis;//store the distance to nearest neighbor of each centorid
	
	//for storage
	protected static Map<Integer, int[]> datamap; // the bdm dataset
	protected static Map<Integer, Integer> traLength; // the bdm dataset
	protected static Map<Integer, Integer> trajectoryHistogram;//the histogram of each bdm
	protected static Map<Integer, ArrayList<Integer>> edgeIndex;// the index used for similarity search
	protected static Map<Integer, Integer> edgeHistogram;// the index used for similarity search
	protected static Map<Integer, String> edgeInfo;// the points information
	static Map<Integer, Integer> edgeType;

	static Map<Integer, Integer> Search2ClusterLookup;
	static Map<Integer, Integer> Cluster2SearchLookup;
	
	//for graph
	protected static HashMap<Integer, ArrayList<Integer>> forwardGraph = new HashMap<Integer, ArrayList<Integer>>();//the linked edge whose start is the end of start
	protected static HashMap<Integer, ArrayList<Integer>> backwardGraph = new HashMap<Integer, ArrayList<Integer>>();//the linked edge whose start is the end of start
	protected static ArrayList<int[]> centoridData = new ArrayList<>();//initialize the centroid
	static HashMap<String, Integer> road_types;
	
	//Mtree index 
	static TrajectoryMtree mindex = new TrajectoryMtree();
	static boolean mtreebuild = false;
	static boolean graphPathExtraction = false;// a sign used to set whether we use the optimization
	
	public Process(String datapath) {
		trajectoryNumber=0;
	}
	
	// stop the iteration when the clusters do not change compared with last time
	protected static boolean timeToEnd() {
	//	if (PRE_CENS == null)
	//		return false;
		for (ClusterPath cc : Process.CENTERS) {
			if(cc.getCenterChanged()==true) {
				return false;
			}
		}
		return true;
	}

	public static void loadData(String trajEdgeReprepFilePath, int number, String RawEdgePath) throws IOException{
		int idx=0;
		int gap = number/k;
		Random rand = new Random();
		int counter = 100;
		readRoadNetwork(RawEdgePath);
		try {
			Scanner in = new Scanner(new BufferedReader(new FileReader(trajEdgeReprepFilePath)));
			while (in.hasNextLine()) {// load the bdm dataset, and we can efficiently find the bdm by their id.
				String str = in.nextLine();
				String strr = str.trim();
				String[] abc = strr.split("\t");
				Integer id = Integer.parseInt(abc[0]);
				Cluster2SearchLookup.put(idx, id);
				Search2ClusterLookup.put(id, idx);
				String[] vertexSeries = abc[1].split(",");
				int[] vertexes = new int[vertexSeries.length];
				for(int t=0; t < vertexSeries.length; t++) {
					vertexes[t] = Integer.valueOf(vertexSeries[t]);
					int edgeID = vertexes[t];
					if(edgeIndex.containsKey(edgeID)) {
						ArrayList<Integer> lists = edgeIndex.get(edgeID);
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
					if(idx==counter && centoridData.size()<k) {// initialize the centroid
						centoridData.add(vertexes);
						System.out.print(vertexes.length+", ");
					//	counter += rand.nextInt(gap);
						counter += 100;
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
		System.out.println("the bdm dataset is loaded");
	//	System.out.println("the M-tree is built");
		System.out.println("the frequency histogram of edge is built");
		System.out.println("the inverted index of edge is built");

		if(mtreebuild) {
			mindex.buildHistogram();//build the histogram
		}

	}

	static void readRoadNetwork(String rawEdgePath) {
		road_types = new HashMap<>();
		edgeType = new HashMap<>();
		int type=0;
		try {
			Scanner in = new Scanner(new BufferedReader(new FileReader(rawEdgePath)));
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
	static void concludeCenter() {
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
	public static void createTrajectoryHistogram(Map<Integer, int[]> datamap, int trajectoryNumber) {	
		//compute the frequency for each bdm
		for(int idx:datamap.keySet()) {	//scan each bdm
			int[] tra = datamap.get(idx);
			int tra_fre =0;
			for(int t=0; t<tra.length; t++) {	//scan each edge
				tra_fre += edgeHistogram.get(tra[t]); //the frequency is the sum of edge frequency in each bdm.
			}
			trajectoryHistogram.put(idx, tra_fre);
		}		
		System.out.println("the frequency histogram of trajectories is built");
		System.out.println("==============================================================\n");
	}

	
	/*
	 * initialize the k clusters by randomly choosing from existing trajectories
	 */
	static void initializeClustersRandom(int k) {
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
	static void initializeClustersIncrease(int k, int delta) {
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
	static void initializeClustersIncrease1(int k, int delta) {
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
	static void initializeClustersHighFrequency(int k, int range) {
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
	 *  print the cluster ids and generate the bdm into file for mapv visualization
	 */
	public static int[] printCluterTraID(int k, int iteration, String folder) {		
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
	 *  print the cluster ids and generate the bdm into file for mapv visualization
	 */
	public static void printCluterTrajectory(int k, int iteration, String folder) {		
		String output = mapv_path+folder+"\\"+Integer.toString(iteration);
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
	public static ArrayList<ClusterPath> assignRebuildInvertedindex(int k, ArrayList<ClusterPath> new_CENTERS, boolean yinyang, 
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
		for (int idx:candidateset) {
			long Time1 = System.nanoTime();
			int[] tra = datamap.get(idx);//the bdm data is read
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
						dist = Yinyang.computeRealDistance(tra, clustra, idx);
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
			newCluster.updateHistorgramGuava(tra, idx); //update the edge histogram using every new bdm
			Time2 = System.nanoTime();
			runrecord.addHistorgramTime((Time2-Time1)/1000000000.0);							
			newCluster.addTrajectoryToCluster(idx);	// update the new bdm to this cluster.
		}
		return new_CENTERS;
	}
	
	/*
	 * single k-path operation
	 */
	public static double singleKpath(int k, double overallDis, boolean yinyang, int groupnumber, String folder, 
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
	public static void kPath(int k, String folder) {
		for(int t = 0; t < Process.TRY_TIMES; t++){
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
	}
	
	public static void testStreamkPath() {
		StreamKpath datareading = new StreamKpath("dataReading");
		datareading.start();
		Yinyang streamkpath = new Yinyang("kpath");
		streamkpath.start();
	}
	
	public static void staticKpath() throws IOException {
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
        String new_folder = mapv_path+folder;
        boolean creat_folder = new File(new_folder).mkdirs();
        if(!creat_folder)
        	return;
        mindex.buildGraph(graphfile, forwardGraph, backwardGraph);
        int iterations=0;
        if(mtreebuild) {
        	mindex.runkpath(folder);
        }else {
        	initializeClustersRandom(k); 		// initialize the kmeans in the first iteration
        //	initializeClustersIncrease(k, 100);
        	// initializeClustersHighFrequency(k, 1000);
        	// kPath(k, folder); // start the kpath clustering

        	iterations = Yinyang.yinyangkPath(k, folder, datamap.keySet());// here you can change datamap.keySet() to the result set got from range query

        //	concludeCenter();
        }
	//	Histogram.SIGMOD07FrequentEdgesMapv(edgeHistogram, edgeInfo, frequencyThreshold, mapv_path_traclu_sigmod07+Integer.toString(frequencyThreshold));
		runrecord.printLog();
		if(graphPathExtraction){
			System.out.println("\nThe final centroid bdm length are:");
			printCluterTrajectory(k, iterations+1, folder);
		}else {
			System.out.println("\nThe final centroid bdm ids are:");
			int []result = printCluterTraID(k, iterations+1, folder);
		}
        
	}

	public static int[] clustering(Set<Integer> trajIds) throws IOException {

	    Set<Integer> transformed = new HashSet<>();
	    for (Integer trans: trajIds)
	        transformed.add(Search2ClusterLookup.get(trans));


		initializeClustersRandom(k); 		// initialize the kmeans in the first iteration
		int iterations = Yinyang.yinyangkPath(k, folder, transformed);// here you can change datamap.keySet() to the result set got from range query
		runrecord.printLog();
		System.out.println("\nThe final centroid bdm ids are:");
		int[] results = printCluterTraID(k, iterations+1, folder);

        for (int i = 0; i < results.length; i++){
            results[i] = Cluster2SearchLookup.get(results[i]);
        }

        return results;
	}

	public static void init() throws IOException {
		datafile = Instance.fileSetting.TRAJECTORY_EDGE_REPRESENTATION_PATH_200000;
		k = 20;
		trajectoryNumber = 200000;
		edgefile = Instance.fileSetting.ID_EDGE_RAW;
		graphfile = Instance.fileSetting.ID_EDGE_LOOKUP;

		CENTERS = new ArrayList<ClusterPath>();
		interMinimumCentoridDis = new double[k];
		innerCentoridDis = new double[k][];
		datamap = new HashMap<>();// a btree map for easy search is created or read
		traLength = new HashMap<>();
		edgeInfo = new HashMap<>();
		edgeIndex = new HashMap<>();
		edgeHistogram = new HashMap<>();
		trajectoryHistogram = new HashMap<>();
		Search2ClusterLookup = new HashMap<>();
		Cluster2SearchLookup = new HashMap<>();
		loadData(datafile, trajectoryNumber, edgefile);	// load the data and create index
		createTrajectoryHistogram(datamap, trajectoryNumber);  // build inverted index if there is not index
		mindex.buildGraph(graphfile, forwardGraph, backwardGraph);

	}

	public static void main(String[] args) throws IOException {
		init();
		clustering(datamap.keySet());
	}
}
