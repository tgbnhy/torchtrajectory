package au.edu.rmit.bdm.clustering.trajectory.visualization;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

public class mapv {
	private static Scanner in;

	/*
	 * show the specific path in the road
	 */
	public static void generateClusterPath(Map<Integer, int[]> map, Map<Integer, String> edgeInfo,
			ArrayList<Integer> clusterIDs, String output) {
		write(output, "geometry\n");
		for (int clust_i = 0; clust_i < clusterIDs.size(); clust_i++) {
			String content = "\"{\"\"type\"\": \"\"LineString\"\", \"\"coordinates\"\": [";
			int[] cluster = map.get(clusterIDs.get(clust_i));
			// System.out.println(Arrays.toString(cluster));
			for (int edge_i = 0; edge_i < cluster.length; edge_i++) {
				String single_edge = edgeInfo.get(cluster[edge_i]);
				String[] abc = single_edge.split(",");
				for (int i = 0; i < abc.length / 2; i++) {
					content += "[" + abc[i + abc.length / 2] + "," + abc[i] + "],";
				}
			}
			content = content.substring(0, content.length() - 1);
			content += "]}\"";
			write(output, content + "\n");
		}
	}
	
	/*
	 * show the specific path in the road
	 */
	public static void generateClusterPath1(Map<Integer, int[]> map, Map<Integer, String> edgeInfo,
			ArrayList<int[]> clusterIDs, String output) {
		write(output, "geometry\n");
		for (int clust_i = 0; clust_i < clusterIDs.size(); clust_i++) {
			
			int[] cluster = clusterIDs.get(clust_i);
			for (int edge_i = 0; edge_i < cluster.length; edge_i++) {
				String single_edge = edgeInfo.get(cluster[edge_i]);
				String content = "\"{\"\"type\"\": \"\"LineString\"\", \"\"coordinates\"\": [";
				String[] abc = single_edge.split(",");
				for (int i = 0; i < abc.length / 2; i++) {
					content += "[" + abc[i + abc.length / 2] + "," + abc[i] + "],";
				}
				content = content.substring(0, content.length() - 1);
				content += "]}\"";
				write(output, content + "\n");
			}	
		}
	}
	
	/*
	 * show the specific sorted path in the road network
	 */
	public static void generateClusterPathSorted(Map<Integer, int[]> map, Map<Integer, String> edgeInfo,
			ArrayList<Integer> clusterIDs, String output) {
		write(output, "geometry\n");
		for (int clust_i = 0; clust_i < clusterIDs.size(); clust_i++) {			
			int[] cluster = map.get(clusterIDs.get(clust_i));
			// System.out.println(Arrays.toString(cluster));
			for (int edge_i = 0; edge_i < cluster.length; edge_i++) {
				String content = "\"{\"\"type\"\": \"\"LineString\"\", \"\"coordinates\"\": [";
				String single_edge = edgeInfo.get(cluster[edge_i]);
				String[] abc = single_edge.split(",");
				for (int i = 0; i < abc.length / 2; i++) {
					content += "[" + abc[i + abc.length / 2] + "," + abc[i] + "],";
				}
				content = content.substring(0, content.length() - 1);
				content += "]}\"";
				write(output, content + "\n");
			}
		}
	}
	
	/*
	 * show the subset of edges
	 */
	public static void generateHighEdges(Map<Integer, String> edgeInfo,
			ArrayList<Integer> edgeIDs, String output) {
		write(output, "geometry\n");
		for (int edge_i = 0; edge_i < edgeIDs.size(); edge_i++) {
			String content = "\"{\"\"type\"\": \"\"LineString\"\", \"\"coordinates\"\": [";
			String single_edge = edgeInfo.get(edgeIDs.get(edge_i));
			String[] abc = single_edge.split(",");
			for (int i = 0; i < abc.length / 2; i++) {
				content += "[" + abc[i + abc.length / 2] + "," + abc[i] + "],";
			}
			content = content.substring(0, content.length() - 1);
			content += "]}\"";
			write(output, content + "\n");
		}
	}
	
	
	/*
	 * generate the graph for mapv based on the edge graph data
	 */
	public static void generate_mapv_graph_porto(String edge, String output) {
		write(output, "geometry\n");
		try {
			in = new Scanner(new BufferedReader(new FileReader(edge)));
			while (in.hasNextLine()) {
				String content = "\"{\"\"type\"\": \"\"LineString\"\", \"\"coordinates\"\": [";
				String str = in.nextLine();
				String strr = str.trim();
				String[] abc = strr.split(",");
				for (int i = 0; i < abc.length / 2; i++) {
					content += "[" + abc[i + abc.length / 2] + "," + abc[i] + "]";
					if (i < abc.length / 2 - 1)
						content += ",";
				}
				content += "]}\"";
				write(output, content + "\n");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	// write the information into files
	public static void write(String fileName, String content) {
		RandomAccessFile randomFile = null;
		try {
			randomFile = new RandomAccessFile(fileName, "rw");
			long fileLength = randomFile.length();
			randomFile.seek(fileLength);
			randomFile.writeBytes(content);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (randomFile != null) {
				try {
					randomFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/*
	 * convert the sorted sequence into a path in the road network.
	 */
	public static void reconvertTrajectory() {
		
	}
}