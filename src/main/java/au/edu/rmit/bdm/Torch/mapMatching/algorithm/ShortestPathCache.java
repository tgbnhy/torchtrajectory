package au.edu.rmit.bdm.Torch.mapMatching.algorithm;


import au.edu.rmit.bdm.Torch.base.helper.GeoUtil;
import au.edu.rmit.bdm.Torch.mapMatching.model.PillarVertex;
import au.edu.rmit.bdm.Torch.mapMatching.model.TorVertex;
import au.edu.rmit.bdm.Torch.mapMatching.model.TowerVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The class is a lookup table having precomputed shortest path information for each vertex with its near vertexes.
 * It will be filled in TorDijkstra, and queried at PrecomputedHiddenMarkovModel
 *
 * @see TorDijkstra
 * @see PrecomputedHiddenMarkovModel
 */
class ShortestPathCache extends HashMap<String, ShortestPathCache.ShortestPathEntry>{

    Logger logger = LoggerFactory.getLogger(ShortestPathCache.class);

    /**
     * For each tower vertex, compute shortest path with its near tower vertices.
     * For each pillar vertex, compute the distance to base vertex on its edge.
     * @param graph TorGraph object containing all required data.
     */
    void init(TorGraph graph) {
        computeShortestPathForTowerVertices(graph);
        computeDistForPillarVertices(graph);
    }


    /**
     *       consider this situation:
     *
     *             A          B
     *             |          |
     *       T1------T2------------T3
     *
     *
     *  Suppose A and B are the inputs. T1, T2 and T3 are three Tower vertices along the path.
     *  The subroutine is to find tower points along the path.
     *  In this case, a list containing T1, T2 and T3 will be returned.
     *
     *  note if two vertices are the same or they are far away from each other,
     *
     * @return a list of TowerVertices represents the path.
     */
    List<TowerVertex> shortestPath(TorVertex _p1, TorVertex _p2) {

        if (_p1.equals(_p2)) return null;

        if (_p1.isTower && _p2.isTower) {
            return this.get(_p1.hash).routing.get(_p2.hash);
        }

        if (_p1.isTower)
            return shortestPathBetweenTowerAndPillar((TowerVertex)_p1, (PillarVertex) _p2);

        if (_p2.isTower) {
            List<TowerVertex> shortestPath = shortestPathBetweenTowerAndPillar((TowerVertex) _p2, (PillarVertex) _p1);
            Collections.reverse(shortestPath);

            return shortestPath;
        }

        return shortestPathBetweenPillars((PillarVertex) _p1, (PillarVertex) _p2);
    }

    /**
     * Get minimum routing distance from p1 to p2
     *
     * @param p1 vertex on the graph
     * @param p2 vertex on the graph
     * @return distance in meters
     */
    double minDistance(TorVertex p1, TorVertex p2) {
        if (p1 == p2) return 0.0;

        if (p1.isTower && p2.isTower) {
            Double value = this.get(p1.hash).dist.get(p2.hash);
            return value == null ? Double.MAX_VALUE : value;
        }
        if (p1.isTower) return minDisBetweenTowerPointAndPillarVertex((TowerVertex)p1, (PillarVertex) p2);
        if (p2.isTower) return minDisBetweenTowerPointAndPillarVertex((TowerVertex) p2, (PillarVertex) p1);
        return minDisBetweenPillarVertexes((PillarVertex)p1, (PillarVertex)p2);
    }

    /**
     * for each tower vertex, compute the distance to its near vertices
     */
    private void computeShortestPathForTowerVertices(TorGraph graph) {
        logger.info("compute shortest path for each tower point to its nearest tower points");
        //for each tower point, compute the shortest path to other tower points that are within a distance threshold
        TorDijkstra dijkstra = new TorDijkstra(graph);
        double totalCount = graph.towerVertexes.size(), count = 0;
        for (TowerVertex tVertex : graph.towerVertexes.values()) {
            if (tVertex.isTower) {
                dijkstra.run(tVertex);
                if (++count % 10000 == 0 || count == totalCount) {
                    String finishRate = String.format("%.2f", count * 100 / totalCount);
                    logger.info("progress {}%", finishRate);
                }
            }
        }
    }

