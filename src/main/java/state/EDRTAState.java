package state;

import java.util.ArrayList;

public class EDRTAState {
	private int id;
	private ArrayList<String> attrs;
	private ArrayList<Integer> inEdges;
	private ArrayList<Integer> outEdges;
	
	public EDRTAState(int id, ArrayList<String> attrs) {
		this.id = id;
		this.attrs = attrs;
		inEdges = new ArrayList<>();
		outEdges = new ArrayList<>();
	}
	
	public int getId() {
		return id;
	}
	
	public ArrayList<String> getAttrs() {
		return attrs;
	}
	
	public void setAttrs(ArrayList<String> attrs) {
		this.attrs = attrs;
	}
	
	public ArrayList<Integer> getInEdges() {
		return inEdges;
	}
	
	public void setInEdges(ArrayList<Integer> inEdges) {
		this.inEdges = inEdges;
	}
	
	public ArrayList<Integer> getOutEdges() {
		return outEdges;
	}
	
	public void setOutEdges(ArrayList<Integer> outEdges) {
		this.outEdges = outEdges;
	}

	public void addOutEdge(int idE) {
		outEdges.add(idE);	
	}

	public void addInEdge(int idE) {
		inEdges.add(idE);
	}

	@Override
	public String toString() {
		String output = "";
		if (id == 0) output += "Initial state 0 ";
		else output += "State " + id ;
		output += " [ attributes=" + attrs.toString() + ", ";
		output += "Out edges id " + outEdges.toString();
		output += " ]";
		return output;
	}	
	
}
