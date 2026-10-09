package es.uma.morse.passta.io;

import java.util.Objects;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;

final class JavaFxGraphPreparer {

	private JavaFxGraphPreparer() {
	}

	static void prepare(JavaFxGraphViewer.RenderedGraph renderedGraph) {

		Objects.requireNonNull(renderedGraph, "Rendered graph is null");

		requireJavaFxThread();

		Parent root = renderedGraph.pane();

		if (root.getScene() == null) {
			Parent sceneRoot = findSceneRoot(root);

			new Scene(sceneRoot);
		}

		root.applyCss();
		root.layout();
	}

	private static Parent findSceneRoot(Parent node) {

		Parent root = node;

		while (root.getParent() != null) {
			root = root.getParent();
		}

		return root;
	}

	private static void requireJavaFxThread() {
		if (!Platform.isFxApplicationThread()) {
			throw new IllegalStateException("The JavaFX graph must be prepared " + "on the JavaFX Application Thread");
		}
	}
}