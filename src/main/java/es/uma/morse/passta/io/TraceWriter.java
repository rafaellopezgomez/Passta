package es.uma.morse.passta.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import es.uma.morse.passta.core.trace.Trace;



public final class TraceWriter {

    private static final String DEFAULT_TRACES_FILE_NAME = "traces.json";

    private TraceWriter() {
    }

    public static void writeTraces(Path destination, List<Trace> traces) {
        Objects.requireNonNull(destination, "Destination path is null");
        Objects.requireNonNull(traces, "Traces list is null");

        if (traces.isEmpty()) {
            throw new IllegalArgumentException("Traces list is empty");
        }

        Path file = resolveTraceFile(destination.toAbsolutePath().normalize());

        try {
            Path parent = file.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            JsonSupport.prettyWriter().writeValue(file.toFile(), traces);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write traces to: " + file, e);
        }
    }

    public static void writeTraces(String destination, List<Trace> traces) {
        Objects.requireNonNull(destination, "Destination path is null");

        if (destination.isBlank()) {
            throw new IllegalArgumentException("Destination path is blank");
        }

        writeTraces(Path.of(destination), traces);
    }

    private static Path resolveTraceFile(Path destination) {
        String value = destination.toString().toLowerCase();

        if (value.endsWith(".json")) {
            return destination;
        }

        return destination.resolve(DEFAULT_TRACES_FILE_NAME);
    }
}