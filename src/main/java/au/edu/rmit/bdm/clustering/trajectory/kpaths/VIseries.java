package au.edu.rmit.bdm.clustering.trajectory.kpaths;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/*
 * sheng
 * @date 2/6/2018
 */
public class VIseries {
	int[] trajectory;// the trajectory
	int length;// the number of vertex inside the trajectory
	int idx;// the id of VI series.

	public VIseries() {
		// TODO Auto-generated constructor stub
	}

	public void setVIseries(int[] cl) {
		trajectory = cl;
	}

	public int[] getVIseries() {
		return trajectory;
	}

	/*
	 * using the intersection based on edge ids directly
	 * this is a metric distance function, which obeys triangular inequality based on our proof
	 * we can propose bound to avoid reading each trajectory and compute the similarity model.
	 */
	public double EDRDistanceJaccard(Integer[] T1, Integer[] T2) {
		Set<Integer> s1 = new HashSet<Integer>(Arrays.asList(T1));
		Set<Integer> s2 = new HashSet<Integer>(Arrays.asList(T2));
		int length = Math.max(s1.size(), s2.size());
		s1.retainAll(s2);
		return length - s1.size();
	}

	/*
	 * EDR distance based on SIGMOD 2005 paper, vertex based.
	 */
	public double EDRDistance(int[] T1, int[] T2) {
		if (T1 == null || T1.length == 0) {
			if (T2 != null)
				return T2.length;
			else
				return 0;
		}
		if (T2 == null || T2.length == 0) {
			return T1.length;
		}

		int[][] dpInts = new int[T1.length + 1][T2.length + 1];

		for (int i = 1; i <= T1.length; ++i) {
			dpInts[i][0] = i;
		}

		for (int j = 1; j <= T2.length; ++j) {
			dpInts[0][j] = j;
		}

		for (int i = 1; i <= T1.length; ++i) {
			for (int j = 1; j <= T2.length; ++j) {
				int subCost = 1;
				if (T1[i - 1] == T2[j - 1])
					subCost = 0;
				dpInts[i][j] = min(dpInts[i - 1][j - 1] + subCost, dpInts[i - 1][j] + 1, dpInts[i][j - 1] + 1);
			}
		}

		return dpInts[T1.length][T2.length];
	}

	private int min(int a, int b, int c) {
		if (a > b)
			a = b;
		if (a > c)
			a = c;
		return a;
	}

	/*
	 * dynamic time warping distance
	 */
	public double DTWDistance(VIseries T1, VIseries T2) {
		// double distance = SimilarityMeasure.DynamicTimeWarping(T1.trajectory,
		// T2.trajectory);
		return 0;
	}
}
