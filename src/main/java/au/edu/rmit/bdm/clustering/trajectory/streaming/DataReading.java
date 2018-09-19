package au.edu.rmit.bdm.clustering.trajectory.streaming;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import au.edu.rmit.bdm.clustering.trajectory.kpaths.Util;

public class DataReading extends Thread {

	private static Map<String, Integer> edgeMapping; // the key stores the composition of camera id, lane id, direction, the value stores the new id
	private static Map<String, Integer> vehicleMapping;// the key stores the original plate number, the value is the new id.
	static int globalStarttime=151792;
	
	public DataReading() {
		
	}
	
	/*
	 * convert the raw data from oracle to a simple version
	 */
	public static void convertToEdges(String path, String output, String newEdgeFile, String newCarFile) throws IOException {
		int edgeCounter=1;
		int carCounter=1;
		edgeMapping = new HashMap<>();
		vehicleMapping = new HashMap<>();
		try {
			Scanner in = new Scanner(new BufferedReader(new FileReader(path)));	
			while (in.hasNextLine()) {// load the trajectory dataset, and we can efficiently find the trajectory by their id.
				String str = in.nextLine();
				String strr = str.trim();
				String[] record = strr.split(",");
				if(record[0].equals("LKBH") || record.length<6)
					continue; //|| StringUtils.isNumeric(record[5])==false
				String oldedge = record[0]+"_"+record[1]+"_"+record[2];//can be combined with the lane number for a high granularity
				int newedge=0;
				if(edgeMapping.containsKey(oldedge)) {
					newedge = edgeMapping.get(oldedge);
				}else {
					newedge = edgeCounter++;
					edgeMapping.put(oldedge, newedge);
				}
				int newcar=0;
				if(vehicleMapping.containsKey(record[3])) {
					newcar = vehicleMapping.get(record[3]);
				}else {
					newcar = carCounter++;
					vehicleMapping.put(record[3], newcar);
				}
				double normalizedTime = Double.valueOf(record[5]);
				String newrecord = (int)normalizedTime+","+newcar+","+newedge+"\n";
				System.out.println(newrecord);
				Util.write(output, newrecord);
			}
			in.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		for(String oldcar:vehicleMapping.keySet()) {
			int newcar = vehicleMapping.get(oldcar);
			String newrecord = newcar+","+oldcar+"\n";
			Util.write(newCarFile, newrecord);
		}
		for(String oldedge:edgeMapping.keySet()) {
			int newedge = edgeMapping.get(oldedge);
			String newrecord = newedge+","+oldedge+"\n";
			Util.write(newEdgeFile, newrecord);
		}
	}
	
	/*
	 * convert to the standard format, each line is the timestamp; edge: carid1, carid2...;
	 */
	public static void convertStandardFormat(String path, String output) {
		Map<Integer, Set<Integer>> storeSecondRecord = null;
		int tempid = -Integer.MAX_VALUE;
		try {
			Scanner in = new Scanner(new BufferedReader(new FileReader(path)));
			while (in.hasNextLine()) {// load the trajectory dataset, and we can efficiently find the trajectory by their id.
				String str = in.nextLine();
				String strr = str.trim();
				String[] record = strr.split(",");
				int cartime = Integer.valueOf(record[0]);
				int carid = Integer.valueOf(record[1]);
				int edgeid = Integer.valueOf(record[2]);
				if(tempid!=cartime) {
					if (storeSecondRecord!=null) {
						int time = tempid+12;
						String content = time + ";";						
						for (int edgeid1 : storeSecondRecord.keySet()) {
							content += edgeid1 + ":";
							Set<Integer> tralist = storeSecondRecord.get(edgeid1);
							for(int carid1: tralist)
								content += carid1+",";
							content = content.substring(0, content.length() - 1);
							content += ";";
						}
						content = content.substring(0, content.length() - 1);
						Util.write(output, content+"\n");
					}					
					storeSecondRecord = new TreeMap<>();
					tempid = cartime;
				}
				Set<Integer> tralist = null;
				if(storeSecondRecord.containsKey(edgeid)) {
					tralist = storeSecondRecord.get(edgeid);
				}else {
					tralist = new HashSet<Integer>();
				}
				tralist.add(carid);
				storeSecondRecord.put(edgeid, tralist);
			}
			in.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