    /**
     * for every pillar vertex, compute the distance to its base vertex on edge.
     */
    private void computeDistForPillarVertices(TorGraph graph) {
        //initialize toBasePointDistance of LightPoint
        for (TorVertex vertex : graph.allPoints.values()) {
            if (!vertex.isTower) {
                PillarVertex current = ((PillarVertex)vertex);
                TorVertex pre;

                double len = 0;
                if (current.edge.isForward){
                    pre = current.edge.adjVertex;
                    for (int i = current.edge.getPillarVertexes().size()-1; i>=0; i--){
                        TorVertex pVertex = current.edge.getPillarVertexes().get(i);
                        len += GeoUtil.distance(pre, pVertex);
                        if (pVertex == current) break;
                        pre = pVertex;
                    }
                    current.baseAndthisDist = len;
                }
            }
        }
    }

    private double minDisBetweenTowerPointAndPillarVertex(TowerVertex p1, PillarVertex p2) {
        double minDis = Double.MAX_VALUE;
        if (p2.edge.isForward) {
            Double res = this.get(p1.hash).dist.get(p2.edge.baseVertex.hash);
            if (res != null) {
                minDis = res + p2.baseAndthisDist;
            }
        }
        if (p2.edge.isBackward) {
            Double res = this.get(p1.hash).dist.get(p2.edge.adjVertex.hash);
            if (res != null) {
                minDis = Math.min(minDis, res + p2.edge.getLength() - p2.baseAndthisDist);
            }
        }
        return minDis;
    }

    private double minDisBetweenPillarVertexes(PillarVertex p1, PillarVertex p2) {

        if (p1 == p2) return 0.0;
        if (p1.edge == p2.edge) {

            int flag = 0;
            for (PillarVertex pillarVertex : p1.edge.getPillarVertexes()) {
                if (flag != 0) break;
                if (pillarVertex.equals(p1)) flag = 1;
                if (pillarVertex.equals(p2)) flag = 2;
            }
            if ((flag == 1 && p1.edge.isForward) || (flag == 2 && p1.edge.isBackward)) {
                return Math.abs(p1.baseAndthisDist - p2.baseAndthisDist);
            } else if (flag == 1 && p1.edge.isBackward) {
                //p1->p1.base->p1.adj->p2
                Double dist = this.get(p1.edge.baseVertex.hash).dist.get(p1.edge.adjVertex.hash);
                if (dist != null)
                    return p1.baseAndthisDist + dist + p2.edge.getLength() - p2.baseAndthisDist;
                else {
                    return Double.MAX_VALUE;
                }
            } else if (flag == 2 && p1.edge.isForward) {
                //p1->p1.adj->p1.base->p2
                Double dist = this.get(p1.edge.adjVertex.hash).dist.get(p1.edge.baseVertex.hash);
                if (dist != null)
                    return p1.edge.getLength() - p1.baseAndthisDist + dist + p2.baseAndthisDist;
                else {
                    return Double.MAX_VALUE;
                }
            }
        }

        double minDis = Double.MAX_VALUE;
        if (p1.edge.isForward) {
            //p1->p1.adj->p2.base->p2
            if (p2.edge.isForward) {
                Double res = this.get(p1.edge.adjVertex.hash).dist.get(p2.edge.baseVertex.hash);
                if (res != null) {
                    minDis = res + p1.edge.getLength() - p1.baseAndthisDist + p2.baseAndthisDist;
                }
            }
            //p1->p1.adj->p2.adj->p2
            if (p2.edge.isBackward) {
                Double res = this.get(p1.edge.adjVertex.hash).dist.get(p2.edge.adjVertex.hash);
                if (res != null) {
                    minDis = Math.min(minDis, res + p1.edge.getLength() - p1.baseAndthisDist + p2.edge.getLength() - p2.baseAndthisDist);
                }
            }
        }
        if (p1.edge.isBackward) {
            //p1->p1.base->p2.base->p2
            if (p2.edge.isForward) {
                Double res = this.get(p1.edge.baseVertex.hash).dist.get(p2.edge.baseVertex.hash);
                if (res != null) {
                    minDis = Math.min(minDis, res + p1.baseAndthisDist + p2.baseAndthisDist);
                }
            }
            //p1->p1.base->p2.adj->p2
            if (p2.edge.isBackward) {
                Double res = this.get(p1.edge.baseVertex.hash).dist.get(p2.edge.adjVertex.hash);
                if (res != null) {
                    minDis = Math.min(minDis, res + p1.baseAndthisDist + p2.edge.getLength() - p2.baseAndthisDist);
                }
            }
        }
        return minDis;
    }

