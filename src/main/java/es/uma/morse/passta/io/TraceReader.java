package es.uma.morse.passta.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.MappingIterator;

import es.uma.morse.passta.core.trace.Trace;

public final class TraceReader {

    private TraceReader() {
    }

    /**
     * Reads all traces from a JSON file.
     *
     * @param source path to the JSON file containing the traces
     * @return list of traces read from the file
     */
    public static List<Trace> readTraces(Path source) {
        Path path = validateJsonFile(source);

        try {
            return JsonSupport.tracesReader().readValue(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Cannot read traces from: " + path, e);
        }
    }

    /**
     * Reads all traces from a JSON file.
     *
     * @param source path to the JSON file containing the traces
     * @return list of traces read from the file
     */
    public static List<Trace> readTraces(String source) {
        Objects.requireNonNull(source, "Source path is null");

        if (source.isBlank()) {
            throw new IllegalArgumentException("Source path is blank");
        }

        return readTraces(Path.of(source));
    }

    /**
     * Streams traces from a JSON file without loading the full list into memory.
     *
     * The input JSON file is expected to contain an array of Trace objects at the root.
     *
     * The returned MappingIterator owns the underlying parser, so the caller must
     * consume it fully or close it explicitly. Prefer using try-with-resources.
     *
     * @param source path to the JSON file containing the traces
     * @return iterator yielding one Trace at a time
     * @throws IOException if parsing fails
     */
    public static MappingIterator<Trace> streamTraces(Path source) throws IOException {
        Path path = validateJsonFile(source);
        return JsonSupport.tracesReader().readValues(path.toFile());
    }

    /**
     * Streams traces from a JSON file without loading the full list into memory.
     *
     * @param source path to the JSON file containing the traces
     * @return iterator yielding one Trace at a time
     * @throws IOException if parsing fails
     */
    public static MappingIterator<Trace> streamTraces(String source) throws IOException {
        Objects.requireNonNull(source, "Source path is null");

        if (source.isBlank()) {
            throw new IllegalArgumentException("Source path is blank");
        }

        return streamTraces(Path.of(source));
    }

    private static Path validateJsonFile(Path source) {
        Objects.requireNonNull(source, "Source path is null");

        Path path = source.toAbsolutePath().normalize();

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Source is not a regular file: " + path);
        }

        if (!path.toString().toLowerCase().endsWith(".json")) {
            throw new IllegalArgumentException("Source file must have .json extension: " + path);
        }

        return path;
    }
}