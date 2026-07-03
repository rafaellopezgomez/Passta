package es.uma.morse.passta.validation;


import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.core.automaton.SRTALocation;
import es.uma.morse.passta.core.trace.Observation;
import es.uma.morse.passta.core.trace.Trace;
import es.uma.morse.passta.io.TraceReader;
import es.uma.morse.passta.io.TraceWriter;


public final class Validator {

    private Validator() {
    }

    public static int nValidTraces(List<Trace> traces, SRTA automaton) {
        return nValidTraces(traces, automaton, (Path) null);
    }

    public static int nValidTraces(List<Trace> traces, SRTA automaton, String destination) {
        Path outputPath = null;

        if (destination != null && !destination.isBlank()) {
            outputPath = Path.of(destination);
        }

        return nValidTraces(traces, automaton, outputPath);
    }

    public static int nValidTraces(List<Trace> traces, SRTA automaton, Path destination) {
        Objects.requireNonNull(traces, "Traces list is null");
        Objects.requireNonNull(automaton, "Automaton is null");

        if (traces.isEmpty()) {
            throw new IllegalArgumentException("Traces list is empty");
        }

        List<Trace> compressedTraces = Passta.compressTraces(traces);

        int acceptedCount = 0;
        List<Trace> rejectedTraces = new ArrayList<>();

        for (Trace trace : compressedTraces) {
            if (checkTrace(trace, automaton)) {
                acceptedCount++;
            } else {
                rejectedTraces.add(trace);
            }
        }

        if (destination != null && !rejectedTraces.isEmpty()) {
            TraceWriter.writeTraces(destination, rejectedTraces);
        }

        return acceptedCount;
    }

    public static int nValidTraces(String source, SRTA automaton) {
        return nValidTraces(source, automaton, (Path) null);
    }

    public static int nValidTraces(String source, SRTA automaton, String destination) {
        Path outputPath = null;

        if (destination != null && !destination.isBlank()) {
            outputPath = Path.of(destination);
        }

        return nValidTraces(source, automaton, outputPath);
    }

    public static int nValidTraces(String source, SRTA automaton, Path destination) {
        Objects.requireNonNull(source, "Source path is null");

        if (source.isBlank()) {
            throw new IllegalArgumentException("Source path is blank");
        }

        return nValidTraces(Path.of(source), automaton, destination);
    }

    public static int nValidTraces(Path source, SRTA automaton) {
        return nValidTraces(source, automaton, (Path) null);
    }

    public static int nValidTraces(Path source, SRTA automaton, Path destination) {
        Objects.requireNonNull(source, "Source path is null");
        Objects.requireNonNull(automaton, "Automaton is null");

        List<Trace> traces = TraceReader.readTraces(source);

        return nValidTraces(traces, automaton, destination);
    }

	public static boolean checkTrace(Trace t, SRTA automaton) {
		SRTALocation lastLocation = null;
		double lastTimeStamp = (float) 0;
		for (Observation obs : t.getObs()) {
			String event = obs.event().isEmpty() ? "□" : obs.event();
			double timeDelta = obs.time() - lastTimeStamp;
			ArrayList<String> variables = obs.variables();
			
			// First state
			if (lastLocation == null) {
				lastLocation = automaton.getLocation(0);
			}

			// Theoretically there is only one possible edge, if the automaton follows the
			// rules
			var pEdge = lastLocation.getOutEdges().stream().map(automaton::getEdge).filter(e -> {
				return e.getEvent().equals(event) && (timeDelta >= e.getMin() && timeDelta <= e.getMax()
						&& automaton.getLocation(e.getTargetId()).getAttrs().equals(variables));
			}).findFirst();

			if (pEdge.isEmpty()) {
				var hasEvent = lastLocation.getOutEdges().stream().map(automaton::getEdge).filter(e -> {
					return e.getEvent().equals(event);
				}).collect(Collectors.toList());
				if(hasEvent.size() > 0) { 
					var systemAttrs = hasEvent.stream().map(e -> automaton.getLocation(e.getTargetId()).getAttrs()).collect(Collectors.toList());
					var guards = hasEvent.stream().map(e -> e.getGuard()).collect(Collectors.toList());
					
					if (!systemAttrs.stream().anyMatch(s -> s.equals(variables))) { // There is not target state with the same system attributes
						var systemAttrsString = systemAttrs.stream().map(sa -> sa.toString()).collect(Collectors.joining(", "));
						obs.variables().add("Error: the automaton recognizes the event but not the system attributes. Available system attributes of target states from state " + lastLocation.toString() + ": " + systemAttrsString);
					}
					
					if (!guards.stream().anyMatch(g -> (timeDelta >= g.getFirst() && timeDelta <= g.getLast()))) { // Time is the problem
						String guardsString = guards.stream().map(g -> g.toString()).collect(Collectors.joining(", "));
						obs.variables().add("Error: the automaton recognizes the event but not the time delta " + timeDelta + ". Guards of outgoing edges from state " + lastLocation.toString() + ", that have the same event: " + guardsString);
					}
					
				} else { // Event is the problem
					String events = lastLocation.getOutEdges().stream().map(automaton::getEdge).map(e -> e.getEvent()).collect(Collectors.joining(", "));
					obs.variables().add("Error: the automaton does not recognize the event. Events available from state " + lastLocation.toString() + ": " + events);
				}
				return false;
			}
			var edge = pEdge.get();
			var targetLocation = automaton.getLocation(edge.getTargetId());

			lastTimeStamp = obs.time();
			lastLocation = targetLocation;
		}
		return true;
	}
}
