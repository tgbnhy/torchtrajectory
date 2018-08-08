package au.edu.rmit.bdm.TTorch.queryEngine.similarity;

import au.edu.rmit.bdm.TTorch.base.helper.GeoUtil;
import au.edu.rmit.bdm.TTorch.base.model.TrajEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Euclidean Distance, DTW, LCSS, EDR
 * usage:
 * DistanceFunction<TorVertex, TorVertex> distFunc = (p1, p2) -> GeoUtil.distance(p1, p2);
 * Comparator<TorVertex> comparator = (p1, p2) -> {
 * double dist = GeoUtil.distance(p1, p2);
 * if (dist < 8) return 0;
 * return 1;
 * };
 * SimilarityFunction<TorVertex> SIM_MEASURE = new SimilarityFunction<>(distFunc, comparator);
 *
 * @author forrest0402
 */
public class SimilarityFunction<T extends TrajEntry> {

    private static Logger logger = LoggerFactory.getLogger(SimilarityFunction.class);
    public static final SimilarityFunction<TrajEntry> DEFAULT;

    static {
        DistanceFunction<TrajEntry, TrajEntry> distFunc = GeoUtil::distance;
        Comparator<TrajEntry> comparator = (p1, p2) -> {
            double dist = GeoUtil.distance(p1, p2);
            if (dist <= 50) return 0;
            return 1;
        };

        DEFAULT = new SimilarityFunction<>(distFunc, comparator);
    }

    private final DistanceFunction distFunc;

    public Comparator<T> comparator;
    public String measure;

    public SimilarityFunction(DistanceFunction<T, T> distFunc, Comparator<T> comparator) {
        this.distFunc = distFunc;
        this.comparator = comparator;
    }


    public double EuclideanDistance(List<T> pointList1, List<T> pointList2) {
        if (pointList1.size() != pointList2.size())
            throw new IllegalArgumentException("pointList1 should be of the same length as pointList2");
        double dist = 0.0;
        for (int i = 0; i < pointList1.size(); ++i) {
            dist += Math.sqrt(distFunc.apply(pointList1.get(i), pointList2.get(i)));
        }
        return dist;
    }


    public double LongestCommonSubsequence(List<T> T1, List<T> T2, int theta) {

        if (T1 == null || T2 == null || T1.size() == 0 || T2.size() == 0)
            return 0;

        int[][] dpInts = new int[T1.size()][T2.size()];

        if (comparator.compare(T1.get(0), T2.get(0)) == 0) dpInts[0][0] = 1;

        for (int i = 1; i < T1.size(); ++i) {
            if (comparator.compare(T1.get(i), T2.get(0)) == 0)
                dpInts[i][0] = 1;
            else dpInts[i][0] = dpInts[i - 1][0];
        }

        for (int i = 1; i < T2.size(); ++i) {
            if (comparator.compare(T2.get(i), T1.get(0)) == 0)
                dpInts[0][i] = 1;
            else dpInts[0][i] = dpInts[0][i - 1];
        }

        for (int i = 1; i < T1.size(); ++i) {
            for (int j = 1; j < T2.size(); ++j) {
                if (Math.abs(i - j) <= theta) {
                    if (comparator.compare(T1.get(i), T2.get(j)) == 0) {
                        dpInts[i][j] = 1 + dpInts[i - 1][j - 1];
                    } else {
                        dpInts[i][j] = Math.max(dpInts[i - 1][j], dpInts[i][j - 1]);
                    }
                }
            }
        }

        return dpInts[T1.size() - 1][T2.size() - 1] * 1.0;
    }

    public double EditDistanceWithRealPenalty(List<T> T1, List<T> T2, T g) {

        if (T1 == null || T1.size() == 0) {
            double res = 0.0;
            if (T2 != null) {
                for (T t : T2) {
                    res += distFunc.apply(t, g);
                }
            }
            return res;
        }

        if (T2 == null || T2.size() == 0) {
            double res = 0.0;
            for (T t : T1) {
                res += distFunc.apply(t, g);
            }
            return res;
        }

        double[][] dpInts = new double[T1.size() + 1][T2.size() + 1];

        for (int i = 1; i <= T1.size(); ++i) {
            dpInts[i][0] = distFunc.apply(T1.get(i - 1), g) + dpInts[i - 1][0];
        }

        for (int j = 1; j <= T2.size(); ++j) {
            dpInts[0][j] = distFunc.apply(T2.get(j - 1), g) + dpInts[0][j - 1];
        }

        for (int i = 1; i <= T1.size(); ++i) {
            for (int j = 1; j <= T2.size(); ++j) {
                dpInts[i][j] = min(dpInts[i - 1][j - 1] + distFunc.apply(T1.get(i - 1), T2.get(j - 1)),
                        dpInts[i - 1][j] + distFunc.apply(T1.get(i - 1), g),
                        dpInts[i][j - 1] + distFunc.apply(g, T2.get(j - 1)));
            }
        }

        return dpInts[T1.size()][T2.size()] * 1.0 / Math.max(T1.size(), T2.size());
    }

    public double EditDistanceonRealSequence(List<T> T1, List<T> T2) {

        if (T1 == null || T1.size() == 0) {
            if (T2 != null) return T2.size();
            else return 0;
        }

        if (T2 == null || T2.size() == 0) {
            return T1.size();
        }

        int[][] dpInts = new int[T1.size() + 1][T2.size() + 1];

        for (int i = 1; i <= T1.size(); ++i) {
            dpInts[i][0] = i;
        }

        for (int j = 1; j <= T2.size(); ++j) {
            dpInts[0][j] = j;
        }

        for (int i = 1; i <= T1.size(); ++i) {
            for (int j = 1; j <= T2.size(); ++j) {
                int subCost = 1;
                if (comparator.compare(T1.get(i - 1), T2.get(j - 1)) == 0)
                    subCost = 0;
                dpInts[i][j] = min(dpInts[i - 1][j - 1] + subCost, dpInts[i - 1][j] + 1, dpInts[i][j - 1] + 1);
            }
        }

        return dpInts[T1.size()][T2.size()] * 1.0;
    }

