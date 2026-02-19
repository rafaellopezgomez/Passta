package trace;

import java.util.ArrayList;

public record Observation(double time, String event,ArrayList<String> systemAttrs) {}