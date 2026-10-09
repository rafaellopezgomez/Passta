package es.uma.morse.passta.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

final class AutomatonPngExporter {

	private static final String PNG_EXTENSION = ".png";

	private static final double IMAGE_MARGIN = 30.0;

	private AutomatonPngExporter() {
	}

	static void export(JavaFxGraphViewer.RenderedGraph renderedGraph, Path target) {

		Objects.requireNonNull(renderedGraph, "Rendered graph is null");

		Objects.requireNonNull(target, "Target path is null");

		JavaFxGraphPreparer.prepare(renderedGraph);

		Parent renderedAutomaton = renderedGraph.pane();

		Path outputFile = ExportPathResolver.resolve(target, PNG_EXTENSION);

		ExportPathResolver.createParentDirectories(outputFile);

		try {
			WritableImage image = createSnapshot(renderedAutomaton);

			boolean written = ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", outputFile.toFile());

			if (!written) {
				throw new IOException("No PNG image writer is available");
			}
		} catch (IOException exception) {
			throw new RuntimeException("Cannot export automaton to PNG: " + outputFile, exception);
		}
	}

	private static WritableImage createSnapshot(Parent renderedAutomaton) {

		Bounds bounds = renderedAutomaton.getBoundsInLocal();

		double minX = bounds.getMinX() - IMAGE_MARGIN;

		double minY = bounds.getMinY() - IMAGE_MARGIN;

		double width = Math.max(1.0, bounds.getWidth() + IMAGE_MARGIN * 2.0);

		double height = Math.max(1.0, bounds.getHeight() + IMAGE_MARGIN * 2.0);

		int imageWidth = Math.max(1, (int) Math.ceil(width));

		int imageHeight = Math.max(1, (int) Math.ceil(height));

		SnapshotParameters parameters = new SnapshotParameters();

		parameters.setFill(Color.WHITE);

		parameters.setViewport(new Rectangle2D(minX, minY, width, height));

		WritableImage image = new WritableImage(imageWidth, imageHeight);

		return renderedAutomaton.snapshot(parameters, image);
	}
}