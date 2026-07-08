package es.uma.morse.passta.core.automaton;


import java.util.*;
import java.util.stream.Collectors;

/**
 * This class implements the Automaton data structure (Dictionaries of locations
 * and edges)
 * 
 * @author Rafael
 *
 */

public class SRTA {
	private Map<Integer, SRTALocation> locations;
	private Map<Integer, SRTAEdge> edges;
	private int locId; // Incremental value to identify locations. Default 0
	private int edgeId; // Incremental value to identify edges. Default 0

	/**
	 * Constructor that initializes an empty automaton
	 */
	public SRTA() {
		locations = new TreeMap<Integer, SRTALocation>();
		edges = new TreeMap<Integer, SRTAEdge>();
	}

	public boolean isEmpty() {
		return locations.isEmpty();
	}

	public SRTALocation getLocation(int id) {
		return locations.get(id);
	}

	public Collection<SRTALocation> getAllLocations() {
		return locations.values();
	}

	/**
	 * Method that returns the edge which matches with the id of the input argument
	 * 
	 * @param id of the edge
	 * @return the edge with the given id or null otherwise
	 */
	public SRTAEdge getEdge(int id) {
		return edges.get(id);
	}

	public Collection<SRTAEdge> getAllEdges() {
		return edges.values();
	}

	/**
	 * Method to add a new location in the automaton given the system attributes
	 * 
	 * @param vars observable system attributes of the current observation
	 * @return the new location
	 */
	public SRTALocation addLocation(List<String> attrs) {
		var newLoc = new SRTALocation(locId, attrs);
		locations.put(locId, newLoc);
		locId++;
		return newLoc;
	}


	/**
	 * Method to add a new Edge in the automata given the source and target locations,
	 * the time passed in the source location and the event occurred
	 * 
	 * @param srcLoc
	 * @param targLoc
	 * @param min
	 * @param max
	 * @param event
	 * @return the new edge
	 */
	public SRTAEdge addEdge(SRTALocation srcLoc, SRTALocation targLoc, double min, double max, String event) {
		var newEdge = new SRTAEdge(edgeId, srcLoc.getId(), targLoc.getId(), min, max, event);
		srcLoc.addOutEdge(edgeId);
		targLoc.addInEdge(edgeId);
		edges.put(edgeId, newEdge);
		edgeId++;
		return newEdge;
	}

	public void deleteLocation(int id) {
		locations.remove(id);
	}

	public void deleteEdge(int id) {
		var deletingEdge = edges.get(id);
		var sourceState = locations.get(deletingEdge.getSourceId());
		var targetState = locations.get(deletingEdge.getTargetId());
		sourceState.getOutEdges().removeIf(e -> id == e);
		targetState.getInEdges().removeIf(e -> id == e);
		edges.remove(id);
	}

	public SRTALocation searchLocationFromSource(SRTALocation srcLoc, String event, ArrayList<String> attrs) {
		Optional<SRTALocation> searchedLoc = srcLoc.getOutEdges().stream().parallel().filter(indexEdge -> {
			var edge = edges.get(indexEdge);
			if (edge.getEvent().equals(event)) {
				var targLoc = locations.get(edge.getTargetId());
				return targLoc.getAttrs().equals(attrs);
			}
			return false;
		}).map(indexEdge -> locations.get(edges.get(indexEdge).getTargetId())).findFirst();
		return searchedLoc.orElse(null);
	}

	public Optional<SRTAEdge> searchEdge(SRTALocation srcLoc, SRTALocation nextLoc, String event) {
        return srcLoc.getOutEdges().stream().parallel()
				.map(indexEdge -> edges.get(indexEdge)).filter(e -> {
					return e.getTargetId() == nextLoc.getId() && e.getEvent().equals(event);
				}).findFirst();
	}

	public SRTAEdge updateGuard(SRTALocation srcLoc, SRTALocation nextLoc, double timeDelta, String event) {
		Optional<SRTAEdge> searchedEdge = searchEdge(srcLoc, nextLoc, event);
		SRTAEdge edge = null;
		if (searchedEdge.isPresent()) {
			edge = searchedEdge.get();

			double min = Math.min(edge.getMin(), timeDelta);
			double max = Math.max(edge.getMax(), timeDelta);

			edge.setMin(min);
			edge.setMax(max);
		}
		return edge;
	}

	/**
	 * Method that prints the automata in console
	 */
	public void print() {
		String output = "\n ############### Locations ############### \n";
		output += locations.values().stream().map(Object::toString).collect(Collectors.joining("\n"));
		output += "\n ############### Edges ############### \n";
		output += edges.values().stream().map(Object::toString).collect(Collectors.joining("\n"));
		System.out.println(output);
	}
}
