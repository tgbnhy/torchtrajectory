package au.edu.rmit.bdm.clustering.Streaming;

import au.edu.rmit.bdm.clustering.trajectory.kpaths.Yinyang;

import java.io.*;
import java.util.*;

public class Test {
    public static void main(String[] args) throws IOException, InterruptedException {
        genTrajEdges();

    }

    private static void genTrajEdges() throws IOException {
        Map<VerticesEdge, Integer> lookup = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader("Nantong/edges.txt"));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] tk = line.split(";");
            lookup.put(new VerticesEdge(Integer.valueOf(tk[1]), Integer.valueOf(tk[2])), Integer.valueOf(tk[0]));
        }
        reader.close();

        BufferedReader vertexReader = new BufferedReader(new FileReader("Nantong/trajectory_vertex_all.txt"));
        BufferedWriter writer = new BufferedWriter(new FileWriter("Nantong/trajectory_edge_all.txt"));

        String vertexLine;
        while ((vertexLine = vertexReader.readLine()) != null) {

            String temp[] = vertexLine.split("\t");
            String carId = temp[0];
            String[] vertexTK = temp[1].split(",");

            StringBuilder builder = new StringBuilder(carId).append("\t");

            for (int i = 1; i < vertexTK.length; i++) {
                int edgeId = lookup.get(new VerticesEdge(Integer.valueOf(vertexTK[i - 1]), Integer.valueOf(vertexTK[i])));
                builder.append(edgeId).append(",");
            }
            builder.setLength(builder.length() - 1);
            writer.write(builder.toString());
            writer.newLine();
        }

        vertexReader.close();
        writer.flush();
        writer.close();
    }

        private static void genRecords() throws IOException {
        Map<VerticesEdge, Integer> lookup = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader("Nantong/edges.txt"));
        String line;
        while((line = reader.readLine())!=null){
            String[] tk = line.split(";");
            lookup.put(new VerticesEdge(Integer.valueOf(tk[1]), Integer.valueOf(tk[2])), Integer.valueOf(tk[0]));
        }
        reader.close();

        BufferedReader vertexReader = new BufferedReader(new FileReader("Nantong/trajectory_vertex_all.txt"));
        BufferedReader timeReader = new BufferedReader(new FileReader("Nantong/trajectory_time_all.txt"));
        Map<Integer, List<CarIdEdge>> records = new HashMap<>();


        String vertexLine, timeLine;
        while ((vertexLine = vertexReader.readLine()) != null){
            timeLine = timeReader.readLine();
            String temp[] = vertexLine.split("\t");
            int carId = Integer.valueOf(temp[0]);
            String[] vertexTK = temp[1].split(",");
            String[] timeTK = timeLine.split("\t")[1].split(",");

            for (int i = 1; i < vertexTK.length; i++){
                int edgeId = lookup.get(new VerticesEdge(Integer.valueOf(vertexTK[i-1]), Integer.valueOf(vertexTK[i])));
                int time = Integer.valueOf(timeTK[i-1]);
                if (!records.containsKey(time)){
                    records.put(time, new LinkedList<>());
                }
                records.get(time).add(new CarIdEdge(carId, edgeId));
            }
        }

        vertexReader.close();
        timeReader.close();

        List<Map.Entry<Integer, List<CarIdEdge>>> list = new ArrayList<>(records.entrySet());
        list.sort(Map.Entry.comparingByKey());
        Iterator<Map.Entry<Integer, List<CarIdEdge>>> iter = list.iterator();
        BufferedWriter writer = new BufferedWriter(new FileWriter("time_car_edge.txt"));
        while (iter.hasNext()){
            Map.Entry<Integer, List<CarIdEdge>> entry = iter.next();
            int time = entry.getKey();
            List<CarIdEdge> l = entry.getValue();
            for (CarIdEdge carIdEdge : l){

                writer.write(time + "," + carIdEdge.carId + "," + carIdEdge.edge);
                writer.newLine();
            }
        }

        writer.flush();
        writer.close();
    }
}
