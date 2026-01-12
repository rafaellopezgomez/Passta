package learning_algorithm;

import automaton.EDRTA;
import edge.EDRTAEdge;
import parser.Parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;

import state.EDRTAState;
import trace.Observation;
import trace.Trace;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.graphper.draw.ExecuteException;

public class Passta {
	private List<Trace> traces;
	private EDRTA automaton;
	private final int k;
	boolean initVars; // Flag used to indicate that the variables of the initial state are correct

	/**
	 * Default constructor of the learning algorithm class
	 *
	 * @return LearningAlgorithm instance
	 */
	public Passta(String src, int k) {
		this.k = k;
		File resourcesFile = new File(src);
		var traces = readTraces(resourcesFile.getAbsoluteFile());
		learn(traces);
	}

	public Passta(ArrayList<Trace> traces, int k) {
		this.k = k;
		learn(traces);
	}

	/*
	 * public LearningAlgorithm(int k) { this.k = k; initVars = false; // learn(); }
	 */

	private void learn(ArrayList<Trace> traces) {
		initVars = false;
		this.traces = compressTraces(traces);
		phase1();
		phase2();
		computeInvariants();
	}

	/*
	 * public void learnNewTraces(ArrayList<Trace> traces) { this.traces =
	 * compressTraces(traces); phase1(); }
	 */

	/*
	 * public void stopLearning() { phase2(); }
	 */

	public EDRTA getEDRTA() {
		return automaton;
	}

