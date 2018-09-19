package au.edu.rmit.bdm.clustering.trajectory.kpaths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import au.edu.rmit.bdm.clustering.trajectory.streaming.streamEdge;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.*;
/*
 * the streaming k-path processing will be conducted based on the static k-path, when the trajectory updated in a cluster, we will update the existing cluster directly, for the new coming trajectory,
 * we will assign to the existing kpath data stream
 * 1) each trajectory is limited in a limited time window
 * 2) use static to cluster first
 * 3) continue the clustering with new updated data based on previous cluster
 */
public class StreamKpath extends Yinyang{
	private Thread runingThread;
	private String threadName;
	private static ArrayList<streamEdge> invertedIndex;//store the inverted index in a window.
	int globalStarttime;
	int globalEndtime;
	int sleepingTime;
	int speedingratio;//the smallest time interval,
	int currentime;

	
	//one thread read the new data into buffer
	//another thread conduct the k-means
	public StreamKpath(String datapath) {
		super(datapath);
		threadName = datapath;
		globalStarttime = 0;
		globalEndtime = streamingDuration;
		sleepingTime = 1;//wait until the data coming
		speedingratio = 1;
		dataEnough = false;
		dataOut = true;
		invertedIndex = new ArrayList<>();
		for(int i=1;i<=streamEdges;i++) {
			streamEdge edgeindex = new streamEdge(i);
			invertedIndex.add(edgeindex);//initialize the edges
		}
	}
	
	/*
	 * keep reading data from the file
	 */
	public void run() {
		System.out.println("Running " + threadName);
		try {
			readDataFromFile("E:\\dataset\\nantong\\nantong_traffic_record");
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
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
	 * check every dataset, remove the old egdes, add new edges to support k-means clustering.
	 */
	public void updateDataset(Map<Integer, int[]> dataset, Map<Integer, ArrayList<Integer>> olddata, 
			Map<Integer, ArrayList<Integer>> newdata) {
		ArrayList<Integer> newdataset = new ArrayList<>();//store the totally new trajectory, and assign them to the 
		if(olddata!=null)
			for(int carid: olddata.keySet()) {
				ArrayList<Integer> a = olddata.get(carid);
				if(dataset.containsKey(carid)) {
					int []c = dataset.get(carid);
					int []newc = new int[c.length-a.size()];
					System.out.println(c.length+" "+a.size());
					if(a.size() == c.length) {//delete a from c;
						dataset.remove(carid);
						trajectoryNumber--;
					}else {
						int i=0;
						for(int edgeid: c) {
							if(!a.contains(edgeid))
								newc[i++] = edgeid;//maintain the remaining
						}
						dataset.put(carid, newc);
					}
				}
			}
		if(newdata!=null)
			for(int carid: newdata.keySet()) {//add to the new data;
				ArrayList<Integer> a = newdata.get(carid);
				if(a.size()>1)
					Collections.sort(a);//sort the new data in advance
				if(dataset.containsKey(carid)) {
					int []c = dataset.get(carid);
					int []newc = new int[c.length+a.size()];
					int z =0;
					for (int i = 0, j = 0; i < a.size() && j < c.length;) {//merge two sorted lists
						if (a.get(i) < c[j]) {
							newc[z++] = a.get(i);
							i++;
						} else {
							newc[z++] = c[j];
							j++;
						}
					}
					dataset.put(carid, newc);
				}else {//never exist
					int []arrays = a.stream().mapToInt(i -> i).toArray();//convert to array
					dataset.put(carid, arrays);
					newdataset.add(carid);//add to the new dataset
					trajectoryNumber++;
				}
			}
	}
	
	// return false if the iteration of k-means is not stopped
	public boolean timetoupdateData() {
		return iterationStops;
	}
	
	/*
	 * our data is stored in the sql, and we will not change the format.
	 */
	public void readDataFromFile(String path) throws SQLException, InterruptedException{
		long startTime = System.nanoTime();
		datamap = new HashMap<>();// a btree map for easy search is created or read
		Map<Integer, ArrayList<Integer>> dataset_new = new HashMap<>();
		int formerexpired=0;
		readingdata = true;
		try {
			Scanner in = new Scanner(new BufferedReader(new FileReader(path)));
			while (in.hasNextLine()) {// load the trajectory dataset, and we can efficiently find the trajectory by their id.
				String str = in.nextLine();
				String strr = str.trim();
				String[] record = strr.split(";");
				int cartime = Integer.valueOf(record[0]);
				System.out.println(record[0]);
				for(int i=1; i<record.length; i++) {
					String[] edgeCars = record[i].split(":");
					int edgeid = Integer.valueOf(edgeCars[0]);
					String[] cars = edgeCars[1].split(",");
					int []carsid = new int[cars.length];
					int j=0;
					for(String carid: cars) {
						carsid[j++] = Integer.valueOf(carid);
					}
					streamEdge timeList = invertedIndex.get(edgeid);
					timeList.addCars(cartime, carsid);//update the inverted index which will be used				
					for(int carid: carsid) {	//add to dataset_new for temporal storage
						ArrayList<Integer> edgeids = null;
						if(dataset_new.containsKey(carid))
							edgeids= dataset_new.get(carid);
						else
							edgeids = new ArrayList<>();
						if(!edgeids.contains(edgeid))
							edgeids.add(edgeid);
						dataset_new.put(carid, edgeids);
					}
				}
				System.out.println("the new dataset size is "+dataset_new.size());
				double nowTime = (System.nanoTime()-startTime)/1000000.0;// we can be faster by 5* speed as too limited data
				if(cartime>nowTime/1000.0)
					Thread.sleep((long) (cartime*1000-nowTime));//read the files by lines, and sleep until the time matches		
				System.out.println(iterationStops);
				if(!dataEnough) {//to accumulate enough data for clustering					
					if(cartime>=slidingwindow-1) {
						readingdata = true;
						updateDataset(datamap, null, dataset_new);// update to the new data
						dataset_new = new HashMap<>();//a new round
						trajectoryNumber = datamap.size();
						dataEnough = true;
						readingdata = false;
					}					
				}else if(iterationStops) {//it is time to delete the expired data in the index, update the dataset
					Map<Integer, ArrayList<Integer>> dataset_remove = new HashMap<>();
					int expiredtime = cartime-slidingwindow;
					for(streamEdge edgeid1: invertedIndex) {
						edgeid1.removeExprired(expiredtime, formerexpired, dataset_remove);
					}
					formerexpired = expiredtime;
					readingdata = true;
				//	updateDataset(datamap, dataset_remove, dataset_new);// update to the new data
					readingdata = false;
					dataset_new = new HashMap<>();//a new round*/
				}
			}
			in.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		dataOut = false;
	}
}
