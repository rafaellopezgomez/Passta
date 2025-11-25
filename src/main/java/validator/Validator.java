package validator;

import automaton.EDRTA;
import edge.EDRTAEdge;
import learning_algorithm.Passta;
import state.EDRTAState;
import trace.Observation;
import trace.Trace;

import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Validator {
	public static int nValidTraces(String src, EDRTA a) {
		File resourcesFile = new File(src);
		var traces = Passta.readTraces(resourcesFile.getAbsoluteFile());

		return nValidTraces(traces, a);
	}

	public static int nValidTraces(ArrayList<Trace> traces, EDRTA a) {
		var compTraces = Passta.compressTraces(traces);
		int nValid = 0;
		for (Trace t : compTraces) {
			if (checkTrace(t, a)) {
				nValid += 1;
			}
		}
		return nValid;
	}

	public static boolean checkTrace(Trace t, EDRTA automaton) {
		EDRTAState lastState = null;
		double lastTimeStamp = (float) 0;
		for (Observation obs : t.getObs()) {
			String event = obs.event().isEmpty() ? "□" : obs.event();
			ArrayList<String> variables = obs.variables();
			double timeDelta = obs.time() - lastTimeStamp;
			
			// First state
			if (lastState == null) {
				lastState = automaton.getState(0);
			}

			// Theoretically there is only one possible edge, if the automaton follows the
			// rules
			var pEdge = lastState.getOutEdges().stream().map(automaton::getEdge).filter(e -> {
				return e.getEvent().equals(event) && (timeDelta >= e.getMin() && timeDelta <= e.getMax()
						&& automaton.getState(e.getTargetId()).getAttrs().equals(obs.variables()));
			}).findFirst();

			if (pEdge.isEmpty()) {
				return false;
			}
			var edge = pEdge.get();
			var targetState = automaton.getState(edge.getTargetId());

			lastTimeStamp = obs.time();
			lastState = targetState;
		}
		return true;
	}
}
