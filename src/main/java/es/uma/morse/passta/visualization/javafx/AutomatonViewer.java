package es.uma.morse.passta.visualization.javafx;

import java.util.Objects;

import es.uma.morse.passta.core.automaton.SRTA;

public final class AutomatonViewer {

	private AutomatonViewer() {
	}

	public static void show(SRTA automaton) {
		Objects.requireNonNull(automaton, "Automaton is null");

		JavaFxGraphViewer.GraphModel graphModel = SrtaGraphAdapter.convert(automaton);

		JavaFxGraphViewer.open(graphModel);
	}
}