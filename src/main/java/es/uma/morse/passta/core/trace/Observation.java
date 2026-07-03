package es.uma.morse.passta.core.trace;

import java.util.ArrayList;

public record Observation(double time, String event,ArrayList<String> variables) {}