    private List<TowerVertex> shortestPathBetweenTowerAndPillar(TowerVertex p1, PillarVertex p2) {
        List<TowerVertex> shortestPath = new ArrayList<>();
        double minDis = Double.MAX_VALUE;
        Double distance = -1.;

        if (p2.edge.isForward) {
             distance = this.get(p1.hash).dist.get(p2.edge.baseVertex.hash);
            if (distance != null) {
                minDis = distance + p2.baseAndthisDist;

                shortestPath.addAll(this.get(p1.hash).routing.get(p2.edge.baseVertex.hash));
                shortestPath.add(p2.edge.adjVertex);
            }
        }
        if (p2.edge.isBackward) {
            distance = this.get(p1.hash).dist.get(p2.edge.adjVertex.hash);
            if (distance != null && minDis > distance + p2.edge.getLength() - p2.baseAndthisDist) {
                shortestPath.clear();

                shortestPath.addAll(this.get(p1.hash).routing.get(p2.edge.adjVertex.hash));
                shortestPath.add(p2.edge.baseVertex);
            }
        }

        return shortestPath;
    }

    private List<TowerVertex> shortestPathBetweenPillars(PillarVertex p1, PillarVertex p2) {

        List<TowerVertex> shortestPath = new ArrayList<>();


        if (p1.edge == p2.edge) {
            int flag = 0;
            for (TorVertex point : p1.edge.getPillarVertexes()) {
                if (flag != 0) break;
                if (point == p1) flag = 1;
                if (point == p2) flag = 2;
            }

            if ((flag == 1 && p1.edge.isForward)) {
                shortestPath.add(p1.edge.baseVertex);
                shortestPath.add(p1.edge.adjVertex);
            } else if((flag == 2 && p1.edge.isBackward)) {
                shortestPath.add(p1.edge.adjVertex);
                shortestPath.add(p1.edge.baseVertex);
            }
            else if (flag == 1 && p1.edge.isBackward) {
                //p1->p1.base->p1.adj->p2
                Double dist = this.get(p1.edge.baseVertex.hash).dist.get(p1.edge.adjVertex.hash);
                if (dist != null) {
                    shortestPath.add(p1.edge.baseVertex);
                    shortestPath.addAll(this.get(p1.edge.baseVertex.hash).routing.get(p1.edge.adjVertex.hash));
                    shortestPath.add(p1.edge.adjVertex);
                }
            } else if (flag == 2 && p1.edge.isForward) {
                //p1->p1.adj->p1.base->p2
                Double dist = this.get(p1.edge.adjVertex.hash).dist.get(p1.edge.baseVertex.hash);
                if (dist != null) {
                    shortestPath.add(p1.edge.adjVertex);
                    shortestPath.addAll(this.get(p1.edge.adjVertex.hash).routing.get(p1.edge.baseVertex.hash));
                    shortestPath.add(p1.edge.baseVertex);
                }
            }
        } else {
            double minDis = Double.MAX_VALUE;
            if (p1.edge.isForward) {
                //p1.base->p1->p1.adj->p2.base->p2->p2.adj
                if (p2.edge.isForward) {
                    Double res = this.get(p1.edge.adjVertex.hash).dist.get(p2.edge.baseVertex.hash);
                    if (res != null && minDis > res + p1.edge.getLength() - p1.baseAndthisDist + p2.baseAndthisDist) {
                        minDis = res + p1.edge.getLength() - p1.baseAndthisDist + p2.baseAndthisDist;

                        shortestPath.add(p1.edge.baseVertex);
                        shortestPath.addAll(this.get(p1.edge.adjVertex.hash).routing.get(p2.edge.baseVertex.hash));
                        shortestPath.add(p2.edge.adjVertex);
                    }
                }
                //p1.base->p1->p1.adj->p2.adj->p2->p2.base
                if (p2.edge.isBackward) {
                    Double res = this.get(p1.edge.adjVertex.hash).dist.get(p2.edge.adjVertex.hash);
                    if (res != null && minDis > res + p1.edge.getLength() - p1.baseAndthisDist + p2.edge.getLength() - p2.baseAndthisDist) {
                        minDis = res + p1.edge.getLength() - p1.baseAndthisDist + p2.edge.getLength() - p2.baseAndthisDist;

                        shortestPath.clear();
                        shortestPath.add(p1.edge.adjVertex);
                        shortestPath.addAll(this.get(p1.edge.adjVertex.hash).routing.get(p2.edge.adjVertex.hash));
                        shortestPath.add(p2.edge.baseVertex);
                    }
                }
            }
            if (p1.edge.isBackward) {
                //p1.adj->p1->p1.base->p2.base->p2.adj
                if (p2.edge.isForward) {
                    Double res = this.get(p1.edge.baseVertex.hash).dist.get(p2.edge.baseVertex.hash);
                    if (res != null && minDis > res + p1.baseAndthisDist + p2.baseAndthisDist) {
                        minDis = res + p1.baseAndthisDist + p2.baseAndthisDist;
                        shortestPath.clear();
                        shortestPath.add(p1.edge.baseVertex);
                        shortestPath.addAll(this.get(p1.edge.baseVertex.hash).routing.get(p2.edge.baseVertex.hash));
                        shortestPath.add(p2.edge.adjVertex);
                    }
                }
                //p1.adj->p1->p1.base->p2.adj->p2->p2.base
                if (p2.edge.isBackward) {
                    Double res = this.get(p1.edge.baseVertex.hash).dist.get(p2.edge.adjVertex.hash);
                    if (res != null && minDis > res + p1.baseAndthisDist + p2.edge.getLength() - p2.baseAndthisDist) {
                        shortestPath.clear();
                        shortestPath.add(p1.edge.baseVertex);
                        shortestPath.addAll(this.get(p1.edge.baseVertex.hash).routing.get(p2.edge.adjVertex.hash));
                        shortestPath.add(p2.edge.baseVertex);
                    }
                }
            }
        }


        return shortestPath;
    }

    void addEntry(TowerVertex startPoint, Map<String, Double> dist, Map<String, List<TowerVertex>> routing) {
        ShortestPathEntry entry = new ShortestPathEntry(startPoint, dist, routing);
        put(startPoint.hash, entry);
    }

    class ShortestPathEntry {

        final TowerVertex startPoint;
        /**
         * distance from startPoint to key, key represents endPoint.hash
         */
        final Map<String, Double> dist;

        /**
         * route from startPoint to key, key represents endPoint.hash
         */
        final Map<String, List<TowerVertex>> routing;

        public ShortestPathEntry(TowerVertex startPoint, Map<String, Double> dist, Map<String, List<TowerVertex>> routing) {
            this.startPoint = startPoint;
            this.dist = dist;
            this.routing = routing;
        }
    }
}