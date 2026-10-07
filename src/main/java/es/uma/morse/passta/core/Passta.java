package es.uma.morse.passta.core;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.core.automaton.SRTAEdge;
import es.uma.morse.passta.core.automaton.SRTALocation;
import es.uma.morse.passta.core.trace.Observation;
import es.uma.morse.passta.core.trace.Trace;
import es.uma.morse.passta.core.matching.SimilarityType;
import es.uma.morse.passta.core.matching.SimilarityMatcher;
import es.uma.morse.passta.io.AutomatonViewer;
import es.uma.morse.passta.io.TraceReader;

public class Passta {

	private SRTA automaton;
	private final int k;

	/**
	 * Flag used to indicate that the variables of the initial location are correct.
	 */
	boolean initVars;

	private final Path tracesPath;

	/**
	 * Creates a PASSTA learning algorithm instance from a source file path.
	 *
	 * @param src tracesPath path to the file or directory where the JSON files are.
	 * @param k   algorithm parameter
	 */
	public Passta(String src, int k) {
		this(Path.of(Objects.requireNonNull(src, "Source path is null")), k);
	}

	/**
	 * Creates a PASSTA learning algorithm instance from a source file path.
	 * 
	 * @param tracesPath path to the file or directory where the JSON files are.
	 * @param k          algorithm parameter
	 */
	public Passta(Path tracesPath, int k) {

		if (k < 1) {
			throw new IllegalArgumentException("k must be greater than zero");
		}

		this.k = k;
		this.tracesPath = Objects.requireNonNull(tracesPath, "tracesPath is null");

		learn();
	}

	/**
	 * Learns from a JSON file containing traces.
	 */
	private void learn() {
		initVars = false;

		phase1();
		phase2();
		phase3();
	}

