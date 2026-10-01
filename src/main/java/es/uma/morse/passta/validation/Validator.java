package es.uma.morse.passta.validation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import es.uma.morse.passta.core.Passta;
import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.core.automaton.SRTALocation;
import es.uma.morse.passta.core.trace.Observation;
import es.uma.morse.passta.core.trace.Trace;
import es.uma.morse.passta.io.TraceReader;
import es.uma.morse.passta.io.TraceWriter;

public final class Validator {

    private Validator() {
    }

    /*
     * String overloads
     */

    public static ValidationResult nValidTraces(
            String source,
            SRTA automaton
    ) {
        return nValidTraces(source, automaton, (Path) null);
    }

    public static ValidationResult nValidTraces(
            String source,
            SRTA automaton,
            String destination
    ) {
        return nValidTraces(
                source,
                automaton,
                toOptionalPath(destination)
        );
    }

    public static ValidationResult nValidTraces(
            String source,
            SRTA automaton,
            Path destination
    ) {
        Objects.requireNonNull(source, "Source path is null");

        if (source.isBlank()) {
            throw new IllegalArgumentException(
                    "Source path is blank"
            );
        }

        return nValidTraces(
                Path.of(source),
                automaton,
                destination
        );
    }

    /*
     * Path overloads
     */

    public static ValidationResult nValidTraces(
            Path source,
            SRTA automaton
    ) {
        return nValidTraces(source, automaton, (Path) null);
    }

    public static ValidationResult nValidTraces(
            Path source,
            SRTA automaton,
            String destination
    ) {
        return nValidTraces(
                source,
                automaton,
                toOptionalPath(destination)
        );
    }

    public static ValidationResult nValidTraces(
            Path source,
            SRTA automaton,
            Path destination
    ) {
        Objects.requireNonNull(source, "Source path is null");
        Objects.requireNonNull(automaton, "Automaton is null");

        Path normalizedSource =
                source.toAbsolutePath().normalize();

        try (Stream<Trace> traces =
                TraceReader.readTraces(normalizedSource)) {

            return validateTraces(
                    traces,
                    automaton,
                    destination,
                    normalizedSource.toString()
            );
        }
    }

    /*
     * Common stream-based implementation
     */

    private static ValidationResult validateTraces(
            Stream<Trace> traces,
            SRTA automaton,
            Path destination,
            String sourceDescription
    ) {
        Objects.requireNonNull(
                traces,
                "Traces stream is null"
        );

        Objects.requireNonNull(
                automaton,
                "Automaton is null"
        );

        Objects.requireNonNull(
                sourceDescription,
                "Source description is null"
        );

        int acceptedCount = 0;
        int totalCount = 0;

        List<Trace> rejectedTraces = new ArrayList<>();

        var iterator = traces.iterator();

        while (iterator.hasNext()) {

            Trace trace = Objects.requireNonNull(
                    iterator.next(),
                    "Null trace found in: " + sourceDescription
            );

            totalCount++;

            Trace compressedTrace = Objects.requireNonNull(
                    Passta.compressTrace(trace),
                    "Compressed trace is null"
            );

            if (checkTrace(compressedTrace, automaton)) {
                acceptedCount++;
            } else {
                rejectedTraces.add(compressedTrace);
            }
        }

        if (totalCount == 0) {
            throw new IllegalArgumentException(
                    "No traces were found in: "
                            + sourceDescription
            );
        }

        if (destination != null && !rejectedTraces.isEmpty()) {
            TraceWriter.writeTraces(
                    destination,
                    rejectedTraces
            );
        }

        return new ValidationResult(
                totalCount,
                acceptedCount
        );
    }

    /*
     * Utility method for optional destinations
     */

    private static Path toOptionalPath(String destination) {

        if (destination == null || destination.isBlank()) {
            return null;
        }

        return Path.of(destination);
    }

    public static boolean checkTrace(
            Trace trace,
            SRTA automaton
    ) {
        Objects.requireNonNull(trace, "Trace is null");
        Objects.requireNonNull(automaton, "Automaton is null");

        SRTALocation lastLocation = automaton.getLocation(0);
        double lastTimeStamp = 0.0;

        for (Observation observation : trace.getObs()) {

            String event = observation.event().isEmpty()
                    ? "□"
                    : observation.event();

            double timeDelta =
                    observation.time() - lastTimeStamp;

            List<String> variables =
                    observation.variables();

            var possibleEdge = lastLocation.getOutEdges()
                    .stream()
                    .map(automaton::getEdge)
                    .filter(edge ->
                            edge.getEvent().equals(event)
                                    && timeDelta >= edge.getMin()
                                    && timeDelta <= edge.getMax()
                                    && automaton
                                            .getLocation(
                                                    edge.getTargetId()
                                            )
                                            .getAttrs()
                                            .equals(variables)
                    )
                    .findFirst();

            if (possibleEdge.isEmpty()) {

                var edgesWithEvent = lastLocation.getOutEdges()
                        .stream()
                        .map(automaton::getEdge)
                        .filter(edge ->
                                edge.getEvent().equals(event)
                        )
                        .collect(Collectors.toList());

                if (!edgesWithEvent.isEmpty()) {

                    var systemAttrs = edgesWithEvent.stream()
                            .map(edge ->
                                    automaton
                                            .getLocation(
                                                    edge.getTargetId()
                                            )
                                            .getAttrs()
                            )
                            .collect(Collectors.toList());

                    var guards = edgesWithEvent.stream()
                            .map(edge -> edge.getGuard())
                            .collect(Collectors.toList());

                    if (systemAttrs.stream()
                            .noneMatch(attrs ->
                                    attrs.equals(variables))) {

                        String systemAttrsString =
                                systemAttrs.stream()
                                        .map(Object::toString)
                                        .collect(
                                                Collectors.joining(", ")
                                        );

                        observation.variables().add(
                                "Error: the automaton recognizes "
                                        + "the event but not the system "
                                        + "attributes. Available system "
                                        + "attributes of target states "
                                        + "from state "
                                        + lastLocation
                                        + ": "
                                        + systemAttrsString
                        );
                    }

                    boolean validTime = guards.stream()
                            .anyMatch(guard ->
                                    timeDelta >= guard.getFirst()
                                            && timeDelta
                                                    <= guard.getLast()
                            );

                    if (!validTime) {

                        String guardsString =
                                guards.stream()
                                        .map(Object::toString)
                                        .collect(
                                                Collectors.joining(", ")
                                        );

                        observation.variables().add(
                                "Error: the automaton recognizes "
                                        + "the event but not the time "
                                        + "delta "
                                        + timeDelta
                                        + ". Guards of outgoing edges "
                                        + "from state "
                                        + lastLocation
                                        + ", that have the same event: "
                                        + guardsString
                        );
                    }

                } else {

                    String events = lastLocation.getOutEdges()
                            .stream()
                            .map(automaton::getEdge)
                            .map(edge -> edge.getEvent())
                            .collect(Collectors.joining(", "));

                    observation.variables().add(
                            "Error: the automaton does not "
                                    + "recognize the event. Events "
                                    + "available from state "
                                    + lastLocation
                                    + ": "
                                    + events
                    );
                }

                return false;
            }

            var edge = possibleEdge.get();

            lastTimeStamp = observation.time();
            lastLocation = automaton.getLocation(
                    edge.getTargetId()
            );
        }

        return true;
    }
}