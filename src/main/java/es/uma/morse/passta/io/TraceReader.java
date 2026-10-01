package es.uma.morse.passta.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.MappingIterator;

import es.uma.morse.passta.core.trace.Trace;

public final class TraceReader {

	private TraceReader() {
	}

	/**
	 * Streams traces from either a JSON file or a directory containing JSON files.
	 *
	 * <p>
	 * If {@code source} is a regular file, it is processed using
	 * {@link #readTracesFromFile(Path)}. If it is a directory, all JSON files
	 * contained directly in it are processed using
	 * {@link #readTracesFromDirectory(Path)}.
	 * </p>
	 *
	 * @param source path to a JSON file or to a directory containing JSON files
	 * @return a sequential stream yielding the traces found in the source
	 * @throws NullPointerException     if {@code source} is {@code null}
	 * @throws IllegalArgumentException if the source is neither a regular file nor
	 *                                  a directory
	 */
	public static Stream<Trace> readTraces(Path source) {

		Objects.requireNonNull(source, "Source path is null");

		Path path = source.toAbsolutePath().normalize();

		if (Files.isRegularFile(path)) {
			return readTracesFromFile(path);
		}

		if (Files.isDirectory(path)) {
			return readTracesFromDirectory(path);
		}

		throw new IllegalArgumentException("Source is neither a regular file nor a directory: " + path);
	}

	/**
	 * Streams traces from either a JSON file or a directory containing JSON files.
	 *
	 * @param source string representation of the path to a JSON file or directory
	 * @return a sequential stream yielding the traces found in the source
	 * @throws NullPointerException     if {@code source} is {@code null}
	 * @throws IllegalArgumentException if {@code source} is blank or does not
	 *                                  identify a regular file or directory
	 */
	public static Stream<Trace> readTraces(String source) {

		Objects.requireNonNull(source, "Source path is null");

		if (source.isBlank()) {
			throw new IllegalArgumentException("Source path is blank");
		}

		return readTraces(Path.of(source));
	}

	/**
	 * Streams traces from a JSON file without loading the full list into memory
	 * simultaneously.
	 *
	 * The input JSON file is expected to contain an array of Trace objects at the
	 * root.
	 *
	 * @param source path to the JSON file containing the traces
	 * @return a sequential stream yielding one trace at a time
	 * @throws UncheckedIOException if the file cannot be opened, read or closed
	 * 
	 */
	private static Stream<Trace> readTracesFromFile(Path source) {

		Path path = validateJsonFile(source);

		final MappingIterator<Trace> mappingIterator;

		try {

			mappingIterator = JsonSupport.traceReader().readValues(path.toFile());

		} catch (IOException e) {
			throw new UncheckedIOException("Cannot open traces file; " + path, e);
		}

		Iterator<Trace> iterator = new Iterator<>() {

			@Override
			public boolean hasNext() {

				try {
					return mappingIterator.hasNextValue();
				} catch (IOException e) {
					throw new UncheckedIOException("Cannot read traces from: " + path, e);
				}
			}

			@Override
			public Trace next() {

				try {
					return mappingIterator.nextValue();
				} catch (IOException e) {
					throw new UncheckedIOException("Cannot read trace from: " + path, e);
				}
			}
		};

		Spliterator<Trace> spliterator = Spliterators.spliteratorUnknownSize(iterator,
				Spliterator.ORDERED | Spliterator.NONNULL);

		return StreamSupport.stream(spliterator, false).onClose(() -> closeMappingIterator(mappingIterator, path));
	}

	/**
	 * Streams all traces from the JSON files contained directly in a directory.
	 * 
	 * Only regular files with a .json extension are processed.
	 * 
	 * @param sourceDirectory path to the directory containing JSON files
	 * @return a sequential stream yielding the traces found in the directory
	 */
	private static Stream<Trace> readTracesFromDirectory(Path sourceDirectory) {

		Path directory = validateDirectory(sourceDirectory);

		final Stream<Path> files;

		try {
			files = Files.list(directory);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot list traces directory: " + directory, e);
		}

		Stream<Trace> traces = files.filter(Files::isRegularFile).filter(TraceReader::isJsonFile)
				.sorted(Comparator.comparing(path -> path.getFileName().toString()))
				.flatMap(TraceReader::readTracesFromFile);

		return traces.onClose(files::close);
	}

	private static void closeMappingIterator(MappingIterator<Trace> iterator, Path source) {

		try {
			iterator.close();
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot close traces file; " + source, e);
		}
	}

	private static Path validateDirectory(Path sourceDirectory) {

		Objects.requireNonNull(sourceDirectory, "Source directory is null");

		Path directory = sourceDirectory.toAbsolutePath().normalize();

		if (!Files.isDirectory(directory)) {
			throw new IllegalArgumentException("Source is not a directory: " + directory);
		}

		return directory;
	}

	private static boolean isJsonFile(Path file) {

		String fileName = file.getFileName().toString().toLowerCase();

		return fileName.endsWith(".json");
	}

	private static Path validateJsonFile(Path source) {
		Objects.requireNonNull(source, "Source path is null");

		Path path = source.toAbsolutePath().normalize();

		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException("Source is not a regular file: " + path);
		}

		if (!isJsonFile(path)) {
			throw new IllegalArgumentException("Source file must have .json extension: " + path);
		}

		return path;
	}
}