package au.edu.rmit.bdm.clustering.mtree;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import au.edu.rmit.bdm.clustering.trajectory.kpaths.ClusterPath;
import au.edu.rmit.bdm.clustering.trajectory.kpaths.Util;
import au.edu.rmit.bdm.clustering.trajectory.kpaths.Yinyang;
import scala.collection.generic.BitOperations.Int;


/**
 * The main class that implements the M-Tree.
 *
 * @param <DATA> The type of data that will be indexed by the M-Tree. Objects of
 *        this type are stored in HashMaps and HashSets, so their
 *        {@code hashCode()} and {@code equals()} methods must be consistent.
 */
public class MTree<DATA> {

	/**
	 * The type of the results for nearest-neighbor queries.
	 */
	public class ResultItem {
		private ResultItem(DATA data, double distance) {
			this.data = data;
			this.distance = distance;
		}

		/** A nearest-neighbor. */
		public DATA data;

		/** 
		 * The distance from the nearest-neighbor to the query data object
		 * parameter.
		 */
		public double distance;
	}
	
	
	// Exception classes
	private static class SplitNodeReplacement extends Exception {
		// A subclass of Throwable cannot be generic.  :-(
		// So, we have newNodes declared as Object[] instead of Node[].
		private Object newNodes[];
		
		private SplitNodeReplacement(Object... newNodes) {
			this.newNodes = newNodes;
		}
	}
	
	private static class RootNodeReplacement extends Exception {
		// A subclass of Throwable cannot be generic.  :-(
		// So, we have newRoot declared as Object instead of Node.
		private Object newRoot;
		
		private RootNodeReplacement(Object newRoot) {
			this.newRoot = newRoot;
		}
	}

	
	private static class NodeUnderCapacity extends Exception { }
	

	private static class DataNotFound extends Exception { }
	
	/*
	 * the leaf node comes from the kpaths running.
	 * input: pivot trajectory, neighbor trajectories.
	 */
	public void addLeafNode() {
		// construct the leaf node using the input
		// add the leaf node to the tree
		// return the root nodes
	}
	
	/*
	 * run the k-path to group, and choose the centroid, and update the radius for group pruning,
	 */
	public void runKpathsToGroup(int centroid, ArrayList<int[]> tralist) {
		// call the addLeafNode when found a good group which has a small radius and many uninserted trajectories.
		// update the radius of inserted parental nodes
	}
	
	/*
	 * the function will build the length and edge histogram for every internal node
	 * this will be called after the tree is constructed
	 */
	public void buildHistogram(Node node) {
		node.edgeOcc = HashMultiset.create();
		node.lengthOcc = HashMultiset.create();
		if(node instanceof MTree.LeafNode){//for the objects
			System.out.println(node.radius);
			for(DATA centerdata : node.children.keySet()) {
				IndexItem child = node.children.get(centerdata);
				int []data = MTree.this.distanceFunction.getData(child.data);
				for(int edgeid: data)
					node.edgeOcc.add(edgeid);
				node.lengthOcc.add(data.length);
			}

		}else if(node instanceof MTree.InternalNode || node instanceof MTree.RootNode){
			for(DATA centerdata : node.children.keySet()) {
				IndexItem child = node.children.get(centerdata);
				Node childnode = (Node)child;
				buildHistogram(childnode);
				node.lengthOcc.addAll(childnode.lengthOcc);
				node.edgeOcc.addAll(childnode.edgeOcc);
			}
		}
	}
	
	/*
	 * the M-tree is too slow to build, we write it to the files after the first construction
	 */
	public void writeMtreetoFile(Node node, int level, int fartherid, String folder) {
		String filename = folder+"/"+Integer.toString(level)+".mtree";
		String content = node.radius+":"+fartherid+";"+node.distanceToParent+";";
		if(node instanceof MTree.LeafNode){//for the objects
			System.out.println(node.radius);
			for(DATA centerdata : node.children.keySet()) {
				IndexItem child = node.children.get(centerdata);
				int idx =  MTree.this.distanceFunction.getID(child.data);
				content += Integer.toString(idx)+",";
			}
//			content += Arrays.toString(node.getEdgeOcc());
			Util.write(filename, content+"\n");
		}else if(node instanceof MTree.InternalNode || node instanceof MTree.RootNode){
			for(DATA centerdata : node.children.keySet()) {
				IndexItem child = node.children.get(centerdata);
				int idx =  MTree.this.distanceFunction.getID(child.data);
				Node childnode = (Node)child;
				writeMtreetoFile(childnode, level+1, idx, folder);
				content += Integer.toString(idx)+","; //write to the internal node file
			}
			Util.write(filename, content+"\n");
		}
	}
	
