package au.edu.rmit.bdm.clustering.mtree.tests;

import au.edu.rmit.bdm.clustering.mtree.DistanceFunctions;

public class Data implements DistanceFunctions.EuclideanCoordinate, Comparable<Data> {
	
	private final int[] values;
	private final int hashCode;
	
	public Data(int... values) {
		this.values = values;
		
		int hashCode = 1;
		for(int value : values) {
			hashCode = 31*hashCode + value;
		}
		this.hashCode = hashCode;
	}
	
	public Data(int[] values, int id) {
		this.values = values;
		this.hashCode = id;
	}
	
	@Override
	public int dimensions() {
		return values.length;
	}

	@Override
	public double get(int index) {
		return values[index];
	}
	
	
	@Override
	public int hashCode() {
		return hashCode;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Data) {
			Data that = (Data) obj;
			if(this.dimensions() != that.dimensions()) {
				return false;
			}
			for(int i = 0; i < this.dimensions(); i++) {
				if(this.values[i] != that.values[i]) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public int compareTo(Data that) {
		int dimensions = Math.min(this.dimensions(), that.dimensions());
		for(int i = 0; i < dimensions; i++) {
			int v1 = this.values[i];
			int v2 = that.values[i];
			if(v1 > v2) {
				return +1;
			}
			if(v1 < v2) {
				return -1;
			}
		}
		
		if(this.dimensions() > dimensions) {
			return +1;
		}
		
		if(that.dimensions() > dimensions) {
			return -1;
		}
		
		return 0;
	}

	@Override
	public int getID() {
		return hashCode;
	}

	@Override
	public int[] getData() {
		return values;
	}
	
}