	/**
	 * Method to read traces from JSON
	 *
	 * @param source source, file in JSON format where the traces are stored
	 * @return A list of the processed traces
	 */
	public static ArrayList<Trace> readTraces(File source) {

		try {
			ObjectMapper mapper = JsonMapper.builder().addModule(new BlackbirdModule()).build()
					.enable(SerializationFeature.INDENT_OUTPUT)
					.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

			return mapper.readValue(source, new TypeReference<ArrayList<Trace>>() {
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Method to perform a compressing operation in the input traces. The
	 * compression operations are: 1. If consecutive observations have not an event
	 * and all have the same variable parameters, then they are fused as one that
	 * compress all the information. 2. If there is an event observation and the
	 * observations that come consecutively later haven´t an event and they have the
	 * same variables as the one with the event then the same fusion operation is
	 * performed.
	 *
	 * @return traces
	 */
	public static ArrayList<Trace> compressTraces(ArrayList<Trace> traces) {
		for (Trace trace : traces) {
			var obs = trace.getObs();
			ArrayList<Observation> newObs = new ArrayList<Observation>();
			for (int i = 0; i < obs.size(); i++) {
				var currentOb = obs.get(i);
				Observation lastObEq = null;
				int j = i + 1;
				while (j < obs.size() && lastObEq == null) {
					var futOb = obs.get(j);
					if (!checkEqOb(currentOb, futOb)) {
						lastObEq = obs.get(j - 1);
					} else {
						j++;
					}
				}

				newObs.add(currentOb);
				i = j - 1; // Next loop iteration will start with i = j (First different Observation)
			}
			trace.setObs(newObs);
		}
		return traces;
	}

	/**
	 * Method to compare two observations
	 *
	 * @param ob1
	 * @param ob2
	 * @return true if both observations are equivalent, false otherwise
	 */
	private static boolean checkEqOb(Observation ob1, Observation ob2) {
		var currentEvent = ob1.event();
		var futureEvent = ob2.event();
		if ((currentEvent.isEmpty() && currentEvent.equals(futureEvent)) || // Both observation haven´t an event
				(!currentEvent.isEmpty() && futureEvent.isEmpty())) { // First observation has an event but second not
			var currentVariables = ob1.variables();
			var futureVariables = ob2.variables();
			return currentVariables.size() == futureVariables.size() // Both Observations have the same variables
					&& currentVariables.containsAll(futureVariables);
		}
		return false;
	}

	/**
	 * Construct the barebone of the automata
	 */
	private void phase1() {
		if (automaton == null)
			automaton = new EDRTA();

		for (var trace : traces) {
			processTrace(trace);
		}
	}

	private void processTrace(Trace trace) {
		var obs = trace.getObs();
		var ob = obs.get(0);
		double currentTime = ob.time();
		double lastTime = (double) 0;
		double delta = currentTime == 0 ? 0 : getDelta(currentTime, lastTime);
		int i = 1;
		EDRTAState qo = automaton.isEmpty() ? null : automaton.getState(0); // Last visited state
		EDRTAState qt = null; // Target state from qo

		if (qo == null) { // If the automata is empty, create the first state
			qo = automaton.addState(new ArrayList<String>(Arrays.asList("Unknown")));
			if (ob.event().isEmpty()) {
				qt = automaton.addState(ob.variables());
				initVars = true;
				var e = automaton.addEdge(qo, qt, 0.0, 0.0, "□"); // New
				e.addSample(0); // New
			} else { // If there is an event in the first observation
				qt = automaton.addState(ob.variables());
				var e = automaton.addEdge(qo, qt, delta, delta, ob.event()); // New
				e.addSample(delta); // New
			}
		} else { // In an existing automata, new traces start in the first state
			if (ob.event().isEmpty()) {
				qt = automaton.searchStateFromSource(qo, "□", ob.variables());
			} else {
				qt = automaton.searchStateFromSource(qo, ob.event(), ob.variables());
			}
			
			if (ob.event().isEmpty()) {
				/*
				 * Check if variables are equal. If not, trace without event will have priority
				 * to correct the variables assumption from another trace starting with an event
				 * (both initial state and its consecutive will have the same variables).
				 *
				 * If there was a previous observation without event in the initial state and
				 * the initial state variables differ with the current observation, a
				 * consistency error is raised
				 *
				 */
				if (qt != null) {
					if (initVars && !qt.getAttrs().equals(ob.variables())) {
						throw new RuntimeException(qt.toString() + "\n" + "Have an inconsistency between attributes \n"
								+ "Current attributes: " + qt.getAttrs().toString() + "\n" + "Observation attributes: "
								+ ob.variables().toString());
					} else if (!initVars) {
						qo.setAttrs(ob.variables());
						initVars = true;
					}
				} else {
					throw new RuntimeException("System attributes does not match");
				}
			} else { // If there is an event in the initial ob, two states are considered
				qt = automaton.searchStateFromSource(qo, ob.event(), ob.variables());

				if (qt != null) { // If there is an existing state, update the guards
					var e = automaton.updateGuard(qo, qt, delta, ob.event());
					e.addSample(delta); // New
				} else { // In other case, create a new state and an edge
					// k + 1 because arrays skip last explicit index
					// int futureIdx = Math.min(0 + (k + 1),trace.getObs().size());
					qt = Math.min((k + 1), trace.getObs().size()) == (k + 1)
							? fastMatching(trace.getObs().subList(0, k + 1), qo, lastTime)
							: null;
					if (qt != null) {
						i += k;
					} else {
						qt = automaton.addState(ob.variables());
						var e = automaton.addEdge(qo, qt, delta, delta, ob.event()); // New
						e.addSample(delta); // New
					}
				}
			}
		}
		qo = qt; // Check
		lastTime = currentTime;

		while (i < trace.getObs().size()) {
			ob = obs.get(i);
			String event = ob.event().isEmpty() ? "□" : ob.event();
			qt = qo != null ? automaton.searchStateFromSource(qo, event, ob.variables()) : null;
			

			if (qt != null) { // If there is an existing state, update the guards
				currentTime = ob.time();
				delta = getDelta(currentTime, lastTime);
				var e = automaton.updateGuard(qo, qt, delta, event);
				e.addSample(delta); // New
			} else { // In other case, create a new state and an edge
				// k + 1 because arrays skip last explicit index
				int futureIdx = Math.min(i + (k + 1), trace.getObs().size());
				qt = (futureIdx - i) == (k + 1) ? fastMatching(trace.getObs().subList(i, futureIdx), qo, lastTime)
						: null;
				if (qt != null) {
					i += k;
					ob = obs.get(i);
					currentTime = ob.time();
				} else {
					qt = automaton.addState(ob.variables());
					currentTime = ob.time();
					delta = getDelta(currentTime, lastTime);
					var e = automaton.addEdge(qo, qt, delta, delta, event); // New
					e.addSample(delta); // New
				}
			}
			qo = qt;
			lastTime = currentTime;
			i += 1;
		}
	}

	private double getDelta(double currentTime, double lastTime) {
		double delta = currentTime - lastTime;
		if (delta <= 0)
			throw new RuntimeException("There is not elapse time between observations");
		return delta;
	}

	/**
	 * Method that tries to compute a merge operation given the last observation and
	 * the future k observations, this merge "on the fly" tries to reduce the space
	 * and time cost of the learning process. The method compares the observation
	 * and all existing states in the automaton in order to perform a "equivalence
	 * merge operation" with the first k future equal state found (only perform the
	 * merge operation between two states in each call).
	 *
	 * @param obsWindow (current observation and its k future observations)
	 * @param qo        Last visited state
	 * @param lastTime
	 * @return Last state in fast merge or null if can not be performed
	 */
	private EDRTAState fastMatching(List<Observation> obsWindow, EDRTAState qo, double lastTime) {
		// List of all states that are possible candidates to perform a merge operation
		ArrayList<EDRTAState> pEqStates = automaton.getAllStates().stream().filter(state -> {
			var vars = state.getAttrs();
			return obsWindow.get(0).variables().equals(vars);
		}).collect(Collectors.toCollection(ArrayList::new));

		if (pEqStates.isEmpty())
			return null;

		// Build the k future of the current observation given an observation window of
		// length k
		ArrayList<Object> obFut = new ArrayList<>(); // Future of the observation
		Iterator<Observation> it = obsWindow.iterator();
		var lastObservation = it.next(); // Skip current observation to construct its future
		double delta = getDelta(lastObservation.time(), lastTime);
		double auxLastTime = lastObservation.time();
		while (it.hasNext()) {
			lastObservation = it.next();
			double auxTimeDelta = getDelta(lastObservation.time(), auxLastTime);
			auxLastTime = lastObservation.time();
			String event = lastObservation.event().isEmpty() ? "□" : lastObservation.event();
			var edge = new EDRTAEdge(-1, -1, -1, auxTimeDelta, auxTimeDelta, event);
			var state = new EDRTAState(-1, lastObservation.variables());
			obFut.add(edge);
			obFut.add(state);
		}

		// Loop through all candidates to try to perform a merge operation
		for (EDRTAState currentEqState : pEqStates) {
			var futuresOfCandidate = getKFutures(currentEqState);

			// Search for a future that is equal to the observation future
			Optional<ArrayList<Object>> sameFuture = futuresOfCandidate.stream().filter(future -> {
				return fmComparison(obFut, future);
			}).findFirst();

			if (sameFuture.isPresent()) { // If there is an equivalent future
				var e = automaton.addEdge(qo, currentEqState, delta, delta, obsWindow.get(0).event());
				e.addSample(delta); // New
				Iterator<Object> itSameFuture = sameFuture.get().iterator();
				Iterator<Object> itObservationFuture = obFut.iterator();
				while (itSameFuture.hasNext()) {
					var stateOrEdge1 = itSameFuture.next();
					var stateOrEdge2 = itObservationFuture.next();
					if (stateOrEdge1 instanceof EDRTAEdge edge1 && stateOrEdge2 instanceof EDRTAEdge edge2) {
						// Update the guards of every edge in the equivalent future given the source and
						// the target states and the time delta of the current observation k futures
						e = automaton.updateGuard(automaton.getState(edge1.getSourceId()),
								automaton.getState(edge1.getTargetId()), edge2.getGuard().get(0), edge1.getEvent());
						e.addSample(edge2.getGuard().get(0)); // New
					} else {
						qo = (EDRTAState) stateOrEdge1;
					}
				}
				return qo; // Return the new merged state
			}
		}
		return null;
	}

	/**
	 * Method to obtain the k futures of a given state
	 *
	 * @param qo
	 * @return futures, a list of all possible future paths with the form [edge,
	 *         state, edge...state]. For example: 3 Futures (future of depth 3) of
	 *         "S0" could be [[edge0, S1, edge2, S2, edge4, S4],[edge1, S2, edge3,
	 *         S3, edge5, S5]]
	 */
	private ArrayList<ArrayList<Object>> getKFutures(EDRTAState qo) {
		ArrayList<ArrayList<Object>> futures = new ArrayList<>();

		for (int idEdge : qo.getOutEdges()) {
			int idQt = automaton.getEdge(idEdge).getTargetId();
			var edge = automaton.getEdge(idEdge);
			var qt = automaton.getState(idQt);
			futures.addAll(getKFuturesAux(new ArrayList<Object>(Arrays.asList(edge, qt)), 2));
		}
		return futures;
	}

	/**
	 * Auxiliary method to perform recursion in order to discover all possible k
	 * future path
	 *
	 * @param currentPath, Current future path
	 * @param level,       level of recursion
	 * @return futures, a list of all possible future paths with the form [edge,
	 *         state, edge...state]. For example: 3 Futures (future of depth 3) of
	 *         "S0" could be [[edge0, S1, edge2, S2, edge4, S4],[edge1, S2, edge3,
	 *         S3, edge5, S5]]
	 */
	private ArrayList<ArrayList<Object>> getKFuturesAux(ArrayList<Object> currentPath, int level) {
		ArrayList<ArrayList<Object>> futures = new ArrayList<>();
		if (level <= k) {
			if (((EDRTAState) currentPath.get(currentPath.size() - 1)).getOutEdges().isEmpty()) {
				futures.add(currentPath);
			} else {
				for (int idEdge : ((EDRTAState) currentPath.get(currentPath.size() - 1)).getOutEdges()) {
					int idOutState = automaton.getEdge(idEdge).getTargetId();
					var edge = automaton.getEdge(idEdge);
					var outState = automaton.getState(idOutState);
					var newPath = new ArrayList<Object>(currentPath);
					newPath.add(edge);
					newPath.add(outState);
					futures.addAll(getKFuturesAux(newPath, level + 1));
				}
			}
		} else {
			futures.add(currentPath);
		}
		return futures;
	}

	/**
	 * Method that performs a comparison between two future paths (trace and
	 * automaton)
	 *
	 * @param future1
	 * @param future2
	 * @return true if both futures are equivalent (same events, variables and
	 *         length), false otherwise
	 */
	private boolean fmComparison(ArrayList<Object> future1, ArrayList<Object> future2) {
		var size1 = future1.size();
		var size2 = future2.size();
		if (size1 == size2) {
			Iterator<Object> it2 = future2.iterator();

			return future1.stream().allMatch(stateOrEdge1 -> {
				var stateOrEdge2 = it2.next();
				if (stateOrEdge1 instanceof EDRTAState state1 && stateOrEdge2 instanceof EDRTAState state2) {

					return state1.getAttrs().equals(state2.getAttrs());

				} else if (stateOrEdge1 instanceof EDRTAEdge edge1 && stateOrEdge2 instanceof EDRTAEdge edge2) {
					return edge1.getEvent().equals(edge2.getEvent());
				}
				return false;
			});
		}
		return false;
	}

	private void phase2() {
		boolean merged = true;
		boolean indet;
		boolean fixed = true;
		do {
			while (merged) {
				merged = lookForMerge();
			}
			var indetEdges = indetEdges();
			indet = indetEdges != null;
			if (indet) {
				fixed = fixIndet(indetEdges);
				if (fixed)
					merged = true;
			}
		} while (merged);
		if (!fixed) {
			try {
				Parser.show(automaton);
			} catch (ExecuteException e) {
				e.printStackTrace();
			}
			throw new RuntimeException("Indeterministic automaton");
		}
		merged = true;
		while (merged) {
			merged = mergeFinalStates();
		}
	}

	/**
	 * Method used to search inconsistencies in the out edges of every state.
	 * Inconsistencies checked 1.If there are two or more edges with the same event
	 * and overlapping guards.
	 *
	 * @return indeterministic edges or null otherwise
	 */
	private ArrayList<EDRTAEdge> indetEdges() {
		var states = automaton.getAllStates();

		for (EDRTAState state : states) {
			for (var idEdge : state.getOutEdges()) {
				var edge = automaton.getEdge(idEdge);
				var event = edge.getEvent();
				var dupEdges = state.getOutEdges().stream().map(automaton::getEdge).filter(e -> { // Check for
																									// inconsistencies
					if (e.getId() == edge.getId())
						return false;
					if (e.getEvent().equals(event)) {
						// boolean targetSameAttrs =
						// automaton.getState(e.getTargetId()).getAttrs().equals(automaton.getState(edge.getTargetId()).getAttrs());
						// // Same target state
						var minVal1 = edge.getMin();
						var minVal2 = e.getMin();
						var maxVal1 = edge.getMax();
						var maxVal2 = e.getMax();
						boolean overlappingGuards = ((minVal1 >= minVal2 && minVal1 <= maxVal2)
								|| (maxVal1 >= minVal2 && maxVal1 <= maxVal2))
								|| ((minVal2 >= minVal1 && minVal2 <= maxVal1) // Overlapping guards
										|| (maxVal2 >= minVal1 && maxVal2 <= maxVal1));
						return overlappingGuards; // If two edges have overlapping guards and the same
													// target state there is an inconsistency
					}
					return false;
				}).collect(Collectors.toCollection(ArrayList::new));
				if (!dupEdges.isEmpty()) {
					dupEdges.add(edge);
					return dupEdges;
				}
			}
		}
		return null;
	}

	/**
	 * Method used to merge the target states of the indeterministic edges if
	 * possible All edges have the same source state.
	 *
	 * @param indetEdges
	 * @return boolean
	 */
	private boolean fixIndet(ArrayList<EDRTAEdge> indetEdges) {
		var variables = automaton.getState(indetEdges.get(0).getTargetId()).getAttrs();
		var eqStates = indetEdges.stream().map(e -> automaton.getState(e.getTargetId()))
				.filter(s -> s.getAttrs().equals(variables)).distinct()
				.collect(Collectors.toCollection(ArrayList::new));
		if (eqStates.size() > 1) {
			var newState = eqStates.get(0);
			for (int i = 1; i < eqStates.size(); i++) {
				newState = mergeStates(newState, eqStates.get(i));
				mergeEdges(newState);
			}
			return true;
		}
		return false;
	}

	private boolean lookForMerge() {
		var states = automaton.getAllStates();

		for (EDRTAState state : states) {
			var kFutures1 = getKFutures(state);
			var possibleEquivalentState = states.stream().filter(state2 -> {
				if (state.getAttrs().equals(state2.getAttrs()) && !state.equals(state2)) {
					var kFutures2 = getKFutures(state2);
					if (!kFutures1.isEmpty() && !kFutures2.isEmpty()) {
						return compareKFutures(kFutures1, kFutures2);
					}
				}
				return false;
			}).findFirst();

			if (possibleEquivalentState.isPresent()) {
				var equivalentState = possibleEquivalentState.get();
				var mergedState = mergeStates(state, equivalentState);
				mergeEdges(mergedState);
				return true;
			}
		}
		return false;
	}

	/**
	 * Method that performs a comparison between the k-futures of two states
	 *
	 * @param fs1
	 * @param fs2
	 * @return true if both k-futures are equivalent, false otherwise
	 */
	private boolean compareKFutures(ArrayList<ArrayList<Object>> fs1, ArrayList<ArrayList<Object>> fs2) {
		var nFut1 = fs1.size();
		var nFut2 = fs2.size();

		if (nFut1 > nFut2) {
			return fs2.stream().allMatch(f2 -> {
				return fs1.stream().anyMatch(f1 -> compareFutures(f1, f2, "c2"));
			});
		} else if (nFut2 > nFut1) {
			return fs1.stream().allMatch(f1 -> {
				return fs2.stream().anyMatch(f2 -> compareFutures(f1, f2, "c2"));
			});
		} else {
			return fs2.stream().allMatch(f2 -> {
				return fs1.stream().anyMatch(f1 -> compareFutures(f1, f2, "c1"));
			});
		}
	}

	/**
	 * Method that performs a comparison between two future paths
	 *
	 * @param f1
	 * @param f2
	 * @param crit
	 * @return true if both futures are equivalent (same events, variables and
	 *         length), false otherwise
	 */
	private boolean compareFutures(ArrayList<Object> f1, ArrayList<Object> f2, String crit) {
		var size1 = f1.size();
		var size2 = f2.size();
		if (size1 == size2) {
			Iterator<Object> it2 = f2.iterator();

			return f1.stream().allMatch(stateOrEdge1 -> {
				var stateOrEdge2 = it2.next();
				if (stateOrEdge1 instanceof EDRTAState state1 && stateOrEdge2 instanceof EDRTAState state2) { // States
					return state1.getAttrs().equals(state2.getAttrs());

				} else if (stateOrEdge1 instanceof EDRTAEdge edge1 && stateOrEdge2 instanceof EDRTAEdge edge2) { // Edges
					if (!edge1.getEvent().equals(edge2.getEvent()))
						return false;
					var minVal1 = edge1.getMin();
					var minVal2 = edge2.getMin();
					var maxVal1 = edge1.getMax();
					var maxVal2 = edge2.getMax();
					if (crit.equals("c1")) {
						return ((minVal1 >= minVal2 && minVal1 <= maxVal2)
								|| (maxVal1 >= minVal2 && maxVal1 <= maxVal2))
								|| ((minVal2 >= minVal1 && minVal2 <= maxVal1)
										|| (maxVal2 >= minVal1 && maxVal2 <= maxVal1));
					} else {
						return (minVal1 >= minVal2 && minVal1 <= maxVal2) && (maxVal1 >= minVal2 && maxVal1 <= maxVal2)
								|| (minVal2 >= minVal1 && minVal2 <= maxVal1)
										&& (maxVal2 >= minVal1 && maxVal2 <= maxVal1);
					}
				}
				return false;
			});
		}
		return false;
	}

	/**
	 * Method used to merge two equivalent states
	 *
	 * @param s1
	 * @param s2
	 * @return resulting state
	 */
	private EDRTAState mergeStates(EDRTAState s1, EDRTAState s2) {
		var stateMerged = automaton.getState(Math.min(s1.getId(), s2.getId()));
		var stateAux = automaton.getState(Math.max(s1.getId(), s2.getId()));

		stateAux.getOutEdges().stream().map(idEdge -> automaton.getEdge(idEdge)).forEach(edge -> {
			edge.setSourceId(stateMerged.getId());
			stateMerged.addOutEdge(edge.getId());
		});

		stateAux.getInEdges().stream().map(idEdge -> automaton.getEdge(idEdge)).forEach(edge -> {
			edge.setTargetId(stateMerged.getId());
			stateMerged.addInEdge(edge.getId());
		});

		automaton.deleteState(stateAux.getId());

		return stateMerged;
	}

	/**
	 * This method looks for duplicate in and out edges for the input state and
	 * merge them. Duplicate edges: Same source (id), target (id), event and overlapping guards
	 *
	 * @param mergedState
	 */
	private void mergeEdges(EDRTAState mergedState) {

		// Checking outEdges, grouping them by the same event.
		var outEdges = mergedState.getOutEdges().stream().map(idEdge -> automaton.getEdge(idEdge))
				.collect(Collectors.groupingBy(EDRTAEdge::getEvent)).values().stream().filter(v -> v.size() > 1)
				.collect(Collectors.toCollection(ArrayList::new));
		mergeEdgesAux(outEdges);

		// Checking inEdges, grouping them by the same event.
		var inEdges = mergedState.getInEdges().stream().map(idEdge -> automaton.getEdge(idEdge))
				.collect(Collectors.groupingBy(EDRTAEdge::getEvent)).values().stream().filter(v -> v.size() > 1)
				.collect(Collectors.toCollection(ArrayList::new));
		mergeEdgesAux(inEdges);
	}
	
	private void mergeEdgesAux(List<List<EDRTAEdge>> edgesToCheck) {
		for (List<EDRTAEdge> possibleEqEdges : edgesToCheck) {
			while (possibleEqEdges.size() > 1) {
				ArrayList<EDRTAEdge> compared = new ArrayList<>();
				EDRTAEdge currentE = possibleEqEdges.get(0);
				ArrayList<EDRTAEdge> eqEdges = possibleEqEdges.stream().filter(e -> {
					if (e.getId() == currentE.getId())
						return false;
					var minVal1 = currentE.getMin();
					var minVal2 = e.getMin();
					var maxVal1 = currentE.getMax();
					var maxVal2 = e.getMax();
					var source1 = currentE.getSourceId();
					var source2 = e.getSourceId();
					var target1 = currentE.getTargetId();
					var target2 = e.getTargetId();
					boolean sameSource = source1 == source2; // Same source state (same id)
					boolean sameTarget = target1 == target2; // Same target state (same id)
					boolean overlappingGuards = ((minVal1 >= minVal2 && minVal1 <= maxVal2)
							|| (maxVal1 >= minVal2 && maxVal1 <= maxVal2))
							|| ((minVal2 >= minVal1 && minVal2 <= maxVal1) // Overlapping guards
									|| (maxVal2 >= minVal1 && maxVal2 <= maxVal1));
					return sameSource && sameTarget && overlappingGuards;
				}).collect(Collectors.toCollection(ArrayList::new));

				compared.add(currentE); // The current edge is checked

				if (!eqEdges.isEmpty()) { // If there is equivalent edges
					compared.addAll(eqEdges); // Add the other equivalent edges checked
					var mergedEdge = compared.stream().min(Comparator.comparing(EDRTAEdge::getId)).get(); // Only the
																											// edge with
																											// the
																											// lowest id
																											// (oldest)
																											// will
																											// remain
					var minGuard = compared.stream().min(Comparator.comparing(EDRTAEdge::getMin)).get().getMin();
					var maxGuard = compared.stream().max(Comparator.comparing(EDRTAEdge::getMax)).get().getMax();
					mergedEdge.setMin(minGuard);
					mergedEdge.setMax(maxGuard);
					compared.removeIf(e -> e.getId() == mergedEdge.getId()); // The edge is fused so another check is
																				// required
					compared.stream().forEach(edge -> {
							mergedEdge.addSamples(edge.getSamples()); // Add to the merged edge all the time samples of
																		// the eq edges that are going to be removed
					});
					
					compared.stream().map(EDRTAEdge::getId).forEach(e -> automaton.deleteEdge(e));
				}
				possibleEqEdges.removeAll(compared);
			}

		}
	}

	/**
	 * This method tries to merge leaf states
	 */
	private boolean mergeFinalStates() {
		var states = automaton.getAllStates();
		var finalStates = states.stream().filter(state -> state.getOutEdges().size() == 0)
				.collect(Collectors.toCollection(ArrayList::new));

		for (EDRTAState finalS : finalStates) {
			var possibleEquivalentLeave = finalStates.stream().filter(leave2 -> {
				if (!finalS.equals(leave2) && finalS.getAttrs().equals(leave2.getAttrs())) {
					var edgesInLeave1 = finalS.getInEdges().stream().map(idEdge -> automaton.getEdge(idEdge))
							.collect(Collectors.toCollection(ArrayList::new));
					var edgesInLeave2 = leave2.getInEdges().stream().map(idEdge -> automaton.getEdge(idEdge))
							.collect(Collectors.toCollection(ArrayList::new));

					if (!edgesInLeave1.isEmpty() && !edgesInLeave2.isEmpty()) {
						if (edgesInLeave1.size() > edgesInLeave2.size()) {
							return edgesInLeave2.stream().allMatch(edge2 -> {
								return edgesInLeave1.stream()
										.anyMatch(edge -> edge.getEvent().equals(edge2.getEvent()));
							});
						} else {
							return edgesInLeave1.stream().allMatch(edge -> {
								return edgesInLeave2.stream()
										.anyMatch(edge2 -> edge2.getEvent().equals(edge.getEvent()));
							});
						}
					}
				}
				return false;
			}).findFirst();

			if (possibleEquivalentLeave.isPresent()) {
				var equivalentLeave = possibleEquivalentLeave.get();
				var mergedLeave = mergeStates(finalS, equivalentLeave);
				mergeEdges(mergedLeave);
				return true;
			}
		}
		return false;
	}
	
    private void computeInvariants() {
        automaton.getAllStates().stream().forEach(state -> {
            if (!state.getOutEdges().isEmpty()) {
                EDRTAEdge edge = state.getOutEdges().stream().map(idEdge -> automaton.getEdge(idEdge)).max(Comparator.comparing(EDRTAEdge::getMax)).get();
                Double invariant = edge.getMax();
                state.setInvariant(invariant);
            }
        });
    }
}
