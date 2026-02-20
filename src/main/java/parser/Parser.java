package parser;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.graphper.api.FileType;
import org.graphper.api.Graphviz;
import org.graphper.draw.ExecuteException;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import automaton.SRTA;

public class Parser {

	public enum Export {
		PNG, SVG, UPPAAL, DOT
	}

	public static void exportTo(String route, SRTA a, Export option) {

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
	public static void show(SRTA a) throws ExecuteException {
		var g = a.toDOTLayout();
		layoutInBrowser(g);
	}

	private static void layoutInBrowser(Graphviz g) {
		try {
			String svg = g.toSvgStr();
			SvgViewer.openSvgInBrowser(svg);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
