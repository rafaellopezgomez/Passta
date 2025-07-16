package parser;

import java.awt.Desktop;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.graphper.api.FileType;
import org.graphper.api.Graphviz;
import org.graphper.draw.ExecuteException;

import automaton.EDRTA;

public class Parser {

	public enum Export {
		PNG, SVG, UPPAAL, DOT
	}

	public static void exportTo(String route, EDRTA a, Export option) {

		switch (option) {
		case PNG:
			writeLayoutInFile(Paths.get(route.toString()), a.toDOTLayout(), FileType.PNG);
			break;
		case SVG:
			writeLayoutInFile(Paths.get(route.toString()), a.toDOTLayout(), FileType.SVG);
			break;
		case UPPAAL:
			new UPPAAL(route, a);
			break;
		default:
			throw new RuntimeException("Export option not defined");
		}

	}

	private static void writeLayoutInFile(Path path, Graphviz g, FileType ft) {
		try {
			if (path != null && Files.notExists(path.getParent())) {
				Files.createDirectories(path.getParent());
			}
			String filename = path.getFileName().toString() == null ? "automaton" : path.getFileName().toString();
			g.toFile(ft).save(path.getParent().toString(), filename);
		} catch (IOException | ExecuteException e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Method that displays the automata representation in a browser
	 * 
	 * @throws ExecuteException
	 */
	public static void show(EDRTA a) throws ExecuteException {
		var g = a.toDOTLayout();
		layoutInBrowser(g);
	}

	private static void layoutInBrowser(Graphviz g) {
		try {
			Path tempFilePathSVG = Files.createTempFile("automaton", null);
			g.toFile(FileType.SVG).save(tempFilePathSVG.getParent().toString(),
					tempFilePathSVG.getFileName().toString());

			// File tempFileSVG = tempFilePathSVG.toFile();

			System.setProperty("java.awt.headless", "false");

			String html = "<!DOCTYPE html>\n" + "<html>\n" + "    <body>\n" + "     	<object data=\"file:\\\\"
					+ tempFilePathSVG.toString() + ".svg" + "\" type=\"image/svg+xml\"></object>\n" + "    </body>\n"
					+ "</html>\n";
			Path file = Files.createTempFile("visualization", ".html");
			try {
				Files.write(file, html.getBytes());
				Desktop.getDesktop().browse(file.toUri());
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (IOException | ExecuteException e) {
			e.printStackTrace();
		}
	}
}
