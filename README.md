# T-Torch
T-Torch is a search engine for
trajectory data retrieval. 

## supported query 
T-torch is able to efficiently
counter two typical types of queries:
* Boolean retrieval
  * range query
  * path query
  * strict path query
* Top K similarity retrieval

and it also support a list of well-known similarity functions:
  * Longest overlapped road segments
  * Longest common sub-sequence
  * Edit distance with real penalty
  * Dynamic time wrapping
  * Hausdorff
  * Frechet


## Features
* T-Torch performs trajectory retrieval over the mapped trajectories. 
Although map-matching process is slow, is could increase both efficiency 
and effectiveness in query processing part.
* edge based search
* data visualization on real graph
* various map-matching algorithms and implementations
* various indexes
* various similarity functions

## Getting started

##### 1. Map matching

Map matching is the technique for projecting raw trajectories onto real road network.

```
MapMatching mm = MapMatching.getBuilder().build("Resources/porto_raw_trajectory.txt","Resources/porto.osm.pbf");
mm.start();
```

The first argument is to specify your raw trajectory data src, while the second argument *"Resources/porto.osm.pbf"* is an PBF file<sup>[1]</sup>
After setup, start() method is to convert raw trajectories to mapped trajectories. and the results 
be stored in *Torch* folder under CWD, which will be used for query processing part later.

###### Note:
```
trajectoryID [[latitude1,longtitude1],[latitude2,longtitude2],...]
```
 1. the format of trajectory data should be the same as it in sample dataset, and there is a **"\t"** separating trajectory id and content of it
 2. it is your part to take care of data cleansing, as high length trajectory(over 200) could slow down mapping process rapidly, and low quality trajectory leads to low projection rate. 
 


todo


### Build vertexInvertedIndex
After preprocessing, Torch can build indices for map matched trajectories.
Torch supports three types of indices in total:  
1. **GridIndex** is used for indexing trajectory points. It first uniformly partitions the whole area into small rectangles. Then build the inverted vertexInvertedIndex of trajectory points based on those rectangles.  
2. **EdgeIndex (EdgII)** is an inverted vertexInvertedIndex for trajectory edges.  
3. **NodeIndex (VerII)** is an inverted vertexInvertedIndex for trajectory points.  

Before using any vertexInvertedIndex, please first declare them as follows:
```
@Autowired
NodeIndex nodeIndex;
@Autowired
EdgeIndex edgeIndex;
@Autowired
GridIndex gridIndex;
...
```
After declaring, buildIndex() should be called to build the vertexInvertedIndex.
### Compress vertexInvertedIndex
After invoking buildIndex(), all indices will be stored in the disk in plain text. Because the vertexInvertedIndex consists of a sequence of integers, it can be compressed by delta encoding and re-stored in binary form in the disk.
This can be done by calling compress() for each vertexInvertedIndex.
### Queries
After the vertexInvertedIndex is built, it can be loaded in the plain text form or compressed binary form using the following code:
```
edgeIndex.load(); // or
edgeIndex.loadCompressedForm();
```
Under three types of indices, Torch supports four types of queries:
#### Range query
Range query is used to retrieve trajectories passing through a specified rectangle area.
Given a torPoint and a radius r, it can be done with the help of NodeIndex and GridIndex.
```
List<Integer> trajectoryIDs = nodeIndex.rangeQuery(gridIndex, torPoint, r, null);
```
#### Path query
Path<sup>[2]</sup> is used to retrieve trajectories containing any edge of the given path.
```
List<Integer> trajectoryIDs = edgeIndex.pathQuery(originalSegments, null);
```
#### Strict path query
Strict path query<sup>[2]</sup> is used to retrieve trajectories strictly passing through the entire path from beginning to end.
```
List<Integer> trajectoryIDs = edgeIndex.strictPathQuery(querySegments, null);
```
#### Top-k trajectory similarity query
A top-k trajectory similarity search query returns
the k highest ranked trajectories based on the specified similarity metric.
Torch supports six similarity metrics: DTW, LCSS, EDR, and LORS.  
```
Map<Integer, MMEdge> allEdgeMap = new HashMap<>();
Map<Integer, MMPoint> allPointMap = new HashMap<>();
getGraphMap(allEdgeMap, allPointMap);
Map<Integer, Trajectory> trajectoryMap = loadTrajectoryTxtFile(trajectoryFile, pointFile, edgeFile, true);
Trajectory query; //This is the query
```
For DTW:  
```
gridIndex.delete();
if (!gridIndex.load()) {
    gridIndex.buildIndex(allPointMap, epsilon);
}
...
List<Integer> trajectoryID = gridIndex.findTopK(trajectoryMap, allPointMap, query, k, measureType);
```
For LCSS and EDR:
```
if (!nodeIndex.load()) {
    nodeIndex.buildIndex(trajectoryMap.values()); //or use nodeIndex.loadCompressedForm();
}
gridIndex.delete();
if (!gridIndex.load()) {
    gridIndex.buildIndex(allPointMap, epsilon);
...
nodeIndex.findTopK(gridIndex, query, k, measureType, trajLenMap);

```
For LORS:
```
edgeIndex.load(); //or edgeIndex.loadCompressedForm();
List<MMEdge> querySegments = query.getMapMathedTrajectory(allEdges);
double[] restDistance = new double[querySegments.size()];
for (int i = querySegments.size() - 2; i >= 0 && i + 1 < querySegments.size(); --i)
    restDistance[i] = restDistance[i + 1] + querySegments.get(i + 1).getLength();
edgeIndex.findTopK(querySegments, k, allEdges, restDistance);

```
### Cite the paper
---
If you use this code for your scientific work, please cite it as:

```
Sheng Wang, Zhifeng Bao, J. Shane Culpepper, Zizhe Xie, Qizhi Liu, Xiaolin Qin: Torch: A Search Engine for Trajectory Data. SIGIR 2018: 535-544
```

```
@inproceedings{wang2018torch,
  author          = {{Wang}, Sheng and {Bao}, Zhifeng and {Culpepper}, J. Shane and {Xie}, Zizhe and {Liu}, Qizhi and {Qin}, Xiaolin},
  title           = "{Torch: {A} Search Engine for Trajectory Data}",
  booktitle       = {Proceedings of the 41th International ACM SIGIR Conference on Research & Development in Information Retrieval},
  organization    = {ACM},
  pages     = {535--544},
  year            = 2018,
}

```
