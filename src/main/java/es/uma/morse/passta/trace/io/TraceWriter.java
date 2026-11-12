package es.uma.morse.passta.trace.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import es.uma.morse.passta.core.trace.Trace;

/**
 * Writes collections of traces to JSON files.
 */
public final class TraceWriter {

	private static final String DEFAULT_TRACES_FILE_NAME = "traces.json";

	private TraceWriter() {
	}

	/**
	 * Writes traces to a JSON file.
	 *
	 * <p>
	 * If {@code destination} is an existing directory, the traces are written to
	 * {@code traces.json} inside that directory. If its file name ends in
	 * {@code .json}, it is treated as the output file. Otherwise, it is treated as
	 * a directory and {@code traces.json} is appended.
	 * </p>
	 *
	 * @param destination output JSON file or destination directory
	 * @param traces      traces to serialize
	 * @throws NullPointerException     if either argument is {@code null}
	 * @throws IllegalArgumentException if the trace list is empty, contains null
	 *                                  elements, or the destination is an existing
	 *                                  non-JSON regular file
	 * @throws UncheckedIOException     if the output directory or file cannot be
	 *                                  created or written
	 */
	public static void writeTraces(Path destination, List<Trace> traces) {
		Objects.requireNonNull(destination, "Destination path is null");
		Objects.requireNonNull(traces, "Traces list is null");

		if (traces.isEmpty()) {
			throw new IllegalArgumentException("Traces list is empty");
		}

		if (traces.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("Traces list contains null elements");
		}

		Path normalizedDestination = destination.toAbsolutePath().normalize();

		Path outputFile = resolveTraceFile(normalizedDestination);

		try {
			createParentDirectories(outputFile);

			TraceJsonSupport.tracesWriter().writeValue(outputFile.toFile(), traces);

		} catch (IOException e) {
			throw new RuntimeException("Cannot write traces to: " + outputFile, e);
		}
	}

	private static Path resolveTraceFile(Path destination) {

		if (Files.isDirectory(destination)) {
			return destination.resolve(DEFAULT_TRACES_FILE_NAME);
		}

		if (Files.isRegularFile(destination)) {
			if (TraceJsonSupport.isJsonFile(destination)) {
				return destination;
			}

			throw new IllegalArgumentException("Destination is an existing non-JSON file: " + destination);
		}

		if (TraceJsonSupport.isJsonFile(destination)) {
			return destination;
		}

		return destination.resolve(DEFAULT_TRACES_FILE_NAME);
	}

	private static void createParentDirectories(Path outputFile) throws IOException {

		Path parent = outputFile.getParent();

		if (parent != null) {
			Files.createDirectories(parent);
		}
	}
}