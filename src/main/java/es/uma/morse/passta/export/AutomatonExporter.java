package es.uma.morse.passta.export;

import java.nio.file.Path;
import java.util.Objects;

import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.visualization.javafx.JavaFxGraphViewer;
import es.uma.morse.passta.visualization.javafx.JavaFxRuntime;
import es.uma.morse.passta.visualization.javafx.SrtaGraphAdapter;

public final class AutomatonExporter {

	private AutomatonExporter() {
	}

	public static void export(SRTA automaton, Path target, AutomatonExportFormat format) {

		Objects.requireNonNull(automaton, "Automaton is null");

		Objects.requireNonNull(target, "Target path is null");

		Objects.requireNonNull(format, "Export format is null");

		switch (format) {
		case PNG, SVG -> exportRenderedAutomaton(automaton, target, format);

		case UPPAAL -> exportToUppaal(automaton, target);
		}
	}

	public static void export(JavaFxGraphViewer.RenderedGraph renderedGraph, Path target,
			AutomatonExportFormat format) {

		Objects.requireNonNull(renderedGraph, "Rendered graph is null");

		Objects.requireNonNull(target, "Target path is null");

		Objects.requireNonNull(format, "Export format is null");

		switch (format) {
		case PNG, SVG -> JavaFxRuntime.runAndWait(() -> exportRenderedGraph(renderedGraph, target, format));

		case UPPAAL -> throw new IllegalArgumentException("UPPAAL export requires an SRTA automaton");
		}
	}

	private static void exportRenderedAutomaton(SRTA automaton, Path target, AutomatonExportFormat format) {

		JavaFxRuntime.runAndWait(() -> {
			JavaFxGraphViewer.GraphModel graph = SrtaGraphAdapter.convert(automaton);

			JavaFxGraphViewer.RenderedGraph renderedGraph = JavaFxGraphViewer.renderGraph(graph);

			exportRenderedGraph(renderedGraph, target, format);
		});
	}
	
	public static Path resolveTarget(
	        Path target,
	        AutomatonExportFormat format) {

	    Objects.requireNonNull(
	        target,
	        "Target path is null"
	    );

	    Objects.requireNonNull(
	        format,
	        "Export format is null"
	    );

	    return switch (format) {
	    case PNG -> ExportPathResolver.resolve(
	        target,
	        ".png"
	    );

	    case SVG -> ExportPathResolver.resolve(
	        target,
	        ".svg"
	    );

	    case UPPAAL -> target
	        .toAbsolutePath()
	        .normalize();
	    };
	}

	private static void exportRenderedGraph(JavaFxGraphViewer.RenderedGraph renderedGraph, Path target,
			AutomatonExportFormat format) {

		switch (format) {
		case PNG -> AutomatonPngExporter.export(renderedGraph, target);

		case SVG -> AutomatonSvgExporter.export(renderedGraph, target);

		case UPPAAL -> throw new IllegalArgumentException("UPPAAL export requires an SRTA automaton");
		}
	}

	private static void exportToUppaal(SRTA automaton, Path target) {

		Path normalizedTarget = target.toAbsolutePath().normalize();

		ExportPathResolver.createParentDirectories(normalizedTarget);

		new AutomatonUppaalExporter(normalizedTarget, automaton);
	}
}