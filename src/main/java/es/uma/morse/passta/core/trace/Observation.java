package es.uma.morse.passta.core.trace;

import java.util.List;

public record Observation(double time, String event, List<String> variables) {}