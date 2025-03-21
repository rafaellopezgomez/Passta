package automaton;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

import org.graphper.api.Graphviz;
import org.graphper.api.Line;
import org.graphper.api.Node;

import edge.EDRTAEdge;
import state.EDRTAState;

import org.graphper.api.Graphviz.GraphvizBuilder;

/**
 * This class implements the Automaton data structure (Dictionaries of states
 * and edges)
 * 
 * @author Rafael
 *
 */

public class EDRTA {
	private Map<Integer, EDRTAState> states;
	private Map<Integer, EDRTAEdge> edges;
	private int stateId; // Incremental value to identify states. Default 0
	private int edgeId; // Incremental value to identify edges. Default 0
	private boolean prob;

	/**
	 * Constructor that initializes an empty automata
	 */
	public EDRTA() {
		states = new TreeMap<Integer, EDRTAState>();
		edges = new TreeMap<Integer, EDRTAEdge>();
		prob = false;
	}
	
	public boolean hasProbs() {
		return prob;
	}

	public boolean isEmpty() {
		return states.isEmpty();
	}

	public EDRTAState getState(int stateId) {
		return states.get(stateId);
	}

	public Collection<EDRTAState> getAllStates() {
		return states.values();
	}

	/**
	 * Method that returns the edge that match with the id of the input argument
	 * 
	 * @param edgeId id of the edge
	 * @return the edge with the given id or null otherwise
	 */
	public EDRTAEdge getEdge(int edgeId) {
		return edges.get(edgeId);
	}

	public Collection<EDRTAEdge> getAllEdges() {
		return edges.values();
	}

	/**
	 * Method to add a new state in the automata given the observation variables
	 * 
	 * @param vars variables of the current observation
	 * @return the new state
	 */
	public EDRTAState addState(ArrayList<String> vars) {
		var newState = new EDRTAState(stateId, vars);
		states.put(stateId, newState);
		stateId++;
		return newState;
	}


	/**
	 * Method to add a new Edge in the automata given the source and target states,
	 * the time passed in the source state and the event ocurred
	 * 
	 * @param sourceState
	 * @param targetState
	 * @param min
	 * @param max
	 * @param event
	 * @return the new edge
	 */
	public EDRTAEdge addEdge(EDRTAState sourceState, EDRTAState targetState, double min, double max, String event) {
		var newEdge = new EDRTAEdge(edgeId, sourceState.getId(), targetState.getId(), min, max, event);
		sourceState.addOutEdge(edgeId);
		targetState.addInEdge(edgeId);
		edges.put(edgeId, newEdge);
		edgeId++;
		return newEdge;
	}

	public void deleteState(int stateId) {
		states.remove(stateId);
	}

	public void deleteEdge(int edgeId) {
		var deletingEdge = edges.get(edgeId);
		var sourceState = states.get(deletingEdge.getSourceId());
		var targetState = states.get(deletingEdge.getTargetId());
		sourceState.getOutEdges().removeIf(e -> edgeId == e);
		targetState.getInEdges().removeIf(e -> edgeId == e);
		edges.remove(edgeId);
	}

	public EDRTAState searchStateFromSource(EDRTAState sourceState, String event, ArrayList<String> vars) {
		Optional<EDRTAState> searchedState = sourceState.getOutEdges().stream().parallel().filter(indexEdge -> {
			var edge = edges.get(indexEdge);
			if (edge.getEvent().equals(event)) {
				var targetState = states.get(edge.getTargetId());
				return targetState.getAttrs().equals(vars);
			}
			return false;
		}).map(indexEdge -> states.get(edges.get(indexEdge).getTargetId())).findFirst();
		return searchedState.orElse(null);
	}

	public Optional<EDRTAEdge> searchEdge(EDRTAState sourceState, EDRTAState nextState, String event) {
        return sourceState.getOutEdges().stream().parallel()
				.map(indexEdge -> edges.get(indexEdge)).filter(e -> {
					return e.getTargetId() == nextState.getId() && e.getEvent().equals(event);
				}).findFirst();
	}

	public EDRTAEdge updateGuard(EDRTAState sourceState, EDRTAState nextState, double timeDelta, String event) {
		Optional<EDRTAEdge> searchedEdge = searchEdge(sourceState, nextState, event);
		EDRTAEdge edge = null;
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
		String output = "\n ############### States ############### \n";
		output += states.values().stream().map(Object::toString).collect(Collectors.joining("\n"));
		output += "\n ############### Edges ############### \n";
		output += edges.values().stream().map(Object::toString).collect(Collectors.joining("\n"));
		System.out.println(output);
	}
	
	public Graphviz toDOTLayout() {
		GraphvizBuilder g = Graphviz.digraph();
		
		try {
			Map<String, Node> nodes = new TreeMap<String, Node>();

			getAllStates().stream().forEach(state -> {
				var id = String.valueOf(state.getId());
				var variables = state.getAttrs().toString();
				var node = Node.builder().label(id + " " + variables).build();
				nodes.put(id, node);
				g.addNode(node);
			});

			getAllEdges().stream().forEach(edge -> {
				Node source = nodes.get(String.valueOf(edge.getSourceId()));
				Node target = nodes.get(String.valueOf(edge.getTargetId()));
				String prob = edge.getProb() == null ? "" :  new DecimalFormat("#########.####", new DecimalFormatSymbols(Locale.ENGLISH)).format(edge.getProb());
				String probLabel = prob != null ? " Prob: { " + prob + " }" : "";
				var line = Line.builder(source, target)
						.label("{" + edge.getEvent() + "}" + " " + edge.getGuard().toString() + probLabel).build();
				g.addLine(line);
			});
		} catch(RuntimeException e) {
			e.printStackTrace();
		}

		return g.build();
	}
	
	public void computeProbs() {
		if (!prob) {
			for(var state : states.values()) {
				var accumSamples = state.getOutEdges().stream().mapToDouble(e -> edges.get(e).getSamples().size()).sum();  // Already casted to double
				state.getOutEdges().stream().map(e -> edges.get(e)).forEach(e -> e.setProb(((double) e.getSamples().size()) / accumSamples));
			}
			prob = true;
		}
	}
}