    public double EditDistanceonRealSequence(List<T> T1, List<T> T2, double bestSoFar) {

        if (T1 == null || T1.size() == 0) {
            if (T2 != null) return T2.size();
            else return 0;
        }

        if (T2 == null || T2.size() == 0) {
            return T1.size();
        }

        int[][] dpInts = new int[T1.size() + 1][T2.size() + 1];

        for (int i = 1; i <= T1.size(); ++i) {
            dpInts[i][0] = i;
        }

        for (int j = 1; j <= T2.size(); ++j) {
            dpInts[0][j] = j;
        }

        for (int i = 1; i <= T1.size(); ++i) {
            for (int j = 1; j <= T2.size(); ++j) {
                int subCost = 1;
                if (comparator.compare(T1.get(i - 1), T2.get(j - 1)) == 0)
                    subCost = 0;
                dpInts[i][j] = min(dpInts[i - 1][j - 1] + subCost, dpInts[i - 1][j] + 1, dpInts[i][j - 1] + 1);
            }
        }

        return dpInts[T1.size()][T2.size()] * 1.0;
    }

    public double DynamicTimeWarping(List<T> T1, List<T> T2) {
        if (T1.size() == 0 && T2.size() == 0) return 0;
        if (T1.size() == 0 || T2.size() == 0) return Integer.MAX_VALUE;

        double[][] dpInts = new double[T1.size() + 1][T2.size() + 1];

        for (int i = 1; i <= T1.size(); ++i) {
            dpInts[i][0] = Integer.MAX_VALUE;
        }

        for (int j = 1; j <= T2.size(); ++j) {
            dpInts[0][j] = Integer.MAX_VALUE;
        }

        for (int i = 1; i <= T1.size(); ++i) {
            for (int j = 1; j <= T2.size(); ++j) {
                dpInts[i][j] = distFunc.apply(T1.get(i - 1), T2.get(j - 1)) + min(dpInts[i - 1][j - 1], dpInts[i - 1][j], dpInts[i][j - 1]);
            }
        }

        return dpInts[T1.size()][T2.size()];
    }

    public double Hausdorff(List<T> t1, List<T> t2) {
        double[][] dist_matrix;
        dist_matrix = new double[t2.size()][t1.size()];
        double result = 0.0D;
        ArrayList<Double> minDistances1 = new ArrayList();
        ArrayList<Double> minDistances2 = new ArrayList();

        int i;
        for (i = 0; i < dist_matrix.length; ++i) {
            for (int j = 0; j < dist_matrix[0].length; ++j) {
                dist_matrix[i][j] = distFunc.apply(t1.get(j), t2.get(i));
            }
        }

        int j;
        double min;
        for (i = 0; i < dist_matrix.length; ++i) {
            min = Double.MAX_VALUE;
            for (j = 0; j < dist_matrix[0].length; ++j) {
                if (dist_matrix[i][j] <= min) {
                    min = dist_matrix[i][j];
                }
            }

            minDistances1.add(min);
        }

        for (i = 0; i < dist_matrix[0].length; ++i) {
            min = Double.MAX_VALUE;

            for (j = 0; j < dist_matrix.length; ++j) {
                if (dist_matrix[j][i] <= min) {
                    min = dist_matrix[j][i];
                }
            }

            minDistances2.add(min);
        }

        Collections.sort(minDistances1);
        Collections.sort(minDistances2);
        double value1 = minDistances1.get(minDistances1.size() - 1);
        double value2 = minDistances2.get(minDistances2.size() - 1);
        result = Math.max(value1, value2);
        return result;
    }

    public double Frechet(List<T> t1, List<T> t2) {
        double[][] ca = new double[t2.size()][t1.size()];
        for (int i = 0; i < t2.size(); ++i) {
            for (int j = 0; j < t1.size(); ++j) {
                ca[i][j] = -1.0D;
            }
        }

        return c(t2.size() - 1, t1.size() - 1, ca, t1, t2);
    }

    private double c(int i, int j, double[][] ca, List<T> t1, List<T> t2) {
        if (ca[i][j] > -1.0D)
            return ca[i][j];

        if (i == 0 && j == 0) {
            ca[i][j] = distFunc.apply(t1.get(0),t2.get(0));
        } else if (j == 0) {
            ca[i][j] = Math.max(c(i - 1, 0, ca, t1, t2), distFunc.apply(t2.get(i), t1.get(0)));
        } else if (i == 0) {
            ca[i][j] = Math.max(c(0, j - 1, ca, t1, t2), distFunc.apply(t2.get(0), t1.get(j)));
        } else {
            ca[i][j] = Math.max(Math.min(Math.min(c(i - 1, j, ca, t1, t2), c(i - 1, j - 1, ca, t1, t2)), c(i, j - 1, ca, t1, t2)), distFunc.apply(t2.get(i), t1.get(j)));
        }
        return ca[i][j];

    }

    /**
     * Return the minimal value of a, b, c
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    private int min(int a, int b, int c) {
        if (a > b) a = b;
        if (a > c) a = c;
        return a;
    }

    private double min(double a, double b, double c) {
        if (a > b) a = b;
        if (a > c) a = c;
        return a;
    }

    private double max(double a, double b, double c) {
        if (a < b) a = b;
        if (a < c) a = c;
        return a;
    }

    public enum MeasureType {
        DTW, LCSS, EDR, ERP, LORS, Hausdorff, Frechet
    }
}
