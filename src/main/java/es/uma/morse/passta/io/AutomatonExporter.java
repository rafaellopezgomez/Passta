package es.uma.morse.passta.io;

import java.nio.file.Path;
import java.util.Objects;

import es.uma.morse.passta.core.automaton.SRTA;

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

	public static void export(SRTA automaton, String target, AutomatonExportFormat format) {

		export(automaton, toTargetPath(target), format);
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

	public static void export(JavaFxGraphViewer.RenderedGraph renderedGraph, String target,
			AutomatonExportFormat format) {

		export(renderedGraph, toTargetPath(target), format);
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

		new UPPAAL(normalizedTarget, automaton);
	}

	private static Path toTargetPath(String target) {

		Objects.requireNonNull(target, "Target path is null");

		if (target.isBlank()) {
			throw new IllegalArgumentException("Target path is blank");
		}

		return Path.of(target);
	}
}