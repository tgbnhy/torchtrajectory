# <img src="https://github.com/tgbnhy/torch-trajectory/blob/developing/t4.png" alt="drawing" width="100"/>  T-Torch: searching trajectories in a fast way
* T-Torch is the world's first search engine for trajectory data, which is built over our research paper below:
```
Sheng Wang, Zhifeng Bao, J. Shane Culpepper, Zizhe Xie, Qizhi Liu, Xiaolin Qin: Torch: A Search Engine for 
Trajectory Data. SIGIR 2018: 535-544
```
* We also have a traffic analytics system based on T-Torch: http://43.240.96.2/TTorchServer/.

## Supported queries 
T-Torch is able to efficiently answer two typical types of queries now:
* Boolean search
  * Range query
  * Path query
  * Strict path query
* Top-k similarity search, we support a list of similarity measures:
  * Dynamic time wrapping (DTW)
  * Longest common sub-sequence (LCSS)
  * Edit distance with real penalty (EDR)
  * Hausdorff distance
  * Discrete Fréchet Distance
  * Longest overlapped road segments (LORS)

## Features of framework
* T-Torch performs trajectory search over the mapped trajectories.
  * Optimized map-matching algorithms over Graphhopper
  * Ligtweight data storage
* Fast search with
  * Compressiable index
  * Various similarity measures
* Trajectory visualization on real road network
  * http://43.240.96.2/TTorchServer/


## Getting started
### 0. Dataset
Our trajectory dataset collected at Porto is also shared. 
The dataset is map-matched and indexed so that you could go part 2 directly.
Please download it at https://drive.google.com/open?id=110U9RaQxHxWS_tN0IjI-A1N71ElRiyBM
and put it in the root directory of the project.

### 1. Map matching

```
   MapMatching mm = MapMatching.getBuilder().build("Resources/porto_raw_trajectory.txt","Resources/porto.osm.pbf");
   mm.start();
```

Map matching is the technique for projecting raw trajectories onto real road network.The first argument is the URI of raw trajectory data-set, while the second argument **"Resources/porto.osm.pbf"** should be the URI to your PBF file<sup>[1]</sup>
After setup, call start() method to convert raw trajectories to mapped trajectories.

#### Note:
```
   trajectoryID [[latitude1,longtitude1],[latitude2,longtitude2],...]
```
 1. The format of trajectory data should be the same as it in sample data-set, and there is a **\t** character separating trajectory id and content of it
 2. It is your part to take care of data cleansing, as low quality trajectories leads to low projection rate, and high length trajectories (over 200) could affect query time.


### 2. Query
After map-matching, we could perform trajectory retrieval over mapped trajectories. T-Torch provides high level class *Engine* containing simple APIs for query processing. 
To get the engine ready( loading or building necessary indexes to support different types of query), 
only a line of code is required: 
```
   Engine engine = Engine.getBuilder().build();
``` 

#### 1) Range query
```
   QueryResult ret = engine.findInRange(50, 50, 50);
```
The range query is used to retrieve trajectories passing through a specified rectangular area. To define the rectangular area, three arguments are needed. 
Latitude and longitude defines the middle point, with radius( in meters) together representing the rectangular area.

#### 2) Path query
```
   QueryResult ret = engine.findOnPath(query);
```
The Path query<sup>[2]</sup> is used to retrieve trajectories having at least one common edge with the query.
The argument it takes is a "path" represented by a list of *Coordinate*.

#### 3) Strict path query
```
   QueryResult ret = engine.findOnStrictPath(query)
```
The strict path query<sup>[2]</sup> is used to retrieve trajectories strictly passing through the entire query from beginning to end.
The argument it takes is a "path" represented by a list of *Coordinate*.

#### 4) Top-k trajectory similarity search
```
   QueryResult ret = engine.findTopK(query, 3);
```
The top-k query returns
k highest ranked trajectories based on the specified similarity measure.
First argument is a "query trajectory" represented by a list of *Coordinate*, 
and the second is number of top results to return.

### 3. QueryResult
```
   if (ret.mappingSucceed){
      List<Trajectory<TrajEntry>> l = ret.getResultTrajectory();
      String mapVformat = ret.getMapVFormat();
   }else{
       //do something
   }
```

After the query is processed, object of type QueryResult is returned uniformly. 
It contains the query trajectory in raw form, the map-matched query trajectory, and all trajectories being retrieved. Also, you can project these on MapV<sup>[3]</sup> for visualization purpose.



## Main contributors
  * Yunzhuang Shen
  * Zizhe Xie
  * Sheng Wang (Homepage: https://sites.google.com/site/shengwangcs/)

## Cite the paper
If you use this code for your scientific work, please cite it as:

```
Sheng Wang, Zhifeng Bao, J. Shane Culpepper, Zizhe Xie, Qizhi Liu, Xiaolin Qin: Torch: A Search Engine for 
Trajectory Data. SIGIR 2018: 535-544
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

[1]: https://wiki.openstreetmap.org/wiki/PBF_Format
[2]: https://dl.acm.org/citation.cfm?id=2666413 "Krogh, B., Pelekis, N., Theodoridis, Y., & Torp, K. (2014, November). Path-based queries on trajectory data. In Proceedings of the 22nd ACM SIGSPATIAL International Conference on Advances in Geographic Information Systems (pp. 341-350). ACM."
[3]: http://mapv.baidu.com/

