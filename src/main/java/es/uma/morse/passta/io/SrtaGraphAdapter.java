package es.uma.morse.passta.io;

import java.util.ArrayList;
import java.util.List;

import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.core.automaton.SRTAEdge;
import es.uma.morse.passta.core.automaton.SRTALocation;

public final class SrtaGraphAdapter {

	private static final double CENTER_X = 600.0;
	private static final double CENTER_Y = 400.0;
	private static final double LAYOUT_RADIUS = 280.0;

	private SrtaGraphAdapter() {
		// Utility class
	}

	public static JavaFxGraphViewer.GraphModel convert(SRTA automaton) {
		if (automaton == null) {
			throw new IllegalArgumentException("The automaton cannot be null");
		}

		List<JavaFxGraphViewer.StateModel> states = convertLocations(automaton);
		List<JavaFxGraphViewer.EdgeModel> edges = convertEdges(automaton);

		return new JavaFxGraphViewer.GraphModel(states, edges);
	}

	private static List<JavaFxGraphViewer.StateModel> convertLocations(SRTA automaton) {
		List<SRTALocation> locations = new ArrayList<>(automaton.getAllLocations());

		List<JavaFxGraphViewer.StateModel> states = new ArrayList<>();

		if (locations.isEmpty()) {
			return states;
		}

		if (locations.size() == 1) {
			SRTALocation location = locations.getFirst();

			states.add(createStateModel(location, CENTER_X, CENTER_Y));

			return states;
		}

		double angleStep = 2.0 * Math.PI / locations.size();

		for (int index = 0; index < locations.size(); index++) {
			SRTALocation location = locations.get(index);

			/*
			 * Empezamos en la parte superior del círculo.
			 */
			double angle = index * angleStep - Math.PI / 2.0;

			double x = CENTER_X + LAYOUT_RADIUS * Math.cos(angle);
			double y = CENTER_Y + LAYOUT_RADIUS * Math.sin(angle);

			states.add(createStateModel(location, x, y));
		}

		return states;
	}

	private static JavaFxGraphViewer.StateModel createStateModel(SRTALocation location, double x, double y) {
		String label = buildLocationLabel(location);

		return new JavaFxGraphViewer.StateModel(Integer.toString(location.getId()), label, x, y);
	}

	private static String buildLocationLabel(SRTALocation location) {
		StringBuilder label = new StringBuilder();

		label.append("L").append(location.getId());

		if (location.getAttrs() != null && !location.getAttrs().isEmpty()) {

			label.append("\n");
			label.append(String.join(", ", location.getAttrs()));
		}

		return label.toString();
	}
	
	private static List<JavaFxGraphViewer.EdgeModel> convertEdges(
		    SRTA automaton
		) {
		    List<JavaFxGraphViewer.EdgeModel> edges = new ArrayList<>();

		    for (SRTAEdge edge : automaton.getAllEdges()) {
		        edges.add(createEdgeModel(edge));
		    }

		    return edges;
		}

		private static JavaFxGraphViewer.EdgeModel createEdgeModel(
		    SRTAEdge edge
		) {
		    return new JavaFxGraphViewer.EdgeModel(
		        Integer.toString(edge.getId()),
		        Integer.toString(edge.getSourceId()),
		        Integer.toString(edge.getTargetId()),
		        buildEdgeDescription(edge)
		    );
		}

		private static String buildEdgeDescription(SRTAEdge edge) {
		    StringBuilder description = new StringBuilder();

		    description.append(edge.getEvent());
		    description.append("\n");
		    description.append("Interval: [");
		    description.append(formatNumber(edge.getMin()));
		    description.append(", ");
		    description.append(formatNumber(edge.getMax()));
		    description.append("]");

		    if (edge.getProb() != null) {
		        description.append("\n");
		        description.append("Probability: ");
		        description.append(formatNumber(edge.getProb()));
		    }

		    if (edge.getSamples() != null && !edge.getSamples().isEmpty()) {
		        description.append("\n");
		        description.append("Samples: ");
		        description.append(edge.getSamples().size());
		    }

		    return description.toString();
		}

		private static String formatNumber(double value) {
		    if (value == Math.rint(value)) {
		        return Long.toString((long) value);
		    }

		    return String.format(
		        java.util.Locale.ROOT,
		        "%.4f",
		        value
		    ).replaceAll("0+$", "")
		     .replaceAll("\\.$", "");
		}
}