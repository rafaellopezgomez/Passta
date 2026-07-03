package es.uma.morse.passta.io;

import java.util.Objects;

import org.graphper.api.Graphviz;

import es.uma.morse.passta.core.automaton.SRTA;

public final class AutomatonViewer {

    private AutomatonViewer() {
    }

    public static void show(SRTA automaton) {
        Objects.requireNonNull(automaton, "Automaton is null");

        Graphviz graphviz = AutomatonGraphvizRenderer.toGraphviz(automaton);
        openInBrowser(graphviz);
    }

    private static void openInBrowser(Graphviz graphviz) {
        Objects.requireNonNull(graphviz, "Graphviz instance is null");

        try {
            String svg = graphviz.toSvgStr();
            SvgViewer.openSvgInBrowser(svg);
        } catch (Exception e) {
            throw new RuntimeException("Cannot open automaton layout in browser", e);
        }
    }
}