package es.uma.morse.passta.io;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.graphper.api.Graphviz;
import org.graphper.api.Graphviz.GraphvizBuilder;

import es.uma.morse.passta.core.automaton.SRTA;

import org.graphper.api.Line;
import org.graphper.api.Node;

public final class AutomatonGraphvizRenderer {

    private AutomatonGraphvizRenderer() {
    }

    public static Graphviz toGraphviz(SRTA automaton) {
        Objects.requireNonNull(automaton, "Automaton is null");

        GraphvizBuilder graph = Graphviz.digraph();

        Map<String, Node> nodes = new TreeMap<>();

        automaton.getAllLocations().forEach(location -> {
            String id = String.valueOf(location.getId());
            String attrs = String.valueOf(location.getAttrs());
            String invariant = String.valueOf(location.getInvariant());

            Node node = Node.builder()
                    .label(id + " " + attrs + "\n" + "<= " + invariant)
                    .build();

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
                    .label(buildEdgeLabel(edge.getEvent(), edge.getGuard(), edge.getProb()))
                    .build();

            graph.addLine(line);
        });

        return graph.build();
    }

    private static String buildEdgeLabel(Object event, Object guard, Double probability) {
        StringBuilder label = new StringBuilder();

        label.append("{")
                .append(event)
                .append("}");

        if (guard != null) {
            label.append(" ")
                    .append(guard);
        }

        if (probability != null) {
            label.append(" Prob: { ")
                    .append(formatProbability(probability))
                    .append(" }");
        }

        return label.toString();
    }

    private static String formatProbability(Double value) {
        DecimalFormat formatter =
                new DecimalFormat("0.####", new DecimalFormatSymbols(Locale.ENGLISH));

        return formatter.format(value);
    }
}