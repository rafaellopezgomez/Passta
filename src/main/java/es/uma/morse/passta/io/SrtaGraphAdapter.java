package es.uma.morse.passta.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.geometry.Point2D;

import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.core.automaton.SRTAEdge;
import es.uma.morse.passta.core.automaton.SRTALocation;

public final class SrtaGraphAdapter {

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

	private static List<JavaFxGraphViewer.StateModel> convertLocations(
		    SRTA automaton
		) {
		    Map<Integer, Point2D> positions =
		        SrtaGraphLayout.calculatePositions(automaton);

		    List<JavaFxGraphViewer.StateModel> states =
		        new ArrayList<>();

		    for (SRTALocation location : automaton.getAllLocations()) {
		        Point2D position = positions.get(location.getId());

		        if (position == null) {
		            throw new IllegalStateException(
		                "No position calculated for location "
		                    + location.getId()
		            );
		        }

		        states.add(
		            createStateModel(
		                location,
		                position.getX(),
		                position.getY()
		            )
		        );
		    }

		    return states;
		}

	private static JavaFxGraphViewer.StateModel createStateModel(SRTALocation location, double x, double y) {

		String label = buildLocationLabel(location);
		boolean initial = location.getId() == 0;

		return new JavaFxGraphViewer.StateModel(Integer.toString(location.getId()), label, x, y, initial);
	}

	private static String buildLocationLabel(SRTALocation location) {
	    StringBuilder label = new StringBuilder();

	    label.append("L").append(location.getId());

	    if (location.getAttrs() != null
	            && !location.getAttrs().isEmpty()) {
	        label.append("\n");
	        label.append(String.join(", ", location.getAttrs()));
	    }

	    Double invariant = location.getInvariant();

	    if (invariant != null && invariant >= 0.0) {
	        label.append("\n");
	        label.append("x <= ");
	        label.append(formatNumber(invariant));
	    }

	    return label.toString();
	}

	private static List<JavaFxGraphViewer.EdgeModel> convertEdges(SRTA automaton) {
		List<JavaFxGraphViewer.EdgeModel> edges = new ArrayList<>();

		for (SRTAEdge edge : automaton.getAllEdges()) {
			edges.add(createEdgeModel(edge));
		}

		return edges;
	}

	private static JavaFxGraphViewer.EdgeModel createEdgeModel(SRTAEdge edge) {
		return new JavaFxGraphViewer.EdgeModel(Integer.toString(edge.getId()), Integer.toString(edge.getSourceId()),
				Integer.toString(edge.getTargetId()), edge.getEvent() + "\n" + edge.getGuard().toString(),
				buildEdgeDescription(edge));
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

		return String.format(java.util.Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
	}
}