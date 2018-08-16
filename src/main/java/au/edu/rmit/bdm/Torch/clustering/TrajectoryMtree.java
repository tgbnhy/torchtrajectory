package au.edu.rmit.bdm.Torch.clustering;

import java.util.Set;

import au.edu.rmit.bdm.Torch.clustering.mtree.*;
import au.edu.rmit.bdm.Torch.clustering.mtree.tests.Data;
import au.edu.rmit.bdm.Torch.clustering.mtree.utils.Pair;
import au.edu.rmit.bdm.Torch.clustering.mtree.utils.Utils;
public class TrajectoryMtree extends MTree<Data>  {
	private static final PromotionFunction<Data> nonRandomPromotion1 = new PromotionFunction<Data>() {
		@Override
		public Pair<Data> process(Set<Data> dataSet, DistanceFunction<? super Data> distanceFunction) {
			return Utils.minMax(dataSet);
		}
	};	
	
	/*
	 * initilize our bdm functions, we can specify the capacity and distance function
	 */
	public TrajectoryMtree() {//DEFAULT_MIN_NODE_CAPACITY
		super(4, DistanceFunctions.EBD, 
			new ComposedSplitFunction<Data>(
				nonRandomPromotion1,
				new PartitionFunctions.BalancedPartition<Data>()
			)
		);
	}
	
	/*
	 * initilize our bdm functions, we can specify the capacity and distance function
	 */
	public TrajectoryMtree(int capacity) {//DEFAULT_MIN_NODE_CAPACITY
		super(capacity, DistanceFunctions.EBD, 
			new ComposedSplitFunction<Data>(
				nonRandomPromotion1,
				new PartitionFunctions.BalancedPartition<Data>()
			)
		);
	}

	public void add(Data data) {
		super.add(data);
		_check();
	}

	public boolean remove(Data data) {
		boolean result = super.remove(data);
		_check();
		return result;
	}
	
	DistanceFunction<? super Data> getDistanceFunction() {
		return distanceFunction;
	}
	
	public void buildMtree(int [] trajectory, int traid) {
		Data data = new Data(trajectory, traid);
		add(data);
	}
	
	public void buildHistogram() {
		buildHistogram(this.root);
	}
}
