package es.uma.morse.passta.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

public final class AutomatonSvgExporter {

	private static final double MARGIN = 30.0;

	private AutomatonSvgExporter() {
	}

	public static void export(JavaFxGraphViewer.RenderedGraph graph, Path target) {

		Objects.requireNonNull(graph, "Rendered graph is null");

		Objects.requireNonNull(target, "Target path is null");

		JavaFxGraphPreparer.prepare(graph);

		Path outputFile = ExportPathResolver.resolve(target, ".svg");

		ExportPathResolver.createParentDirectories(outputFile);

		try {
			String svg = createSvg(graph);

			Files.writeString(outputFile, svg, StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new RuntimeException("Cannot export automaton to SVG: " + outputFile, exception);
		}
	}

	private static String createSvg(JavaFxGraphViewer.RenderedGraph graph) {

		Bounds bounds = graph.pane().getBoundsInLocal();

		double minX = bounds.getMinX() - MARGIN;
		double minY = bounds.getMinY() - MARGIN;

		double width = Math.max(1.0, bounds.getWidth() + MARGIN * 2.0);

		double height = Math.max(1.0, bounds.getHeight() + MARGIN * 2.0);

		StringBuilder svg = new StringBuilder();

		svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

		svg.append("<svg");
		svg.append(" xmlns=\"http://www.w3.org/2000/svg\"");
		svg.append(" version=\"1.1\"");
		svg.append(" viewBox=\"");
		svg.append(number(minX)).append(" ");
		svg.append(number(minY)).append(" ");
		svg.append(number(width)).append(" ");
		svg.append(number(height));
		svg.append("\"");
		svg.append(" width=\"").append(number(width)).append("\"");
		svg.append(" height=\"").append(number(height)).append("\"");
		svg.append(">\n");

		appendStyles(svg);
		appendBackground(svg, minX, minY, width, height);

		svg.append("  <g id=\"automaton\">\n");

		svg.append("    <g id=\"edges\">\n");

		for (JavaFxGraphViewer.EdgeView edge : graph.edges()) {
			appendEdge(svg, edge);
		}

		svg.append("    </g>\n");

		svg.append("    <g id=\"initial-markers\">\n");

		for (JavaFxGraphViewer.InitialStateMarker marker : graph.initialMarkers()) {

			appendInitialMarker(svg, marker);
		}

		svg.append("    </g>\n");

		svg.append("    <g id=\"states\">\n");

		for (JavaFxGraphViewer.StateView state : graph.states()) {
			appendState(svg, state);
		}

		svg.append("    </g>\n");
		svg.append("  </g>\n");
		svg.append("</svg>\n");

		return svg.toString();
	}

	private static void appendStyles(StringBuilder svg) {

		svg.append("  <style>\n");

		svg.append("    .state {" + " fill: #ffffff;" + " stroke: #2f4f6f;" + " stroke-width: 2;" + " }\n");

		svg.append("    .state-label {" + " fill: #1f1f1f;" + " font-family: sans-serif;" + " font-size: 13px;"
				+ " text-anchor: middle;" + " dominant-baseline: middle;" + " }\n");

		svg.append("    .edge {" + " fill: none;" + " stroke: #555555;" + " stroke-width: 1.8;" + " }\n");

		svg.append("    .edge-arrow {" + " fill: #555555;" + " }\n");

		svg.append("    .edge-label-background {" + " fill: #ffffff;" + " fill-opacity: 0.90;" + " }\n");

		svg.append(
				"    .edge-label {" + " fill: #333333;" + " font-family: sans-serif;" + " font-size: 11px;" + " }\n");

		svg.append("    .initial-line {" + " stroke: #2f4f6f;" + " stroke-width: 2;" + " }\n");

		svg.append("    .initial-arrow {" + " fill: #2f4f6f;" + " }\n");

		svg.append("  </style>\n");
	}

	private static void appendBackground(StringBuilder svg, double x, double y, double width, double height) {

		svg.append("  <rect");
		svg.append(" x=\"").append(number(x)).append("\"");
		svg.append(" y=\"").append(number(y)).append("\"");
		svg.append(" width=\"").append(number(width)).append("\"");
		svg.append(" height=\"").append(number(height)).append("\"");
		svg.append(" fill=\"#ffffff\"");
		svg.append("/>\n");
	}

	private static void appendState(StringBuilder svg, JavaFxGraphViewer.StateView state) {

		String id = escapeAttribute(state.getStateId());

		svg.append("      <g id=\"state-");
		svg.append(id);
		svg.append("\">\n");

		svg.append("        <circle");
		svg.append(" class=\"state\"");
		svg.append(" cx=\"");
		svg.append(number(state.getCenterX()));
		svg.append("\"");
		svg.append(" cy=\"");
		svg.append(number(state.getCenterY()));
		svg.append("\"");
		svg.append(" r=\"");
		svg.append(number(state.getRadius()));
		svg.append("\"");
		svg.append("/>\n");

		appendCenteredMultilineText(svg, state.getLabelText(), state.getCenterX(), state.getCenterY(), "state-label",
				16.0, 8);

		svg.append("      </g>\n");
	}

	private static void appendEdge(StringBuilder svg, JavaFxGraphViewer.EdgeView edge) {

		CubicCurve curve = edge.getCurve();

		svg.append("      <g id=\"edge-");
		svg.append(escapeAttribute(edge.getEdgeId()));
		svg.append("\">\n");

		svg.append("        <path");
		svg.append(" class=\"edge\"");
		svg.append(" d=\"M ");
		svg.append(number(curve.getStartX()));
		svg.append(" ");
		svg.append(number(curve.getStartY()));

		svg.append(" C ");
		svg.append(number(curve.getControlX1()));
		svg.append(" ");
		svg.append(number(curve.getControlY1()));
		svg.append(", ");

		svg.append(number(curve.getControlX2()));
		svg.append(" ");
		svg.append(number(curve.getControlY2()));
		svg.append(", ");

		svg.append(number(curve.getEndX()));
		svg.append(" ");
		svg.append(number(curve.getEndY()));

		svg.append("\"/>\n");

		appendPolygon(svg, edge.getArrow(), "edge-arrow", 8);

		appendEdgeLabel(svg, edge.getEdgeLabel());

		svg.append("      </g>\n");
	}

	private static void appendEdgeLabel(StringBuilder svg, Label label) {

		label.applyCss();
		label.autosize();

		double x = label.getLayoutX();
		double y = label.getLayoutY();

		double width = label.getWidth();
		double height = label.getHeight();

		svg.append("        <rect");
		svg.append(" class=\"edge-label-background\"");
		svg.append(" x=\"").append(number(x)).append("\"");
		svg.append(" y=\"").append(number(y)).append("\"");
		svg.append(" width=\"").append(number(width)).append("\"");
		svg.append(" height=\"").append(number(height)).append("\"");
		svg.append(" rx=\"4\"");
		svg.append(" ry=\"4\"");
		svg.append("/>\n");

		String text = label.getText();

		if (text == null || text.isBlank()) {
			return;
		}

		String[] lines = text.split("\\R", -1);

		double textX = x + 5.0;
		double lineHeight = 14.0;

		double textY = y + 3.0 + lineHeight * 0.8;

		svg.append("        <text");
		svg.append(" class=\"edge-label\"");
		svg.append(" x=\"").append(number(textX)).append("\"");
		svg.append(" y=\"").append(number(textY)).append("\"");
		svg.append(">\n");

		for (int index = 0; index < lines.length; index++) {
			svg.append("          <tspan");
			svg.append(" x=\"").append(number(textX)).append("\"");

			if (index > 0) {
				svg.append(" dy=\"");
				svg.append(number(lineHeight));
				svg.append("\"");
			}

			svg.append(">");
			svg.append(escapeText(lines[index]));
			svg.append("</tspan>\n");
		}

		svg.append("        </text>\n");
	}

	private static void appendInitialMarker(StringBuilder svg, JavaFxGraphViewer.InitialStateMarker marker) {

		Line line = marker.getLine();

		svg.append("      <g>\n");

		svg.append("        <line");
		svg.append(" class=\"initial-line\"");
		svg.append(" x1=\"");
		svg.append(number(line.getStartX()));
		svg.append("\"");
		svg.append(" y1=\"");
		svg.append(number(line.getStartY()));
		svg.append("\"");
		svg.append(" x2=\"");
		svg.append(number(line.getEndX()));
		svg.append("\"");
		svg.append(" y2=\"");
		svg.append(number(line.getEndY()));
		svg.append("\"");
		svg.append("/>\n");

		appendPolygon(svg, marker.getArrow(), "initial-arrow", 8);

		svg.append("      </g>\n");
	}

	private static void appendCenteredMultilineText(StringBuilder svg, String text, double centerX, double centerY,
			String cssClass, double lineHeight, int indentation) {

		if (text == null || text.isBlank()) {
			return;
		}

		String indent = " ".repeat(indentation);
		String[] lines = text.split("\\R", -1);

		double firstLineY = centerY - (lines.length - 1) * lineHeight / 2.0;

		svg.append(indent);
		svg.append("<text");
		svg.append(" class=\"").append(cssClass).append("\"");
		svg.append(" x=\"").append(number(centerX)).append("\"");
		svg.append(" y=\"").append(number(firstLineY)).append("\"");
		svg.append(">\n");

		for (int index = 0; index < lines.length; index++) {
			svg.append(indent);
			svg.append("  <tspan");
			svg.append(" x=\"").append(number(centerX)).append("\"");

			if (index > 0) {
				svg.append(" dy=\"");
				svg.append(number(lineHeight));
				svg.append("\"");
			}

			svg.append(">");
			svg.append(escapeText(lines[index]));
			svg.append("</tspan>\n");
		}

		svg.append(indent);
		svg.append("</text>\n");
	}

	private static void appendPolygon(StringBuilder svg, Polygon polygon, String cssClass, int indentation) {

		List<Double> points = polygon.getPoints();

		if (points.isEmpty()) {
			return;
		}

		String indent = " ".repeat(indentation);

		svg.append(indent);
		svg.append("<polygon");
		svg.append(" class=\"").append(cssClass).append("\"");
		svg.append(" points=\"");

		for (int index = 0; index + 1 < points.size(); index += 2) {

			if (index > 0) {
				svg.append(" ");
			}

			svg.append(number(points.get(index)));
			svg.append(",");
			svg.append(number(points.get(index + 1)));
		}

		svg.append("\"/>\n");
	}

	private static String number(double value) {
		if (!Double.isFinite(value)) {
			throw new IllegalArgumentException("SVG coordinate is not finite: " + value);
		}

		if (value == Math.rint(value)) {
			return Long.toString((long) value);
		}

		return String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
	}

	private static String escapeText(String text) {

		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String escapeAttribute(String text) {

		return escapeText(text).replace("\"", "&quot;").replace("'", "&apos;");
	}
}