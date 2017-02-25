package dc_project1_bellman_ford;


/**
 * Team Members:
 * Sujal Patel (ssp150930)
 * Harshil Shah (hxs155030)
 * Sagar Mehta (sam150930)
 *
 * This class is the object for edges. 
 */
public class Edge {

	private Processes P1,P2;
	private int Weight;
	
	public Edge(Processes p1, Processes p2, int weight) {
		
		this.P1 = p1;
		this.P2 = p2;
		this.Weight = weight;
	}

	public Processes getP1() {
		return P1;
	}

	public Processes getP2() {
		return P2;
	}

	public int getWeight() {
		return Weight;
	}
	
	public Processes getNeighbour(Processes P){
		if(P == P1)
			return P2;
		return P1;
	}
}
