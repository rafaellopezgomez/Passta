package es.uma.morse.passta.validation;

/**
 * Result of validating a collection of traces against an automaton.
 *
 * @param totalTraces    total number of processed traces
 * @param acceptedTraces number of accepted traces
 */
public record ValidationResult(
        int totalTraces,
        int acceptedTraces
) {

    public ValidationResult {

        if (totalTraces < 0) {
            throw new IllegalArgumentException(
                    "Total number of traces cannot be negative"
            );
        }

        if (acceptedTraces < 0) {
            throw new IllegalArgumentException(
                    "Number of accepted traces cannot be negative"
            );
        }

        if (acceptedTraces > totalTraces) {
            throw new IllegalArgumentException(
                    "Number of accepted traces cannot exceed total traces"
            );
        }
    }

    /**
     * Returns the number of rejected traces.
     *
     * @return number of rejected traces
     */
    public int rejectedTraces() {
        return totalTraces - acceptedTraces;
    }

    /**
     * Returns the acceptance rate as a value between 0.0 and 1.0.
     *
     * @return acceptance rate
     */
    public double acceptanceRate() {
        if (totalTraces == 0) {
            return 0.0;
        }

        return (double) acceptedTraces / totalTraces;
    }
}