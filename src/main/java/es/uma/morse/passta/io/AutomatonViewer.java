package es.uma.morse.passta.io;

import java.util.List;
import java.util.Objects;

import org.graphper.api.Graphviz;

import es.uma.morse.passta.core.automaton.SRTA;

public final class AutomatonViewer {

	private AutomatonViewer() {
	}

	public static void show(SRTA automaton) {
		Objects.requireNonNull(automaton, "Automaton is null");

//		Graphviz graphviz = AutomatonGraphvizRenderer.toGraphviz(automaton);
//		openInBrowser(graphviz);

		JavaFxGraphViewer.GraphModel graphModel = SrtaGraphAdapter.convert(automaton);

		JavaFxGraphViewer.open(graphModel);
	}
}