	/*
	 * build the HM-tree from files, and the data set to form a index exact the same with before
	 */
	public void rebuildMtreeFromFile(String path, int level, ArrayList<Node> nodes, Map<Integer, int[]> datamap) {
		try {
			Scanner in = new Scanner(new BufferedReader(new FileReader(path)));					
			while (in.hasNextLine()) {
				String str = in.nextLine();
				String strr = str.trim();
				String[] abc = strr.split(":");
				String[] radius_data = abc[1].split(";");
				double radius = Double.valueOf(radius_data[0]);
				double distanceToFarther = Double.valueOf(radius_data[1]);
				String[] traids = radius_data[2].split(",");
				for(String traidStr : traids) {
					int traid = Integer.valueOf(traidStr);
					// read datamap
				}
				//read the leaf node file first
				//read the internal node file
			}
			in.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	
	/*
	 * read the graph file to construct the linked edges
	 */
	public void buildGraph(String edgePath,  HashMap<Integer, ArrayList<Integer>> forwardGraph,  HashMap<Integer, ArrayList<Integer>> backwardGraph) {
		Map<Integer, ArrayList<Integer>> aMap = new HashMap<>();
		Map<Integer, ArrayList<Integer>> bMap = new HashMap<>();
		try {
			Scanner in = new Scanner(new BufferedReader(new FileReader(edgePath)));
			while (in.hasNextLine()) {		// load the geo-information of all the edges in the graph
				String str = in.nextLine();
				String strr = str.trim();
				String[] abc = strr.split(";");
				int edge = Integer.valueOf(abc[0]);
				if(!abc[1].equals("")) {					
					int vertex1 = Integer.valueOf(abc[1]);
					ArrayList<Integer> edgeids;
					if (aMap.containsKey(vertex1)) {
						edgeids = aMap.get(vertex1);
					} else {
						edgeids = new ArrayList<>();
					}
					edgeids.add(edge);
					aMap.put(vertex1, edgeids);
				}
				if (!abc[2].equals("")) {
					int vertex2 = Integer.valueOf(abc[2]);
					ArrayList<Integer> edgeids;
					if (bMap.containsKey(vertex2)) {
						edgeids = bMap.get(vertex2);
					} else {
						edgeids = new ArrayList<>();
					}
					edgeids.add(edge);
					bMap.put(vertex2, edgeids);
				}
			}
			in.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		try {
			Scanner in = new Scanner(new BufferedReader(new FileReader(edgePath)));
			while (in.hasNextLine()) {		// load the geo-information of all the edges in the graph
				String str = in.nextLine();
				String strr = str.trim();
				String[] abc = strr.split(";");
				int edge = Integer.valueOf(abc[0]);
				if(!abc[1].equals("")) {
					int vertex1 = Integer.valueOf(abc[1]);
					backwardGraph.put(edge, bMap.get(vertex1));
				}
				if(!abc[2].equals("")) {
					int vertex2 = Integer.valueOf(abc[2]);
					forwardGraph.put(edge, aMap.get(vertex2));
				}				
			}
			in.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * run the kpath on the M-tree
	 */
	public void runkpath(String folder) {
		//if index file exists, we read all the leaf nodes, otherwise rebuild
		KMeansAssignment assignment = new KMeansAssignment("");
		assignment.runKpath(folder);
	}
	
	
	public class KMeansAssignment extends Yinyang{
		ArrayList<Set<IndexItem>> candiList;
		private PriorityQueue<ItemWithDistances<Node>> pendingQueue = new PriorityQueue<ItemWithDistances<Node>>();
		public KMeansAssignment(String datapath) {
			super(datapath);
			pendingQueue.add(new ItemWithDistances<Node>(root, null, 0));//start from the root
			candiList = new ArrayList<>();
		}
		
		public KMeansAssignment(String datapath, String leafPath) {
			super(datapath);
			try {
				Scanner in = new Scanner(new BufferedReader(new FileReader(leafPath)));					
				while (in.hasNextLine()) {
					Node newleaf = null;
					// read the file and enqueue all the leaf nodes to the queue.
					pendingQueue.add(new ItemWithDistances<Node>(newleaf, null, 0));//start from the root
				}
				in.close();
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			candiList = new ArrayList<>();
		}		
		
		/*
		 * compute the drift between current center and previous center
		 */
		protected void computeGroupDrift(int k, int t) {
			group_drift = new double[t];
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
		 * compare the bound distance
		 */
		public class ItemWithDistances <U> implements Comparable<ItemWithDistances<U>> {
			private U item;
			private double [] distance;//maybe we can change it to be an array to put an bound,
			private double minDistance;

			public ItemWithDistances(U item, double []distance, double minDistance) {
				this.item = item;
				this.distance = distance;
				this.minDistance = minDistance;
			}

			@Override
			public int compareTo(ItemWithDistances<U> that) {//the comparison for the queue
				if(this.minDistance < that.minDistance) {
					return -1;
				} else if(this.minDistance > that.minDistance) {
					return +1;
				} else {
					return 0;
				}
			}
		}
			
		double minUpperBound;
		int minId;
		
		/*
		 * the main running function
		 */
		public void runKpath(String folder) {
			int groupNumber = k;
			//divide the k centroids into t groups when k is big
			center_drift = new HashMap<Integer, Double>();
			groupInitialClusters(groupNumber, k);
			assignmentNormal(centoridData);
			double overallDis = pathExtractionHistogram(forwardGraph, backwardGraph, centoridData);
			System.out.println("iteration 0, the sum distance is "+overallDis);
			computeGroupDrift(k, groupNumber);
			int t = 1;		
			for(; t < TRY_TIMES; t++){
				printCluterTrajectory(k, t, folder);
				assignmentWithPrevious(centoridData);
				overallDis = pathExtractionHistogram(forwardGraph, backwardGraph, centoridData);
				computeGroupDrift(k, groupNumber);
				System.out.println("iteration "+(t+1)+", the sum distance is "+overallDis);
				if(timeToEnd()) {
					System.out.println("\nIteration stops now, the final centers are:");
					runrecord.setIterationtimes(t+1);
					break;//convergence
				}
			}
			printCluterTrajectory(k, t+1, folder);
		}
		
		/*
		 * conduct the assignment, find all the leaf nodes we can use the centroids to prune
		 * k is the number of assigment, 
		 * we can choose some centers in node to represent the centroid in the initial stage
		 */
		public void assignmentNormal(ArrayList<int[]> centoridData) {
			long time1 = System.nanoTime();
			for(int i=0; i<k; i++) {
				Set<IndexItem> aNodes = new HashSet<IndexItem>();//store the candidate indexitem
				candiList.add(aNodes);
			}
			Set<Integer> candidateofAllclusters = new HashSet<Integer>();
			int minlength = Integer.MAX_VALUE;
			int min_length_id=0;
			System.out.println(centoridData.size());
			for (int j = 0; j < k; j++) {
				Set<Integer> candilist = CENTERS.get(j).creatCandidateListNoDatamap(edgeIndex, centoridData.get(j));//generate the candidate list
				candidateofAllclusters.addAll(candilist);// merge it to a single list
				int length = centoridData.get(j).length;
				if(length<minlength) {// get the minimum length
					minlength = length;
					min_length_id = j;
				}
			}		
			while(!pendingQueue.isEmpty()){
				ItemWithDistances<Node> pending = pendingQueue.poll();
				Node node = pending.item;				
				for(DATA centerdata : node.children.keySet()) {
					IndexItem child = node.children.get(centerdata);
					int idx = MTree.this.distanceFunction.getID(child.data);// used for inverted index
					int []data = MTree.this.distanceFunction.getData(child.data);
					if(child instanceof MTree.Entry){//the data
						double min_dist = Double.MAX_VALUE;
						int minId=0;
						child.bounds = new double[k];
						if (!candidateofAllclusters.contains(idx)) {// if it is never contained by any list
							min_dist = Math.max(data.length, minlength);							
							for (int j = 0; j < k; j++) {
								int length = centoridData.get(j).length;
								double dist = Math.max(data.length, length);
								if (j == min_length_id) {
									child.bounds[j] = Double.MAX_VALUE;
									continue;// jump this best value
								}
								child.bounds[j] = dist;
							}
						} else {
							for (int j = 0; j < k; j++) {
								Set<Integer> canlist = CENTERS.get(j).getCandidateList();// get the candidate list of each cluster
								double dist = 0;
								int[] clustra = centoridData.get(j);
								if (!canlist.contains(idx)) // it is not contained
									dist = Math.max(data.length, clustra.length);
								else {
									dist = Intersection(data, clustra, data.length, clustra.length);
								}
								if (min_dist > dist) {
									min_dist = dist; // maintain the one with min distance
									minId = j;
								}
								child.bounds[j] = dist;
							}
							child.bounds[minId] = Double.MAX_VALUE;
						}
						candiList.get(minId).add(child);//add the trajectory into the list of cluster
						ClusterPath aClusterPath = CENTERS.get(minId);
						aClusterPath.updateHistorgramGuava(data, 0);//histogram used for
					}else {// the node
						Node childNode = (Node)child;//if this node cannot be pruned, we will further enqueue this to the queue with the bounds												
						if(assignNode(childNode, centoridData, data, child.radius)) {//pruned
							candiList.get(minId).add(child);
							ClusterPath aClusterPath = CENTERS.get(minId);
							aClusterPath.updateHistorgramGuava(childNode.edgeOcc,childNode.lengthOcc);
						}else {		
							
							pendingQueue.add(new ItemWithDistances<Node>(childNode, childNode.bounds, minUpperBound));
						}
					}
				}
			}
			long time2 = System.nanoTime();
			runrecord.addAssignmentTime((time2-time1)/1000000000.0);
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
		 * compute with every centroid and see whether we need to further check its children, this can be shared by all assignment
		 */
		public boolean assignNode(Node childNode, ArrayList<int[]> centoridData, int []data, double radius) {
			childNode.bounds = new double[k];
			int i=0;
			minId = 0;
			minUpperBound = Double.MAX_VALUE;
			double smallest=Double.MAX_VALUE;
			double secondsmallest=Double.MAX_VALUE;	//get the second minimum 
			for(int[] centroid: centoridData) {
				double upperBound = (double)Intersection(centroid, data, centroid.length, data.length) + radius;
				if(upperBound<minUpperBound) {
					minUpperBound = upperBound;
					minId = i;
				}
				System.out.println("aa"+k+" "+childNode.bounds.length);
				childNode.bounds[i] = (double) Intersection(centroid, data, centroid.length, data.length) - radius;
				if(childNode.bounds[i]<smallest){
				    secondsmallest=smallest;
				    smallest=childNode.bounds[i];
				}else if(childNode.bounds[i]<secondsmallest){
				    secondsmallest=childNode.bounds[i];
				}
				i++;
			}
			if(secondsmallest>=minUpperBound)
				return true;
			else
				return false;
		}
		
		/*
		 * find the minimum one as the group bound
		 */
		public double getMinimumLowerboundInNode(double [] bounds, int groupNumber, int owngroup) {
			double lowerboud = Double.MAX_VALUE;
			for(int group_j=0; group_j<groupNumber; group_j++) {//get the minimum lower bound of all group
				if(group_j == owngroup)
					continue;
				double lowerboud_temp = Math.abs(bounds[group_j] - group_drift[group_j]);
				if(lowerboud_temp<lowerboud)
					lowerboud = lowerboud_temp;
			}
			return lowerboud;
		}
		
		/*
		 * update the trajectories in each clusters
		 */
		public void updateCentersNew(Map<Integer, ArrayList<IndexItem>> idxNeedsIn, Map<Integer, ArrayList<IndexItem>> idxNeedsOut) {
			long Time1 = System.nanoTime();
			for(int idx: idxNeedsIn.keySet()) {
				ArrayList<IndexItem> idxs = idxNeedsIn.get(idx);
				candiList.get(idx).addAll(idxs);
			}
			for(int idx: idxNeedsOut.keySet()) {
				ArrayList<IndexItem> idxs = idxNeedsOut.get(idx);
				candiList.get(idx).removeAll(idxs);
			}
			long Time2 = System.nanoTime();
			runrecord.addHistorgramTime((Time2-Time1)/1000000000.0);			
		}
		
		/*
		 * when we have the previous assignment and update the histograms
		 */
		public void assignmentWithPrevious(ArrayList<int[]> centoridData) {
			long time1 = System.nanoTime();
			Set<Integer> candidateofAllclusters = new HashSet<Integer>();
			Map<Integer, int[]> clustData = new HashMap<Integer, int[]>();
			Map<Integer, ArrayList<IndexItem>> idxNeedsIn = new HashMap<>();//it stores all the idxs of trajectories that move in
			Map<Integer, ArrayList<IndexItem>> idxNeedsOut = new HashMap<>();
			int centerMinlength = Integer.MAX_VALUE;
			int minLengthCenterid=0;			
			long startTime2 = System.nanoTime();								
			for (int j = 0; j < k; j++) {
				long startTime1 = System.nanoTime();
				int[] clustra = centoridData.get(j);
				Set<Integer> candilist = CENTERS.get(j).creatCandidateListNoDatamap(edgeIndex, clustra);//generate the candidate list			
				Collections.addAll(candidateofAllclusters, candilist.toArray(new Integer[0]));
				long endtime1 = System.nanoTime();
				runrecord.addIOTime((endtime1-startTime1)/1000000000.0);
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
			for (int group_i = 0; group_i < k; group_i++) {//check each group
				int centerID = group_i;
				{//check each center in the group
					int center_length = clustData.get(centerID).length;			
					Set<IndexItem> tralist = candiList.get(centerID);				
					for (IndexItem child : tralist) { // check every trajectory in the center to assign which integrate the group filtering and local filtering				
						int idx = MTree.this.distanceFunction.getID(child.data);
						int []tra = MTree.this.distanceFunction.getData(child.data);
						if(child instanceof MTree.Entry){							
						int tralength = tra.length; // the length of trajectory is read
						double min_dist = Double.MAX_VALUE;// to record the best center's distance
						int newCenterId = centerID;//initialize as the original center
						if (!checkInvertedIndex(candidateofAllclusters, idx)) { // if it is never contained by any list, we can assign it to the cluster with minimum length
							min_dist = Math.max(tralength, centerMinlength);
							double min_dist1 = Math.max(tralength, center_length);
							if(min_dist1>min_dist) {//change to other center 	
								newCenterId = minLengthCenterid;							
							}
							indexFil+=k;
						} else {//check whether we need to change the center by comparing the bounds						
							double [] bounds = child.bounds;
							double lowerbound = getMinimumLowerboundInNode(bounds, k, centerID);	// bound from drift					
							Set<Integer> canlist = CENTERS.get(centerID).getCandidateList();	
							int[] clustra = clustData.get(centerID);
							if (checkInvertedIndex(canlist, idx)) {
								min_dist = computeRealDistance(tra, clustra, idx);//compute the distance with new center						
							}else {// do not need to read as no overlap
								min_dist = Math.max(tralength, clustra.length);
							}
							double newupperbound = min_dist;// tighten the upper bound
							newCenterId = centerID;
							double centroidBound = interMinimumCentoridDis[centerID]/2.0;
							lowerbound = Math.max(lowerbound, centroidBound);
							if(lowerbound < newupperbound){//cannot not pass the group filtering
								for(int group_j=0; group_j<k; group_j++) {
									if( group_j == group_i)//skip current group
										continue;
									double localbound = Math.max((bounds[group_j]-group_drift[group_j]), innerCentoridDis[centerID][group_j]/2.0);
									if( localbound < min_dist) {//the groups that cannot pass the filtering of bound and inverted index									
										double second_min_dist_local = Double.MAX_VALUE;
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
										child.bounds[group_j] = second_min_dist_local;
									}else {
										numFilGroup++;
										child.bounds[group_j] = bounds[group_j] - group_drift[group_j];										
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
							ArrayList<IndexItem> idxlist;
							if(idxNeedsIn.containsKey(newCenterId))
								idxlist = idxNeedsIn.get(newCenterId);
							else
								idxlist = new ArrayList<IndexItem>();
							idxlist.add(child);
							idxNeedsIn.put(newCenterId, idxlist);// temporal store as we cannot add them the trajectory list which will be scanned later, batch remove later
							if(idxNeedsOut.containsKey(centerID))
								idxlist = idxNeedsOut.get(centerID);
							else
								idxlist = new ArrayList<IndexItem>();
							idxlist.add(child);
							idxNeedsOut.put(centerID, idxlist);// temporal store, batch remove later							
							accumulateHistogramGuava(tra, idx, newCenterId, centerID);	// update the histogram directly
							long Time2 = System.nanoTime();
							runrecord.addHistorgramTime((Time2-Time1)/1000000000.0);
						}
					}else {
						Node childNode = (Node)child;//if this node cannot be pruned, we will further enqueue this to the queue with the bounds												
						ClusterPath bClusterPath = CENTERS.get(centerID);
						if(assignNode(childNode, centoridData, tra, child.radius)) {	// if the histogram is not equal to the original centroids, we move 
							System.out.println("bbbbbb");
							if(centerID!=minId) {								
								ArrayList<IndexItem> idxlist;
								if(idxNeedsIn.containsKey(minId))
									idxlist = idxNeedsIn.get(minId);
								else
									idxlist = new ArrayList<IndexItem>();
								idxlist.add(child);
								idxNeedsIn.put(minId, idxlist);// temporal store as we cannot add them the trajectory list which will be scanned later, batch remove later
								if(idxNeedsOut.containsKey(centerID))
									idxlist = idxNeedsOut.get(centerID);
								else
									idxlist = new ArrayList<IndexItem>();
								idxlist.add(child);
								idxNeedsOut.put(centerID, idxlist);// temporal store, batch remove later
								
								ClusterPath aClusterPath = CENTERS.get(minId);
								bClusterPath.removeHistorgramGuava(childNode.edgeOcc,childNode.lengthOcc);
								aClusterPath.updateHistorgramGuava(childNode.edgeOcc,childNode.lengthOcc);								
							}
						}else {
							ArrayList<IndexItem> idxlist;
							if(idxNeedsOut.containsKey(centerID))
								idxlist = idxNeedsOut.get(centerID);
							else
								idxlist = new ArrayList<IndexItem>();
							idxlist.add(child);
							idxNeedsOut.put(centerID, idxlist);// temporal store, batch remove later
							bClusterPath.removeHistorgramGuava(childNode.edgeOcc,childNode.lengthOcc);//remove the histogram from original histogram
							pendingQueue.add(new ItemWithDistances<Node>(childNode, childNode.bounds, minUpperBound));
						}
					}
					}
				}
			}
			long Time1 = System.nanoTime();		
			updateCentersNew(idxNeedsIn, idxNeedsOut);//add to the new
			long Time2 = System.nanoTime();
			runrecord.addHistorgramTime((Time2-Time1)/1000000000.0);
			assignmentNormal(centoridData);//assign the nodes in the pending queue using the same method
			System.out.println(movedtrajectory);
			long time2 = System.nanoTime();
			runrecord.addAssignmentTime((time2-time1)/1000000000.0);			
		}	
		
		/*
		 * choose the centers in the index as the centroid, randomly scan the tree to choose k centroids from node
		 */
		public void InitializeCentroids(Node rootnode, ArrayList<Int[]> centoridData) {
			//get all the levels, and do not need to access the 
			if(rootnode.getChildren().size()>k) {//choose the centroid randomly
				
			}else {
				//go deeper to check if there are more than k centroid 
			}
		}
		
		/*
		 * scan the histogram to choose the path in a fast way.
		 */
		public double pathExtractionHistogram(HashMap<Integer, ArrayList<Integer>> forwardGraph, 
				HashMap<Integer, ArrayList<Integer>> backwardGraph, ArrayList<int[]> centoridData) {
			long Time1 = System.nanoTime();
			double overallDis = 0;
			for(int i=0; i<k; i++) {
				ClusterPath aClusterPath = CENTERS.get(i);
				double drfit = aClusterPath.extractNewPathFrequency(forwardGraph, backwardGraph, i);
				updateCentroids(centoridData);
				center_drift.put(i, drfit);
				overallDis += CENTERS.get(i).getSumDistance();
			}
			long Time2 = System.nanoTime();
			runrecord.addRefinementTime((Time2-Time1)/1000000000.0);
			return overallDis;
		}

		private void updateCentroids(ArrayList<int[]> centoridData) {
			for(int i=0; i<k; i++) {
				centoridData.set(i, CENTERS.get(i).getTrajectoryData());
			}
		}
	}

	/**
	 * An {@link Iterable} class which can be iterated to fetch the results of a
	 * nearest-neighbors query.
	 * 
	 * <p>The neighbors are presented in non-decreasing order from the {@code
	 * queryData} argument to the {@link MTree#getNearest(Object, double, int)
	 * getNearest*()}
	 * call.
	 * 
	 * <p>The query on the M-Tree is executed during the iteration, as the
	 * results are fetched. It means that, by the time when the <i>n</i>-th
	 * result is fetched, the next result may still not be known, and the
	 * resources allocated were only the necessary to identify the <i>n</i>
	 * first results.
	 */
	public class Query implements Iterable<ResultItem> {

		public class ResultsIterator implements Iterator<ResultItem> {
			
			public class ItemWithDistances <U> implements Comparable<ItemWithDistances<U>> {
				private U item;
				private double distance;
				private double minDistance;

				public ItemWithDistances(U item, double distance, double minDistance) {
					this.item = item;
					this.distance = distance;
					this.minDistance = minDistance;
				}

				@Override
				public int compareTo(ItemWithDistances<U> that) {
					if(this.minDistance < that.minDistance) {
						return -1;
					} else if(this.minDistance > that.minDistance) {
						return +1;
					} else {
						return 0;
					}
				}
			}
			
			
			private ResultItem nextResultItem = null;
			private boolean finished = false;
			private PriorityQueue<ItemWithDistances<Node>> pendingQueue = new PriorityQueue<ItemWithDistances<Node>>();
			private double nextPendingMinDistance;
			private PriorityQueue<ItemWithDistances<Entry>> nearestQueue = new PriorityQueue<ItemWithDistances<Entry>>();
			private int yieldedCount;
			
			private ResultsIterator() {
				if(MTree.this.root == null) {
					finished = true;
					return;
				}
				
				double distance = MTree.this.distanceFunction.calculate(Query.this.data, MTree.this.root.data);
				double minDistance = Math.max(distance - MTree.this.root.radius, 0.0);
				
				pendingQueue.add(new ItemWithDistances<Node>(MTree.this.root, distance, minDistance));
				nextPendingMinDistance = minDistance;
			}
			
			
			@Override
			public boolean hasNext() {
				if(finished) {
					return false;
				}
				
				if(nextResultItem == null) {
					fetchNext();
				}
				
				if(nextResultItem == null) {
					finished = true;
					return false;
				} else {
					return true;
				}
			}
			
			@Override
			public ResultItem next() {
				if(hasNext()) {
					ResultItem next = nextResultItem;
					nextResultItem = null;
					return next;
				} else {
					throw new NoSuchElementException();
				}
			}
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			
			private void fetchNext() {
				assert !finished;
				
				if(finished  ||  yieldedCount >= Query.this.limit) {
					finished = true;
					return;
				}
				
				while(!pendingQueue.isEmpty()  ||  !nearestQueue.isEmpty()) {
					if(prepareNextNearest()) {
						return;
					}
					
					assert !pendingQueue.isEmpty();
					
					ItemWithDistances<Node> pending = pendingQueue.poll();
					Node node = pending.item;
					
					for(IndexItem child : node.children.values()) {
						if(Math.abs(pending.distance - child.distanceToParent) - child.radius <= Query.this.range) {
							double childDistance = MTree.this.distanceFunction.calculate(Query.this.data, child.data);
							double childMinDistance = Math.max(childDistance - child.radius, 0.0);
							if(childMinDistance <= Query.this.range) {
								if(child instanceof MTree.Entry) {
									@SuppressWarnings("unchecked")
									Entry entry = (Entry)child;
									nearestQueue.add(new ItemWithDistances<Entry>(entry, childDistance, childMinDistance));
								} else {
									@SuppressWarnings("unchecked")
									Node childNode = (Node)child;
									pendingQueue.add(new ItemWithDistances<Node>(childNode, childDistance, childMinDistance));
								}
							}
						}
					}
					
					if(pendingQueue.isEmpty()) {
						nextPendingMinDistance = Double.POSITIVE_INFINITY;
					} else {
						nextPendingMinDistance = pendingQueue.peek().minDistance;
					}
				}

				finished = true;
			}

			
			private boolean prepareNextNearest() {
				if(!nearestQueue.isEmpty()) {
					ItemWithDistances<Entry> nextNearest = nearestQueue.peek();
					if(nextNearest.distance <= nextPendingMinDistance) {
						nearestQueue.poll();
						nextResultItem = new ResultItem(nextNearest.item.data, nextNearest.distance);
						++yieldedCount;
						return true;
					}
				}
				
				return false;
			}
			
		}
		
		
		private Query(DATA data, double range, int limit) {
			this.data = data;
			this.range = range;
			this.limit = limit;
		}
		
		
		@Override
		public Iterator<ResultItem> iterator() {
			return new ResultsIterator();
		}

		
		private DATA data;
		private double range;
		private int limit;
	}


	
	/**
	 * The default minimum capacity of nodes in an M-Tree, when not specified in
	 * the constructor call.
	 */
	public static final int DEFAULT_MIN_NODE_CAPACITY = 50;


	protected int minNodeCapacity;
	protected int maxNodeCapacity;
	public DistanceFunction<? super DATA> distanceFunction;
	protected SplitFunction<DATA> splitFunction;
	protected Node root;
	
	
	/**
	 * Constructs an M-Tree with the specified distance function.
	 * @param distanceFunction The object used to calculate the distance between
	 *        two data objects.
	 */
	public MTree(DistanceFunction<? super DATA> distanceFunction,
			SplitFunction<DATA> splitFunction) {
		this(DEFAULT_MIN_NODE_CAPACITY, distanceFunction, splitFunction);
	}
	
	/**
	 * Constructs an M-Tree with the specified minimum node capacity and
	 * distance function.
	 * @param minNodeCapacity The minimum capacity for the nodes of the tree.
	 * @param distanceFunction The object used to calculate the distance between
	 *        two data objects.
	 * @param splitFunction The object used to process the split of nodes if
	 *        they are full when a new child must be added.
	 */
	public MTree(int minNodeCapacity,
			DistanceFunction<? super DATA> distanceFunction,
			SplitFunction<DATA> splitFunction) {
		this(minNodeCapacity, 2 * minNodeCapacity - 1, distanceFunction, splitFunction);
	}
	
	/**
	 * Constructs an M-Tree with the specified minimum and maximum node
	 * capacities and distance function.
	 * @param minNodeCapacity The minimum capacity for the nodes of the tree.
	 * @param maxNodeCapacity The maximum capacity for the nodes of the tree.
	 * @param distanceFunction The object used to calculate the distance between
	 *        two data objects.
	 * @param splitFunction The object used to process the split of nodes if
	 *        they are full when a new child must be added.
	 */
	public MTree(int minNodeCapacity, int maxNodeCapacity,
			DistanceFunction<? super DATA> distanceFunction,
			SplitFunction<DATA> splitFunction)
	{
		if(minNodeCapacity < 2  ||  maxNodeCapacity <= minNodeCapacity  ||
		   distanceFunction == null) {
			throw new IllegalArgumentException();
		}
		
		if(splitFunction == null) {
			splitFunction = new ComposedSplitFunction<DATA>(
					new PromotionFunctions.RandomPromotion<DATA>(),
					new PartitionFunctions.BalancedPartition<DATA>()
				);
		}
		
		this.minNodeCapacity = minNodeCapacity;
		this.maxNodeCapacity = maxNodeCapacity;
		this.distanceFunction = distanceFunction;
		this.splitFunction = splitFunction;
		this.root = null;
	}

	/**
	 * Adds and indexes a data object.
	 * 
	 * <p>An object that is already indexed should not be added. There is no
	 * validation regarding this, and the behavior is undefined if done.
	 * 
	 * @param data The data object to index.
	 */
	public void add(DATA data) {
		if(root == null) {
			root = new RootLeafNode(data);
			try {
				root.addData(data, 0);
			} catch (SplitNodeReplacement e) {
				throw new RuntimeException("Should never happen!");
			}
		} else {
			double distance = distanceFunction.calculate(data, root.data);
			try {
				root.addData(data, distance);
			} catch(SplitNodeReplacement e) {
				Node newRoot = new RootNode(data);
				root = newRoot;
				for(int i = 0; i < e.newNodes.length; i++) {
					@SuppressWarnings("unchecked")
					Node newNode = (Node) e.newNodes[i];
					distance = distanceFunction.calculate(root.data, newNode.data);
					root.addChild(newNode, distance);
				}
			}
		}
	}


	/**
	 * Removes a data object from the M-Tree.
	 * @param data The data object to be removed.
	 * @return {@code true} if and only if the object was found.
	 */
	public boolean remove(DATA data) {
		if(root == null) {
			return false;
		}
		
		double distanceToRoot = distanceFunction.calculate(data, root.data);
		try {
			root.removeData(data, distanceToRoot);
		} catch(RootNodeReplacement e) {
			@SuppressWarnings("unchecked")
			Node newRoot = (Node) e.newRoot;
			root = newRoot;
		} catch(DataNotFound e) {
			return false;
		} catch (NodeUnderCapacity e) {
			throw new RuntimeException("Should have never happened", e);
		}
		return true;
	}

	/**
	 * Performs a nearest-neighbors query on the M-Tree, constrained by distance.
	 * @param queryData The query data object.
	 * @param range     The maximum distance from {@code queryData} to fetched
	 *                  neighbors.
	 * @return A {@link Query} object used to iterate on the results.
	 */
	public Query getNearestByRange(DATA queryData, double range) {
		return getNearest(queryData, range, Integer.MAX_VALUE);
	}
	
	
	/**
	 * Performs a nearest-neighbors query on the M-Tree, constrained by the
	 * number of neighbors.
	 * @param queryData The query data object.
	 * @param limit     The maximum number of neighbors to fetch.
	 * @return A {@link Query} object used to iterate on the results.
	 */
	public Query getNearestByLimit(DATA queryData, int limit) {
		return getNearest(queryData, Double.POSITIVE_INFINITY, limit);
	}

	/**
	 * Performs a nearest-neighbor query on the M-Tree, constrained by distance
	 * and/or the number of neighbors.
	 * @param queryData The query data object.
	 * @param range     The maximum distance from {@code queryData} to fetched
	 *                  neighbors.
	 * @param limit     The maximum number of neighbors to fetch.
	 * @return A {@link Query} object used to iterate on the results.
	 */
	public Query getNearest(DATA queryData, double range, int limit) {
		return new Query(queryData, range, limit);
	}

	/**
	 * Performs a nearest-neighbor query on the M-Tree, without constraints.
	 * @param queryData The query data object.
	 * @return A {@link Query} object used to iterate on the results.
	 */
	public Query getNearest(DATA queryData) {
		return new Query(queryData, Double.POSITIVE_INFINITY, Integer.MAX_VALUE);
	}
	
	
	protected void _check() {
		if(root != null) {
			root._check();
		}
	}
	
	/*
	 * this is the 
	 */
	public class IndexItem {
		public double []bounds;//this stores the lower bound
		DATA data;
		protected double radius;
		double distanceToParent;

		private IndexItem(DATA data) {
			this.data = data;
			this.radius = 0;
			this.distanceToParent = -1;
		}
		

		int _check() {
			_checkRadius();
			_checkDistanceToParent();
			return 1;
		}

		private void _checkRadius() {
			assert radius >= 0;
		}

		protected void _checkDistanceToParent() {
			assert !(this instanceof MTree.RootLeafNode);
			assert !(this instanceof MTree.RootNode);
			assert distanceToParent >= 0;
		}
		
		public DATA getData() {
			return data;
		}
		
		public double getRadius() {
			return radius;
		}
	}

	
	
	public abstract class Node extends IndexItem {
		/*
		 * we can add additional information here, such as the histogram
		 */
		protected Map<DATA, IndexItem> children = new HashMap<DATA, IndexItem>();
		protected Rootness       rootness;
		protected Leafness<DATA> leafness;
		
		private Multiset<Integer> edgeOcc; 
		private Multiset<Integer> lengthOcc;
		
		public Multiset<Integer> getEdgeOcc(){
			return edgeOcc;
		}
		
		public Multiset<Integer> getLengthOcc(){
			return lengthOcc;
		}
		
		
		public Map<DATA, IndexItem> getChildren() {
			return children;
		}
		
		private
		<R extends NodeTrait & Rootness, L extends NodeTrait & Leafness<DATA>>
		Node(DATA data, R rootness, L leafness) {
			super(data);
			
			rootness.thisNode = this;
			this.rootness = rootness;
			
			leafness.thisNode = this;
			this.leafness = leafness;
		}

		private final void addData(DATA data, double distance) throws SplitNodeReplacement {
			doAddData(data, distance);
			checkMaxCapacity();
		}
		
		
		int _check() {
			super._check();
			_checkMinCapacity();
			_checkMaxCapacity();

			int childHeight = -1;
			for(Map.Entry<DATA, IndexItem> e : children.entrySet()) {
				DATA data = e.getKey();
				IndexItem child = e.getValue();
				assert child.data.equals(data);

				_checkChildClass(child);
				_checkChildMetrics(child);

				int height = child._check();
				if(childHeight < 0) {
					childHeight = height;
				} else {
					assert childHeight == height;
				}
			}

			return childHeight + 1;
		}

		protected void doAddData(DATA data, double distance) {
			leafness.doAddData(data, distance);
		}

		protected void doRemoveData(DATA data, double distance) throws DataNotFound {
			leafness.doRemoveData(data, distance);
		}

		private final void checkMaxCapacity() throws SplitNodeReplacement {
			if(children.size() > MTree.this.maxNodeCapacity) {
				DistanceFunction<? super DATA> cachedDistanceFunction = DistanceFunctions.cached(MTree.this.distanceFunction);
				SplitFunction.SplitResult<DATA> splitResult = MTree.this.splitFunction.process(children.keySet(), cachedDistanceFunction);
				
				Node newNode0 = null;
				Node newNode1 = null;
				for(int i = 0; i < 2; ++i) {
					DATA promotedData   = splitResult.promoted.get(i);
					Set<DATA> partition = splitResult.partitions.get(i);
					
					Node newNode = newSplitNodeReplacement(promotedData);
					for(DATA data : partition) {
						IndexItem child = children.get(data);
						children.remove(data);
						double distance = cachedDistanceFunction.calculate(promotedData, data);
						newNode.addChild(child, distance);
					}

					if(i == 0) {
						newNode0 = newNode;
					} else {
						newNode1 = newNode;
					}
				}
				assert children.isEmpty();

				throw new SplitNodeReplacement(newNode0, newNode1);
			}

		}

		protected Node newSplitNodeReplacement(DATA data) {
			return leafness.newSplitNodeReplacement(data);
		}

		protected void addChild(IndexItem child, double distance) {
			leafness.addChild(child, distance);
		}

		void removeData(DATA data, double distance) throws RootNodeReplacement, NodeUnderCapacity, DataNotFound {
			doRemoveData(data, distance);
			if(children.size() < getMinCapacity()) {
				throw new NodeUnderCapacity();
			}
		}

		protected int getMinCapacity() {
			return rootness.getMinCapacity();
		}

		private void updateMetrics(IndexItem child, double distance) {
			child.distanceToParent = distance;
			updateRadius(child);
		}

		private void updateRadius(IndexItem child) {
			this.radius = Math.max(this.radius, child.distanceToParent + child.radius);
		}

		void _checkMinCapacity() {
			rootness._checkMinCapacity();
		}

		private void _checkMaxCapacity() {
			assert children.size() <= MTree.this.maxNodeCapacity;
		}

		private void _checkChildClass(IndexItem child) {
			leafness._checkChildClass(child);
		}

		private void _checkChildMetrics(IndexItem child) {
			double dist = MTree.this.distanceFunction.calculate(child.data, this.data);
			assert child.distanceToParent == dist;

			double sum = child.distanceToParent + child.radius;
			assert sum <= this.radius;
		}
		
		protected void _checkDistanceToParent() {
			rootness._checkDistanceToParent();
		}

		private MTree<DATA> mtree() {
			return MTree.this;
		}
	}
	
	
	
	
	private abstract class NodeTrait {
		protected Node thisNode;
	}
	
	private interface Leafness<DATA> {
		void doAddData(DATA data, double distance);
		void addChild(MTree<DATA>.IndexItem child, double distance);
		void doRemoveData(DATA data, double distance) throws DataNotFound;
		MTree<DATA>.Node newSplitNodeReplacement(DATA data);
		void _checkChildClass(MTree<DATA>.IndexItem child);
	}
	
	private interface Rootness {
		int getMinCapacity();
		void _checkDistanceToParent();
		void _checkMinCapacity();
	}
	
	
	
	private class RootNodeTrait extends NodeTrait implements Rootness {
		
		@Override
		public int getMinCapacity() {
			throw new RuntimeException("Should not be called!");
		}
		
		@Override
		public void _checkDistanceToParent() {
			assert thisNode.distanceToParent == -1;
		}

		@Override
		public void _checkMinCapacity() {
			thisNode._checkMinCapacity();
		}
		
	};


	private class NonRootNodeTrait extends NodeTrait implements Rootness {
		
		@Override
		public int getMinCapacity() {
			return MTree.this.minNodeCapacity;
		}
		
		@Override
		public void _checkMinCapacity() {
			assert thisNode.children.size() >= thisNode.mtree().minNodeCapacity;
		}
		
		@Override
		public void _checkDistanceToParent() {
			assert thisNode.distanceToParent >= 0;
		}
	};
	
	
	private class LeafNodeTrait extends NodeTrait implements Leafness<DATA> {
		
		public void doAddData(DATA data, double distance) {
			Entry entry = thisNode.mtree().new Entry(data);
			assert !thisNode.children.containsKey(data);
			thisNode.children.put(data, entry);
			assert thisNode.children.containsKey(data);
			thisNode.updateMetrics(entry, distance);
		}

		public void addChild(IndexItem child, double distance) {
			assert !thisNode.children.containsKey(child.data);
			thisNode.children.put(child.data, child);
			assert thisNode.children.containsKey(child.data);
			thisNode.updateMetrics(child, distance);
		}
		
		public Node newSplitNodeReplacement(DATA data) {
			return thisNode.mtree().new LeafNode(data);
		}
		

		@Override
		public void doRemoveData(DATA data, double distance) throws DataNotFound {
			if(thisNode.children.remove(data) == null) {
				throw new DataNotFound();
			}
		}

		public void _checkChildClass(IndexItem child) {
			assert child instanceof MTree.Entry;
		}
	}


	class NonLeafNodeTrait extends NodeTrait implements Leafness<DATA> {
		
		public void doAddData(DATA data, double distance) {
			class CandidateChild {
				Node node;
				double distance;
				double metric;
				private CandidateChild(Node node, double distance, double metric) {
					this.node = node;
					this.distance = distance;
					this.metric = metric;
				}
			}
			
			CandidateChild minRadiusIncreaseNeeded = new CandidateChild(null, -1.0, Double.POSITIVE_INFINITY);
			CandidateChild nearestDistance         = new CandidateChild(null, -1.0, Double.POSITIVE_INFINITY);
			
			for(IndexItem item : thisNode.children.values()) {
				@SuppressWarnings("unchecked")
				Node child = (Node)item;
				double childDistance = thisNode.mtree().distanceFunction.calculate(child.data, data);
				if(childDistance > child.radius) {
					double radiusIncrease = childDistance - child.radius;
					if(radiusIncrease < minRadiusIncreaseNeeded.metric) {
						minRadiusIncreaseNeeded = new CandidateChild(child, childDistance, radiusIncrease);
					}
				} else {
					if(childDistance < nearestDistance.metric) {
						nearestDistance = new CandidateChild(child, childDistance, childDistance);
					}
				}
			}
			
			CandidateChild chosen = (nearestDistance.node != null)
			                      ? nearestDistance
			                      : minRadiusIncreaseNeeded;
			
			Node child = chosen.node;
			try {
				child.addData(data, chosen.distance);
				thisNode.updateRadius(child);
			} catch(SplitNodeReplacement e) {
				// Replace current child with new nodes
				IndexItem _ = thisNode.children.remove(child.data);
				assert _ != null;
				
				for(int i = 0; i < e.newNodes.length; ++i) {
					@SuppressWarnings("unchecked")
					Node newChild = (Node) e.newNodes[i];
					distance = thisNode.mtree().distanceFunction.calculate(thisNode.data, newChild.data);
					thisNode.addChild(newChild, distance);
				}
			}
		}
		

		public void addChild(IndexItem newChild_, double distance) {
			@SuppressWarnings("unchecked")
			Node newChild = (Node) newChild_;
			
			class ChildWithDistance {
				Node child;
				double distance;
				private ChildWithDistance(Node child, double distance) {
					this.child = child;
					this.distance = distance;
				}
			}
			
			Deque<ChildWithDistance> newChildren = new ArrayDeque<ChildWithDistance>();
			newChildren.addFirst(new ChildWithDistance(newChild, distance));
			
			while(!newChildren.isEmpty()) {
				ChildWithDistance cwd = newChildren.removeFirst();
				
				newChild = cwd.child;
				distance = cwd.distance;
				if(thisNode.children.containsKey(newChild.data)) {
					@SuppressWarnings("unchecked")
					Node existingChild = (Node) thisNode.children.get(newChild.data);
					assert existingChild.data.equals(newChild.data);
					
					// Transfer the _children_ of the newChild to the existingChild
					for(IndexItem grandchild : newChild.children.values()) {
						existingChild.addChild(grandchild, grandchild.distanceToParent);
					}
					newChild.children.clear();
					
					try {
						existingChild.checkMaxCapacity();
					} catch(SplitNodeReplacement e) {
						IndexItem _ = thisNode.children.remove(existingChild.data);
						assert _ != null;
						
						for(int i = 0; i < e.newNodes.length; ++i) {
							@SuppressWarnings("unchecked")
							Node newNode = (Node) e.newNodes[i];
							distance = thisNode.mtree().distanceFunction.calculate(thisNode.data, newNode.data);
							newChildren.addFirst(new ChildWithDistance(newNode, distance));
						}
					}
				} else {
					thisNode.children.put(newChild.data, newChild);
					thisNode.updateMetrics(newChild, distance);
				}
			}
		}


		public Node newSplitNodeReplacement(DATA data) {
			return new InternalNode(data);
		}


		public void doRemoveData(DATA data, double distance) throws DataNotFound {
			for(IndexItem childItem : thisNode.children.values()) {
				@SuppressWarnings("unchecked")
				Node child = (Node)childItem;
				if(Math.abs(distance - child.distanceToParent) <= child.radius) {
					double distanceToChild = thisNode.mtree().distanceFunction.calculate(data, child.data);
					if(distanceToChild <= child.radius) {
						try {
							child.removeData(data, distanceToChild);
							thisNode.updateRadius(child);
							return;
						} catch(DataNotFound e) {
							// If DataNotFound was thrown, then the data was not found in the child
						} catch(NodeUnderCapacity e) {
							Node expandedChild = balanceChildren(child);
							thisNode.updateRadius(expandedChild);
							return;
						} catch (RootNodeReplacement e) {
							throw new RuntimeException("Should never happen!");
						}
					}
				}
			}
			
			throw new DataNotFound();
		}

		
		private Node balanceChildren(Node theChild) {
			// Tries to find anotherChild which can donate a grand-child to theChild.
			
			Node nearestDonor = null;
			double distanceNearestDonor = Double.POSITIVE_INFINITY;
			
			Node nearestMergeCandidate = null;
			double distanceNearestMergeCandidate = Double.POSITIVE_INFINITY;

			for(IndexItem child : thisNode.children.values()) {
				@SuppressWarnings("unchecked")
				Node anotherChild = (Node)child;
				if(anotherChild == theChild) continue;

				double distance = thisNode.mtree().distanceFunction.calculate(theChild.data, anotherChild.data);
				if(anotherChild.children.size() > anotherChild.getMinCapacity()) {
					if(distance < distanceNearestDonor) {
						distanceNearestDonor = distance;
						nearestDonor = anotherChild;
					}
				} else {
					if(distance < distanceNearestMergeCandidate) {
						distanceNearestMergeCandidate = distance;
						nearestMergeCandidate = anotherChild;
					}
				}
			}

			if(nearestDonor == null) {
				// Merge
				for(IndexItem grandchild : theChild.children.values()) {
					double distance = thisNode.mtree().distanceFunction.calculate(grandchild.data, nearestMergeCandidate.data);
					nearestMergeCandidate.addChild(grandchild, distance);
				}

				IndexItem removed = thisNode.children.remove(theChild.data);
				assert removed != null;
				return nearestMergeCandidate;
			} else {
				// Donate
				// Look for the nearest grandchild
				IndexItem nearestGrandchild = null;
				double nearestGrandchildDistance = Double.POSITIVE_INFINITY;
				for(IndexItem grandchild : nearestDonor.children.values()) {
					double distance = thisNode.mtree().distanceFunction.calculate(grandchild.data, theChild.data);
					if(distance < nearestGrandchildDistance) {
						nearestGrandchildDistance = distance;
						nearestGrandchild = grandchild;
					}
				}

				IndexItem _ = nearestDonor.children.remove(nearestGrandchild.data);
				assert _ != null;
				theChild.addChild(nearestGrandchild, nearestGrandchildDistance);
				return theChild;
			}
		}


		public void _checkChildClass(IndexItem child) {
			assert child instanceof MTree.InternalNode
			    || child instanceof MTree.LeafNode;
		}
	}


	private class RootLeafNode extends Node {
		
		private RootLeafNode(DATA data) {
			super(data, new RootNodeTrait(), new LeafNodeTrait());
		}
		
		void removeData(DATA data, double distance) throws RootNodeReplacement, DataNotFound {
			try {
				super.removeData(data, distance);
			} catch (NodeUnderCapacity e) {
				assert children.isEmpty();
				throw new RootNodeReplacement(null);
			}
		}
		
		protected int getMinCapacity() {
			return 1;
		}

		void _checkMinCapacity() {
			assert children.size() >= 1;
		}
	}

	private class RootNode extends Node {

		private RootNode(DATA data) {
			super(data, new RootNodeTrait(), new NonLeafNodeTrait());
		}
		
		void removeData(DATA data, double distance) throws RootNodeReplacement, NodeUnderCapacity, DataNotFound {
			try {
				super.removeData(data, distance);
			} catch(NodeUnderCapacity e) {
				// Promote the only child to root
				@SuppressWarnings("unchecked")
				Node theChild = (Node)(children.values().iterator().next());
				Node newRoot;
				if(theChild instanceof MTree.InternalNode) {
					newRoot = new RootNode(theChild.data);
				} else {
					assert theChild instanceof MTree.LeafNode;
					newRoot = new RootLeafNode(theChild.data);
				}

				for(IndexItem grandchild : theChild.children.values()) {
					distance = MTree.this.distanceFunction.calculate(newRoot.data, grandchild.data);
					newRoot.addChild(grandchild, distance);
				}
				theChild.children.clear();

				throw new RootNodeReplacement(newRoot);
			}
		}
		
		
		@Override
		protected int getMinCapacity() {
			return 2;
		}
		
		@Override
		void _checkMinCapacity() {
			assert children.size() >= 2;
		}
	}


	private class InternalNode extends Node {
		private InternalNode(DATA data) {
			super(data, new NonRootNodeTrait(), new NonLeafNodeTrait());
		}
	};


	public class LeafNode extends Node {

		public LeafNode(DATA data) {
			super(data, new NonRootNodeTrait(), new LeafNodeTrait());
		}
	}


	public class Entry extends IndexItem {
		private Entry(DATA data) {
			super(data);
		}
	}
}
