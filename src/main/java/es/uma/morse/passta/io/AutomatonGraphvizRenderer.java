package es.uma.morse.passta.io;


import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.graphper.api.Graphviz;
import org.graphper.api.Graphviz.GraphvizBuilder;

import es.uma.morse.passta.core.automaton.SRTA;

import org.graphper.api.Line;
import org.graphper.api.Node;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class AutomatonGraphvizRenderer {

	private static final MathContext CONTEXT = new MathContext(6, RoundingMode.HALF_UP);
	private static final double ZERO_EPSILON = 1e-12;

	private AutomatonGraphvizRenderer() {
	}

	public static Graphviz toGraphviz(SRTA automaton) {
		Objects.requireNonNull(automaton, "Automaton is null");

		GraphvizBuilder graph = Graphviz.digraph();

		Map<String, Node> nodes = new TreeMap<>();

		automaton.getAllLocations().forEach(location -> {
			String id = String.valueOf(location.getId());
			String attrs = String.valueOf(location.getAttrs());
			String invariant = String.valueOf(format(location.getInvariant()));

			Node node = Node.builder().label(id + " " + attrs + "\n" + "<= " + invariant).build();

			nodes.put(id, node);
			graph.addNode(node);
		});

		automaton.getAllEdges().forEach(edge -> {
			String sourceId = String.valueOf(edge.getSourceId());
			String targetId = String.valueOf(edge.getTargetId());

			Node source = nodes.get(sourceId);
			Node target = nodes.get(targetId);

			if (source == null) {
				throw new IllegalStateException("Unknown source location: " + sourceId);
			}

			if (target == null) {
				throw new IllegalStateException("Unknown target location: " + targetId);
			}

			Line line = Line.builder(source, target)
					.label(buildEdgeLabel(edge.getEvent(), edge.getGuard(), edge.getProb())).build();

			graph.addLine(line);
		});

		return graph.build();
	}

	private static String buildEdgeLabel(String event, List<Double> guard, Double probability) {
		StringBuilder label = new StringBuilder();

		label.append("{").append(event).append("}");

		if (guard != null) {
			label.append(" [").append(format(guard.getFirst())).append("-").append(format(guard.getLast())).append("]");
		}

		if (probability != null) {
			label.append(" Prob: { ").append(format(probability)).append(" }");
		}

		return label.toString();
	}

	private static String format(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return Double.toString(value);
		}

		if (Math.abs(value) < ZERO_EPSILON) {
			return "0";
		}

		BigDecimal decimal = BigDecimal.valueOf(value).round(CONTEXT).stripTrailingZeros();

		return decimal.toPlainString();
	}
}