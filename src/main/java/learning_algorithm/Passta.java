package learning_algorithm;

import automaton.SRTA;
import edge.SRTAEdge;
import location.SRTALocation;
import parser.JsonSupport;
import parser.Parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;

import trace.Observation;
import trace.Trace;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.graphper.draw.ExecuteException;

public class Passta {
	private List<Trace> traces;
	private SRTA automaton;
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

	public Passta(List<Trace> traces, int k) {
		this.k = k;
		learn(traces);
	}

	/*
	 * public LearningAlgorithm(int k) { this.k = k; initVars = false; // learn(); }
	 */

	private void learn(List<Trace> traces2) {
		initVars = false;
		this.traces = compressTraces(traces2);
		phase1();
		phase2();
		phase3();
	}

	/*
	 * public void learnNewTraces(ArrayList<Trace> traces) { this.traces =
	 * compressTraces(traces); phase1(); }
	 */

	/*
	 * public void stopLearning() { phase2(); }
	 */

	public SRTA getAutomaton() {
		return automaton;
	}

	/**
	 * Method to read traces from JSON
	 *
	 * @param source source, file in JSON format where the traces are stored
	 * @return A list of the processed traces
	 */
	public static List<Trace> readTraces(File source) {
		if (source == null) {
			throw new IllegalArgumentException("Source file is null");
		}
		if (!source.isFile()) {
			throw new IllegalArgumentException("Source is not a file: " + source.getAbsolutePath());
		}

		try {
			return JsonSupport.tracesReader().readValue(source);
		} catch (IOException e) {
			throw new RuntimeException("Cannot read traces from: " + source.getAbsolutePath(), e);
		}
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
	public static List<Trace> compressTraces(List<Trace> traces2) {
		for (Trace trace : traces2) {
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
		return traces2;
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
	 * Construct the barebone of the automaton
	 */
	private void phase1() {
		if (automaton == null)
			automaton = new SRTA();

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
		SRTALocation qo = automaton.isEmpty() ? null : automaton.getLocation(0); // Last visited location
		SRTALocation qt = null; // Target state from qo

		if (qo == null) { // If the automata is empty, create the first location
			qo = automaton.addLocation(new ArrayList<String>(Arrays.asList("Unknown")));
			if (ob.event().isEmpty()) {
				qt = automaton.addLocation(ob.variables());
				initVars = true;
				var e = automaton.addEdge(qo, qt, 0.0, 0.0, "□"); // New
				e.addSample(0); // New
			} else { // If there is an event in the first observation
				qt = automaton.addLocation(ob.variables());
				var e = automaton.addEdge(qo, qt, delta, delta, ob.event()); // New
				e.addSample(delta); // New
			}
		} else { // In an existing automata, new traces start in the first location
			if (ob.event().isEmpty()) {
				qt = automaton.searchLocationFromSource(qo, "□", ob.variables());
			} else {
				qt = automaton.searchLocationFromSource(qo, ob.event(), ob.variables());
			}

			if (ob.event().isEmpty()) {
				/*
				 * Check if the system attributes are equal. If not, a trace without event will
				 * have priority to correct the attributes assumption from another trace
				 * starting with an event (both initial location and its consecutive will have
				 * the same attributes).
				 *
				 * If there was a previous observation without event in the initial location and
				 * its initial system attributes differ with the current observation, a
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
					throw new RuntimeException("System attributes do not match");
				}
			} else { // If there is an event in the initial ob, two locations are considered
				qt = automaton.searchLocationFromSource(qo, ob.event(), ob.variables());

				if (qt != null) { // If there is an existing location, update the guards
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
						qt = automaton.addLocation(ob.variables());
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
			qt = qo != null ? automaton.searchLocationFromSource(qo, event, ob.variables()) : null;

			if (qt != null) { // If there is an existing location, update the guards
				currentTime = ob.time();
				delta = getDelta(currentTime, lastTime);
				var e = automaton.updateGuard(qo, qt, delta, event);
				e.addSample(delta); // New
			} else { // In other case, create a new location and an edge
				// k + 1 because arrays skip last explicit index
				int futureIdx = Math.min(i + (k + 1), trace.getObs().size());
				qt = (futureIdx - i) == (k + 1) ? fastMatching(trace.getObs().subList(i, futureIdx), qo, lastTime)
						: null;
				if (qt != null) {
					i += k;
					ob = obs.get(i);
					currentTime = ob.time();
				} else {
					qt = automaton.addLocation(ob.variables());
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
	 * and all existing locations in the automaton in order to perform a
	 * "equivalence merge operation".
	 *
	 * @param obsWindow (current observation and its k future observations)
	 * @param qo        Last visited location
	 * @param lastTime
	 * @return Last location in fast merge or null if can not be performed
	 */
	private SRTALocation fastMatching(List<Observation> obsWindow, SRTALocation qo, double lastTime) {
		// List of all states that are possible candidates to perform a merge operation
		ArrayList<SRTALocation> pEqLocs = automaton.getAllLocations().stream().filter(state -> {
			var attrs = state.getAttrs();
			return obsWindow.get(0).variables().equals(attrs);
		}).collect(Collectors.toCollection(ArrayList::new));

		if (pEqLocs.isEmpty())
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
			var edge = new SRTAEdge(-1, -1, -1, auxTimeDelta, auxTimeDelta, event);
			var state = new SRTALocation(-1, lastObservation.variables());
			obFut.add(edge);
			obFut.add(state);
		}

		// Loop through all candidates to try to perform a merge operation
		for (SRTALocation currentEqLoc : pEqLocs) {
			var futuresOfCandidate = getKFutures(currentEqLoc);

			// Search for a future that is equal to the observation future
			Optional<ArrayList<Object>> sameFuture = futuresOfCandidate.stream().filter(future -> {
				return fmComparison(obFut, future);
			}).findFirst();

			if (sameFuture.isPresent()) { // If there is an equivalent future
				var e = automaton.addEdge(qo, currentEqLoc, delta, delta, obsWindow.get(0).event());
				e.addSample(delta); // New
				Iterator<Object> itSameFuture = sameFuture.get().iterator();
				Iterator<Object> itObservationFuture = obFut.iterator();
				while (itSameFuture.hasNext()) {
					var stateOrEdge1 = itSameFuture.next();
					var stateOrEdge2 = itObservationFuture.next();
					if (stateOrEdge1 instanceof SRTAEdge edge1 && stateOrEdge2 instanceof SRTAEdge edge2) {
						// Update the guards of every edge in the equivalent future given the source and
						// the target states and the time delta of the current observation k futures
						e = automaton.updateGuard(automaton.getLocation(edge1.getSourceId()),
								automaton.getLocation(edge1.getTargetId()), edge2.getGuard().get(0), edge1.getEvent());
						e.addSample(edge2.getGuard().get(0)); // New
					} else {
						qo = (SRTALocation) stateOrEdge1;
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
	private ArrayList<ArrayList<Object>> getKFutures(SRTALocation qo) {
		ArrayList<ArrayList<Object>> futures = new ArrayList<>();

		for (int idEdge : qo.getOutEdges()) {
			int idQt = automaton.getEdge(idEdge).getTargetId();
			var edge = automaton.getEdge(idEdge);
			var qt = automaton.getLocation(idQt);
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
			if (((SRTALocation) currentPath.get(currentPath.size() - 1)).getOutEdges().isEmpty()) {
				futures.add(currentPath);
			} else {
				for (int idEdge : ((SRTALocation) currentPath.get(currentPath.size() - 1)).getOutEdges()) {
					int idOutState = automaton.getEdge(idEdge).getTargetId();
					var edge = automaton.getEdge(idEdge);
					var outState = automaton.getLocation(idOutState);
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

			return future1.stream().allMatch(locOrEdge1 -> {
				var locOrEdge2 = it2.next();
				if (locOrEdge1 instanceof SRTALocation state1 && locOrEdge2 instanceof SRTALocation state2) {

					return state1.getAttrs().equals(state2.getAttrs());

				} else if (locOrEdge1 instanceof SRTAEdge edge1 && locOrEdge2 instanceof SRTAEdge edge2) {
					return edge1.getEvent().equals(edge2.getEvent());
				}
				return false;
			});
		}
		return false;
	}

	private void phase2() {
		boolean merged = false;
		boolean indet = false;
		boolean fixed = true;
		do {

			var simLocs = findSim();

			simLocs.ifPresent(list -> {
				SRTALocation first = list.get(0);
				SRTALocation second = list.get(1);
				merge(first, second);

			});

			merged = simLocs.isPresent() ? true : false;

			do {
				var indetEdges = indetEdges();
				indet = indetEdges.isPresent();
				if (indet) {
					fixed = fixIndet(indetEdges.get());
					if (fixed) {
						merged = true;
					} else {
						try {
							Parser.show(automaton);
						} catch (ExecuteException e) {
							e.printStackTrace();
						}
						throw new RuntimeException("Indeterministic automaton");
					}
				}
			} while (indet);
		} while (merged);
		merged = true;
		while (merged) {
			merged = mergeFinalLocations();
		}
	}

	/**
	 * Method used to search and merge similar locations based on the comparison of
	 * their k-futures
	 *
	 * @return Some List<SRTALocation> if two similar locations were found, empty
	 *         otherwise
	 */
	private Optional<List<SRTALocation>> findSim() {
		var locs = automaton.getAllLocations();

		for (SRTALocation loc : locs) {
			var kFutures1 = getKFutures(loc);
			var possSimLoc = locs.stream().filter(state2 -> {
				if (loc.getAttrs().equals(state2.getAttrs()) && !loc.equals(state2)) {
					var kFutures2 = getKFutures(state2);
					if (!kFutures1.isEmpty() && !kFutures2.isEmpty()) {
						return compareKFutures(kFutures1, kFutures2);
					}
				}
				return false;
			}).findFirst();

			if (possSimLoc.isPresent()) {
				var simLoc = possSimLoc.get();
				return Optional.of(List.of(loc, simLoc)); // Immutable list
			}
		}
		return Optional.empty();
	}

	/**
	 * Method that performs a comparison between the k-futures of two states
	 *
	 * @param fs1
	 * @param fs2
	 * @return true if both k-futures are equivalent, false otherwise
	 */
	private boolean compareKFutures(ArrayList<ArrayList<Object>> fs1, ArrayList<ArrayList<Object>> fs2) {

		boolean weakTimeEq = false;
		boolean strongTimeInc = false;

		boolean weakTimeEqF1 = fs1.stream().allMatch(f1 -> {
			return fs2.stream().anyMatch(f2 -> compareFutures(f1, f2, "weak"));
		});

		boolean weakTimeEqF2 = fs2.stream().allMatch(f2 -> {
			return fs1.stream().anyMatch(f1 -> compareFutures(f1, f2, "weak"));
		});

		weakTimeEq = weakTimeEqF1 && weakTimeEqF2;

		if (!weakTimeEq) {
			boolean strongTimeIncF1 = fs1.stream().allMatch(f1 -> {
				return fs2.stream().anyMatch(f2 -> compareFutures(f1, f2, "weak"));
			});

			boolean strongTimeIncF2 = fs2.stream().allMatch(f2 -> {
				return fs1.stream().anyMatch(f1 -> compareFutures(f1, f2, "weak"));
			});

			strongTimeInc = strongTimeIncF1 || strongTimeIncF2;
		}

		boolean timeInclusion = weakTimeEq || strongTimeInc;

		return timeInclusion;
	}

	/**
	 * Method that performs a comparison between two future paths
	 *
	 * @param f1
	 * @param f2
	 * @param crit: weak or strong
	 * @return true if both futures are equivalent (same events, attributes and
	 *         length), false otherwise
	 */
	private boolean compareFutures(ArrayList<Object> f1, ArrayList<Object> f2, String crit) {
		var size1 = f1.size();
		var size2 = f2.size();
		if (size1 == size2) {
			Iterator<Object> it2 = f2.iterator();

			return f1.stream().allMatch(locOrEdge1 -> {
				var locOrEdge2 = it2.next();
				if (locOrEdge1 instanceof SRTALocation loc1 && locOrEdge2 instanceof SRTALocation loc2) { // States
					return loc1.getAttrs().equals(loc2.getAttrs());

				} else if (locOrEdge1 instanceof SRTAEdge edge1 && locOrEdge2 instanceof SRTAEdge edge2) { // Edges
					if (!edge1.getEvent().equals(edge2.getEvent()))
						return false;
					var minVal1 = edge1.getMin();
					var minVal2 = edge2.getMin();
					var maxVal1 = edge1.getMax();
					var maxVal2 = edge2.getMax();
					if (crit.equals("weak")) { // Weak time similarity
						return ((minVal1 >= minVal2 && minVal1 <= maxVal2)
								|| (maxVal1 >= minVal2 && maxVal1 <= maxVal2))
								|| ((minVal2 >= minVal1 && minVal2 <= maxVal1)
										|| (maxVal2 >= minVal1 && maxVal2 <= maxVal1));
					} else if (crit.equals("strong")) { // Strong time similarity
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
	 * Method used to merge two equivalent locations
	 *
	 * @param loc1
	 * @param loc2
	 * @return merged location
	 */
	private SRTALocation merge(SRTALocation loc1, SRTALocation loc2) {
		var mergedLoc = automaton.getLocation(Math.min(loc1.getId(), loc2.getId()));
		var auxLoc = automaton.getLocation(Math.max(loc1.getId(), loc2.getId()));

		auxLoc.getOutEdges().stream().map(idEdge -> automaton.getEdge(idEdge)).forEach(edge -> {
			edge.setSourceId(mergedLoc.getId());
			mergedLoc.addOutEdge(edge.getId());
		});

		auxLoc.getInEdges().stream().map(idEdge -> automaton.getEdge(idEdge)).forEach(edge -> {
			edge.setTargetId(mergedLoc.getId());
			mergedLoc.addInEdge(edge.getId());
		});

		automaton.deleteLocation(auxLoc.getId());
		mergeEdges(mergedLoc);
		return mergedLoc;
	}

	/**
	 * This method looks for duplicate in and out edges for the input location and
	 * merge them. Duplicate edges: Same source (id), target (id), event and
	 * overlapping guards
	 *
	 * @param location
	 */
	private void mergeEdges(SRTALocation loc) {

		// Checking outEdges, grouping them by the same event.
		var outEdges = loc.getOutEdges().stream().map(idEdge -> automaton.getEdge(idEdge))
				.collect(Collectors.groupingBy(SRTAEdge::getEvent)).values().stream().filter(v -> v.size() > 1)
				.toList();
		mergeEdgesAux(outEdges);

		// Checking inEdges, grouping them by the same event.
		var inEdges = loc.getInEdges().stream().map(idEdge -> automaton.getEdge(idEdge))
				.collect(Collectors.groupingBy(SRTAEdge::getEvent)).values().stream().filter(v -> v.size() > 1)
				.toList();
		mergeEdgesAux(inEdges);
	}

	private void mergeEdgesAux(List<List<SRTAEdge>> edgesToCheck) {
		for (List<SRTAEdge> possibleEqEdges : edgesToCheck) {
			while (possibleEqEdges.size() > 1) {
				ArrayList<SRTAEdge> compared = new ArrayList<>();
				SRTAEdge currentE = possibleEqEdges.get(0);
				List<SRTAEdge> eqEdges = possibleEqEdges.stream().filter(e -> {
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
				}).toList();

				compared.add(currentE); // The current edge is checked

				if (!eqEdges.isEmpty()) { // If there is equivalent edges
					compared.addAll(eqEdges); // Add the other equivalent edges checked
					var mergedEdge = compared.stream().min(Comparator.comparing(SRTAEdge::getId)).get(); // Only the
																											// edge with
																											// the
																											// lowest id
																											// (oldest)
																											// will
																											// remain
					var minGuard = compared.stream().min(Comparator.comparing(SRTAEdge::getMin)).get().getMin();
					var maxGuard = compared.stream().max(Comparator.comparing(SRTAEdge::getMax)).get().getMax();
					mergedEdge.setMin(minGuard);
					mergedEdge.setMax(maxGuard);
					compared.removeIf(e -> e.getId() == mergedEdge.getId()); // The edge is fused so another check is
																				// required
					compared.stream().forEach(edge -> {
						mergedEdge.addSamples(edge.getSamples()); // Add to the merged edge all the time samples of
																	// the eq edges that are going to be removed
					});

					compared.stream().map(SRTAEdge::getId).forEach(e -> automaton.deleteEdge(e));
				}
				possibleEqEdges.removeAll(compared);
			}

		}
	}

	/**
	 * Method used to search inconsistencies in the out edges of every state.
	 * Inconsistencies checked: If there are two or more edges with the same event,
	 * overlapping guard and they go to different states.
	 *
	 * @return Indeterministic edges or empty otherwise
	 */
	private Optional<List<SRTAEdge>> indetEdges() {
		var locs = automaton.getAllLocations();

		for (SRTALocation loc : locs) {
			for (var idEdge : loc.getOutEdges()) {
				var edge = automaton.getEdge(idEdge);
				var event = edge.getEvent();
				var dupEdges = loc.getOutEdges().stream().map(automaton::getEdge).filter(e -> { // Check for
																								// inconsistencies
					if (e.getId() == edge.getId())
						return false;
					if (e.getEvent().equals(event)) {

						// // Same target location
						var minVal1 = edge.getMin();
						var minVal2 = e.getMin();
						var maxVal1 = edge.getMax();
						var maxVal2 = e.getMax();
						boolean overlappingGuards = ((minVal1 >= minVal2 && minVal1 <= maxVal2)
								|| (maxVal1 >= minVal2 && maxVal1 <= maxVal2))
								|| ((minVal2 >= minVal1 && minVal2 <= maxVal1) // Overlapping guards
										|| (maxVal2 >= minVal1 && maxVal2 <= maxVal1));
						return overlappingGuards; // If two edges have overlapping guards and the same event
					}
					return false;
				}).collect(Collectors.toCollection(ArrayList::new));
				if (!dupEdges.isEmpty()) {
					dupEdges.add(edge);
					return Optional.of(dupEdges);
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Method used to merge the target locations of the indeterministic edges if
	 * possible all edges have the same source location. If simStates if 0 means
	 * that it is impossible to fix because the target locations have different
	 * system attributes
	 *
	 * @param indetEdges
	 * @return boolean
	 */
	private boolean fixIndet(List<SRTAEdge> indetEdges) {
		var attrs = automaton.getLocation(indetEdges.get(0).getTargetId()).getAttrs();
		var simLocs = indetEdges.stream().map(e -> automaton.getLocation(e.getTargetId()))
				.filter(s -> s.getAttrs().equals(attrs)).distinct().toList();
		if (simLocs.size() > 1) {
			var newLoc = simLocs.get(0);
			for (int i = 1; i < simLocs.size(); i++) {
				newLoc = merge(newLoc, simLocs.get(i));
				mergeEdges(newLoc);
			}
			return true;
		}
		return false;
	}

	/**
	 * This method tries to merge leaf states
	 */
	private boolean mergeFinalLocations() {
		var locations = automaton.getAllLocations();
		var finalLocs = locations.stream().filter(loc -> loc.getOutEdges().size() == 0).toList();

		for (SRTALocation leaf : finalLocs) {
			var possEqLeaf = finalLocs.stream().filter(leaf2 -> {
				if (!leaf.equals(leaf2) && leaf.getAttrs().equals(leaf2.getAttrs())) {
					var edgesInLeaf1 = leaf.getInEdges().stream().map(idEdge -> automaton.getEdge(idEdge)).toList();
					var edgesInLeaf2 = leaf2.getInEdges().stream().map(idEdge -> automaton.getEdge(idEdge)).toList();

					if (!edgesInLeaf1.isEmpty() && !edgesInLeaf2.isEmpty()) {
						if (edgesInLeaf1.size() > edgesInLeaf2.size()) {
							return edgesInLeaf2.stream().allMatch(edge2 -> {
								return edgesInLeaf1.stream().anyMatch(edge -> edge.getEvent().equals(edge2.getEvent()));
							});
						} else {
							return edgesInLeaf1.stream().allMatch(edge -> {
								return edgesInLeaf2.stream()
										.anyMatch(edge2 -> edge2.getEvent().equals(edge.getEvent()));
							});
						}
					}
				}
				return false;
			}).findFirst();

			if (possEqLeaf.isPresent()) {
				var equivalentLeaf = possEqLeaf.get();
				var mergedLeaf = merge(leaf, equivalentLeaf);
				mergeEdges(mergedLeaf);
				return true;
			}
		}
		return false;
	}

	private void phase3() {
		computeProbs();
		computeInvariants();

	}

	private void computeInvariants() {
		automaton.getAllLocations().stream().forEach(loc -> {
			if (!loc.getOutEdges().isEmpty()) {
				SRTAEdge edge = loc.getOutEdges().stream().map(idEdge -> automaton.getEdge(idEdge))
						.max(Comparator.comparing(SRTAEdge::getMax)).get();
				Double invariant = edge.getMax();
				loc.setInvariant(invariant);
			}
		});
	}

	private void computeProbs() {

		for (var loc : automaton.getAllLocations()) {
			var accumSamples = loc.getOutEdges().stream().mapToDouble(e -> automaton.getEdge(e).getSamples().size())
					.sum(); // Already casted to double
			loc.getOutEdges().stream().map(e -> automaton.getEdge(e))
					.forEach(e -> e.setProb(((double) e.getSamples().size()) / accumSamples));
		}
	}
}
