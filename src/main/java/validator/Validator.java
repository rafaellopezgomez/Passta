package validator;

import automaton.SRTA;
import learning_algorithm.Passta;
import location.SRTALocation;
import trace.Observation;
import trace.Trace;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;

public class Validator {
	public static int nValidTraces(ArrayList<Trace> traces, SRTA a, String dst) {
		if(traces == null) throw new RuntimeException("Null traces");
		var compTraces = Passta.compressTraces(traces);
		int nAccepted = 0;
		ArrayList<Trace> rejectedT = new ArrayList<>(); //
		for (Trace t : compTraces) {
			if (checkTrace(t, a)) {
				nAccepted += 1;
			} else {
				rejectedT.add(t);
			}
		}
		if(dst != null && !dst.isBlank()) {
			writeTraces(dst, rejectedT);
		}
		
		return nAccepted;
	}
	
	public static int nValidTraces(ArrayList<Trace> traces, SRTA a) {
		return nValidTraces(traces, a, "");
	}
	
	public static int nValidTraces(String src, SRTA a, String dst) {
		if(src == null || src.isBlank()) throw new RuntimeException("Source path is null or undefined");
		File resourcesFile = new File(src);
		var traces = Passta.readTraces(resourcesFile.getAbsoluteFile());

		return nValidTraces(traces, a, dst);
	}
	
	
	public static int nValidTraces(String src, SRTA a) {
		if(src == null || src.isBlank()) throw new RuntimeException("Source path is null or undefined");
		File resourcesFile = new File(src);
		var traces = Passta.readTraces(resourcesFile.getAbsoluteFile());

		return nValidTraces(traces, a, "");
	}

	public static boolean checkTrace(Trace t, SRTA automaton) {
		SRTALocation lastState = null;
		double lastTimeStamp = (float) 0;
		for (Observation obs : t.getObs()) {
			String event = obs.event().isEmpty() ? "□" : obs.event();
			double timeDelta = obs.time() - lastTimeStamp;
			ArrayList<String> variables = obs.variables();
			
			// First state
			if (lastState == null) {
				lastState = automaton.getLocation(0);
			}

			// Theoretically there is only one possible edge, if the automaton follows the
			// rules
			var pEdge = lastState.getOutEdges().stream().map(automaton::getEdge).filter(e -> {
				return e.getEvent().equals(event) && (timeDelta >= e.getMin() && timeDelta <= e.getMax()
						&& automaton.getLocation(e.getTargetId()).getAttrs().equals(variables));
			}).findFirst();

			if (pEdge.isEmpty()) {
				var hasEvent = lastState.getOutEdges().stream().map(automaton::getEdge).filter(e -> {
					return e.getEvent().equals(event);
				}).collect(Collectors.toList());
				if(hasEvent.size() > 0) { 
					var systemAttrs = hasEvent.stream().map(e -> automaton.getLocation(e.getTargetId()).getAttrs()).collect(Collectors.toList());
					var guards = hasEvent.stream().map(e -> e.getGuard()).collect(Collectors.toList());
					
					if (!systemAttrs.stream().anyMatch(s -> s.equals(variables))) { // There is not target state with the same system attributes
						var systemAttrsString = systemAttrs.stream().map(sa -> sa.toString()).collect(Collectors.joining(", "));
						obs.variables().add("Error: the automaton recognizes the event but not the system attributes. Available system attributes of target states from state " + lastState.toString() + ": " + systemAttrsString);
					}
					
					if (!guards.stream().anyMatch(g -> (timeDelta >= g.getFirst() && timeDelta <= g.getLast()))) { // Time is the problem
						String guardsString = guards.stream().map(g -> g.toString()).collect(Collectors.joining(", "));
						obs.variables().add("Error: the automaton recognizes the event but not the time delta " + timeDelta + ". Guards of outgoing edges from state " + lastState.toString() + ", that have the same event: " + guardsString);
					}
					
				} else { // Event is the problem
					String events = lastState.getOutEdges().stream().map(automaton::getEdge).map(e -> e.getEvent()).collect(Collectors.joining(", "));
					obs.variables().add("Error: the automaton does not recognize the event. Events available from state " + lastState.toString() + ": " + events);
				}
				return false;
			}
			var edge = pEdge.get();
			var targetState = automaton.getLocation(edge.getTargetId());

			lastTimeStamp = obs.time();
			lastState = targetState;
		}
		return true;
	}
	
	private static void writeTraces(String dst, List<Trace> traces) {
		if(dst == null | dst.isBlank()) throw new RuntimeException("Destination path is null or undefined");
		Path path = Paths.get(dst.toString());
		try {
			if (Files.notExists(path)) {
				Files.createDirectories(path);
			}
			String filename = Files.isRegularFile(path) ? path.toString() : Paths.get(path.toString(), "rejected.json").toString();
			System.out.println(filename);
			if(!filename.endsWith(".json")) throw new RuntimeException("Destination filename must have .json extension");
			ObjectMapper mapper = JsonMapper.builder().addModule(new BlackbirdModule()).build()
					.enable(SerializationFeature.INDENT_OUTPUT)
					.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
			mapper.writeValue(new File(filename), traces);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
}