	public SRTA getAutomaton() {
		return automaton;
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
	public static List<Trace> compressTraces(List<Trace> traces) {
		Objects.requireNonNull(traces, "traces is null");

		for (int idx = 0; idx < traces.size(); idx++) {
			Trace t = traces.get(idx);
			if (t == null)
				continue;
			compressTrace(t);
		}
		return traces;
	}

	/**
	 * Method to perform a compressing operation in the input trace. The compression
	 * operations are: 1. If consecutive observations have not an event and all have
	 * the same variable parameters, then they are fused as one that compress all
	 * the information. 2. If there is an event observation and the observations
	 * that come consecutively later haven´t an event and they have the same
	 * variables as the one with the event then the same fusion operation is
	 * performed.
	 *
	 * @return trace
	 */
	public static Trace compressTrace(Trace trace) {
		if (trace == null)
			return null;

		List<Observation> obs = trace.getObs();
		if (obs == null || obs.isEmpty())
			return trace;

		ArrayList<Observation> compressed = new ArrayList<>(obs.size());

		for (int i = 0; i < obs.size();) {
			Observation current = obs.get(i);

			int j = i + 1;

			while (j < obs.size() && checkEqOb(current, obs.get(j))) {
				j++;
			}

			compressed.add(current);

			i = j;
		}

		trace.setObs(compressed);
		return trace;
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

		processTraces();
	}

	private void processTraces() {

		try (Stream<Trace> traces = TraceReader.readTraces(tracesPath)) {

			Iterator<Trace> iterator = traces.iterator();

			while (iterator.hasNext()) {
				Trace trace = iterator.next();

				if (!trace.isEmpty()) {
					trace = compressTrace(trace);
					processTrace(trace);
				}
			}

		} catch (UncheckedIOException e) {
			throw new RuntimeException("Cannot process traces from: " + tracesPath, e);
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
				var edge = automaton.addEdge(qo, qt, 0.0, 0.0, "□");
				edge.addSample(0.0);
			} else { // If there is an event in the first observation
				qt = automaton.addLocation(ob.variables());
				var e = automaton.addEdge(qo, qt, delta, delta, ob.event());
				e.addSample(delta);
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
		List<SRTALocation> pEqLocs = automaton.getAllLocations().stream().filter(state -> {
			var attrs = state.getAttrs();
			return obsWindow.get(0).variables().equals(attrs);
		}).toList();

		if (pEqLocs.isEmpty())
			return null;

		// Build the k future of the current observation given an observation window of
		// length k
		List<Object> obFut = new ArrayList<>(); // Future of the observation
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

			// Search for a future that is stucturally similar to the observation future
			Optional<List<Object>> sameFuture = futuresOfCandidate.stream().filter(future -> {
				return SimilarityMatcher.compareFutures(obFut, future, SimilarityType.STRUCTURAL);
			}).findFirst();

			if (sameFuture.isPresent()) { // If there is an equivalent future

				String currentEvent = obsWindow.get(0).event().isEmpty() ? "□" : obsWindow.get(0).event();

				var e = automaton.addEdge(qo, currentEqLoc, delta, delta, currentEvent);

				e.addSample(delta);

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
	private List<List<Object>> getKFutures(SRTALocation qo) {
		List<List<Object>> futures = new ArrayList<>();

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
	private List<List<Object>> getKFuturesAux(List<Object> currentPath, int level) {
		List<List<Object>> futures = new ArrayList<>();
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

	private void phase2() {

		automaton.getAllLocations().forEach(this::mergeEdges);

		boolean merged;
		boolean indet;
		boolean fixed;

		do {
			var simLocs = findSim();

			simLocs.ifPresent(locations -> {
				SRTALocation first = locations.get(0);
				SRTALocation second = locations.get(1);
				merge(first, second);
			});

			merged = simLocs.isPresent();

			do {
				var indetEdges = indetEdges();
				indet = indetEdges.isPresent();

				if (indet) {
					fixed = fixIndet(indetEdges.get());

					if (fixed) {
						merged = true;
					} else {
						AutomatonViewer.show(automaton);
						throw new RuntimeException("Cannot repair nondeterministic edges: " + indetEdges.get());
					}
				}
			} while (indet);

		} while (merged);

		while (mergeFinalLocations()) {
			Optional<List<SRTAEdge>> conflict;

			while ((conflict = indetEdges()).isPresent()) {
				if (!fixIndet(conflict.get())) {
					AutomatonViewer.show(automaton);
					throw new RuntimeException(
							"Cannot repair nondeterministic edges after leaf merging: " + conflict.get());
				}
			}
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
						return SimilarityMatcher.compareKFutures(kFutures1, kFutures2);
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
	 * Method used to merge two similar locations
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

				List<SRTAEdge> compared = new ArrayList<>();
				SRTAEdge currentEdge = possibleEqEdges.get(0);
				List<SRTAEdge> eqEdges = possibleEqEdges.stream().filter(otherEdge -> {

					if (otherEdge.getId() == currentEdge.getId()) {
						return false;
					}
						
					boolean sameSource = currentEdge.getSourceId() == otherEdge.getSourceId(); // Same source location (same id)
					boolean sameTarget = currentEdge.getTargetId() == otherEdge.getTargetId(); // Same target state (same id)
					boolean overlappingGuards = SimilarityMatcher.intervalsOverlap(currentEdge.getMin(), currentEdge.getMax(),
							otherEdge.getMin(), otherEdge.getMax());
					return sameSource && sameTarget && overlappingGuards;
				}).toList();

				compared.add(currentEdge); // The current edge is checked

				if (!eqEdges.isEmpty()) { // If there is equivalent edges
					compared.addAll(eqEdges); // Add the other equivalent edges checked
					SRTAEdge mergedEdge = compared.stream().min(Comparator.comparing(SRTAEdge::getId)).orElseThrow(); // Only the
																											// edge with
																											// the
																											// lowest id
																											// (oldest)
																											// will
																											// remain
					double minGuard = compared.stream().mapToDouble(SRTAEdge::getMin).min().orElseThrow();
					double maxGuard = compared.stream().mapToDouble(SRTAEdge::getMax).max().orElseThrow();
					mergedEdge.setMin(minGuard);
					mergedEdge.setMax(maxGuard);
					compared.removeIf(edge -> edge.getId() == mergedEdge.getId()); // The edge is fused so another check is
																				// required
					compared.forEach(edge -> {
						mergedEdge.addSamples(edge.getSamples()); // Add to the merged edge all the time samples of
																	// the duplicated edges that are going to be removed
					});

					compared.stream().map(SRTAEdge::getId).forEach(automaton::deleteEdge);
				}
				possibleEqEdges.removeAll(compared);
			}
		}
	}

	/**
	 * Searches for nondeterministic outgoing edges.
	 *
	 * Two distinct edges are nondeterministic if:
	 *
	 * 1. They leave the same location. 2. They have the same event. 3. Their guards
	 * overlap.
	 *
	 * The target locations and their attributes are irrelevant.
	 *
	 * @return a list containing conflicting edges, or an empty Optional if the
	 *         automaton is deterministic
	 */
	private Optional<List<SRTAEdge>> indetEdges() {

		for (SRTALocation location : automaton.getAllLocations()) {

			List<SRTAEdge> outgoingEdges = location.getOutEdges().stream().map(automaton::getEdge).toList();

			for (int i = 0; i < outgoingEdges.size(); i++) {
				SRTAEdge first = outgoingEdges.get(i);

				List<SRTAEdge> conflictingEdges = new ArrayList<>();

				for (int j = i + 1; j < outgoingEdges.size(); j++) {
					SRTAEdge second = outgoingEdges.get(j);

					boolean sameEvent = first.getEvent().equals(second.getEvent());

					boolean overlappingGuards = SimilarityMatcher.intervalsOverlap(first.getMin(), first.getMax(),
							second.getMin(), second.getMax());

					if (sameEvent && overlappingGuards) {
						conflictingEdges.add(second);
					}
				}

				if (!conflictingEdges.isEmpty()) {
					conflictingEdges.add(0, first);
					return Optional.of(conflictingEdges);
				}
			}
		}

		return Optional.empty();
	}

	private boolean fixIndet(List<SRTAEdge> indetEdges) {

		if (indetEdges == null || indetEdges.size() < 2) {
			return false;
		}

		List<SRTALocation> targetLocations = indetEdges.stream().map(edge -> automaton.getLocation(edge.getTargetId()))
				.distinct().toList();

		var expectedAttrs = targetLocations.get(0).getAttrs();

		boolean sameAttributes = targetLocations.stream()
				.allMatch(location -> location.getAttrs().equals(expectedAttrs));

		if (!sameAttributes) {
			return false;
		}

		/*
		 * If every conflicting edge already has the same target, there are no locations
		 * to merge. The edges themselves must be merged.
		 */
		if (targetLocations.size() == 1) {
			SRTALocation source = automaton.getLocation(indetEdges.get(0).getSourceId());

			mergeEdges(source);
			return true;
		}

		SRTALocation mergedLocation = targetLocations.get(0);

		for (int i = 1; i < targetLocations.size(); i++) {
			mergedLocation = merge(mergedLocation, targetLocations.get(i));
		}

		return true;
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
		computeProbabilities();
		computeInvariants();

	}

	private void computeInvariants() {
		for (var location : automaton.getAllLocations()) {
			location.getOutEdges().stream().map(automaton::getEdge).mapToDouble(SRTAEdge::getMax).max().ifPresent(location::setInvariant);
		}
	}

	private void computeProbabilities() {

		for (SRTALocation location : automaton.getAllLocations()) {

			List<SRTAEdge> outgoingEdges = location.getOutEdges().stream().map(automaton::getEdge).toList();

			int totalSamples = outgoingEdges.stream().mapToInt(edge -> edge.getSamples().size()).sum();

			if (totalSamples == 0) {
				continue;
			}

			outgoingEdges.forEach(edge -> edge.setProb((double) edge.getSamples().size() / totalSamples));
		}
	}
}
