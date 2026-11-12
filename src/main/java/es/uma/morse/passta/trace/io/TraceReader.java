package es.uma.morse.passta.trace.io;

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

import tools.jackson.databind.MappingIterator;

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
	 * <p>
	 * The returned stream holds I/O resources and must be closed, preferably using
	 * a try-with-resources statement.
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
	 * Streams traces from a JSON file without loading the full list into memory
	 * simultaneously.
	 *
	 * <p>
	 * The input JSON file must to contain an array of {@link Trace} objects at its
	 * root.
	 * <p>
	 *
	 * @param source path to the JSON file containing the traces
	 * @return a sequential stream yielding one trace at a time
	 */
	private static Stream<Trace> readTracesFromFile(Path source) {
		Path path = validateJsonFile(source);

		MappingIterator<Trace> mappingIterator = TraceJsonSupport.traceReader().readValues(path.toFile());

		Iterator<Trace> iterator = new Iterator<>() {

			@Override
			public boolean hasNext() {
				return mappingIterator.hasNextValue();
			}

			@Override
			public Trace next() {
				return mappingIterator.nextValue();
			}
		};

		Spliterator<Trace> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);

		return StreamSupport.stream(spliterator, false).onClose(mappingIterator::close);
	}

	/**
	 * Streams all traces from the JSON files contained directly in a directory.
	 * 
     * <p>Only regular files with a {@code .json} extension are processed.
     * Files are processed in lexicographical order by file name.</p>
	 * 
	 * @param sourceDirectory directory containing the JSON files
	 * @return a sequential stream yielding the traces found in the directory
	 */
	private static Stream<Trace> readTracesFromDirectory(Path sourceDirectory) {
		Path directory = validateDirectory(sourceDirectory);

		try {
			Stream<Path> files = Files.list(directory);
			
			return files.filter(Files::isRegularFile).filter(TraceJsonSupport::isJsonFile).sorted(Comparator.comparing(path -> path.getFileName().toString())).flatMap(TraceReader::readTracesFromFile).onClose(files::close);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot list traces directory: " + directory, e);
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

	private static Path validateJsonFile(Path source) {
		Objects.requireNonNull(source, "Source path is null");

		Path path = source.toAbsolutePath().normalize();

		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException("Source is not a regular file: " + path);
		}

		if (!TraceJsonSupport.isJsonFile(path)) {
			throw new IllegalArgumentException("Source file must have .json extension: " + path);
		}

		return path;
	}
}