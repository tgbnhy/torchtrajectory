package au.edu.rmit.bdm.clustering.trajectory.kpaths;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class TestNewMethods extends Process {

	public TestNewMethods(String datapath) {
		super(datapath);
	}
	
	/*
	 * test the triangle inequality
	 */
	public void testTriangular() {
		System.out.println("Start testing");
		while(true) {
			Random rand = new Random();
			int  n = rand.nextInt(trajectoryNumber) + 1;
			System.out.print(n+" ");
			int[] A = datamap.get(n);
			n = rand.nextInt(trajectoryNumber) + 1;
			System.out.print(n+" ");
			int[] B = datamap.get(n);
			n = rand.nextInt(trajectoryNumber) + 1;
			System.out.print(n+" ");
			int[] C = datamap.get(n);
			Integer[] tra_A = Arrays.stream(A).boxed().toArray(Integer[]::new);
			Integer[] tra_B = Arrays.stream(B).boxed().toArray(Integer[]::new);		
			Integer[] tra_C = Arrays.stream(C).boxed().toArray(Integer[]::new);

	/*		double A_B = VIseries.EDRDistanceJaccard(tra_A, tra_B);// compute the distance to the new center
			double A_C = VIseries.EDRDistanceJaccard(tra_A, tra_C);// compute the distance to the new center
			double B_C = VIseries.EDRDistanceJaccard(tra_B, tra_C);// compute the distance to the new center
			if(A_B+A_C<B_C)
				break;
			if(A_B+B_C<A_C)
				break;
			if(A_C+B_C<A_B)
				break;*/
			System.out.println();
		}
	//	System.out.println("End testing");
	}
	
	public static void main(String[] args) throws IOException {
	//	trajectoryNumber=100000;
	//	testTriangular();
	}
}
