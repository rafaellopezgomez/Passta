package parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.graphper.api.FileType;
import org.graphper.api.Graphviz;
import org.graphper.draw.ExecuteException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;

import automaton.SRTA;
import trace.Trace;

public class Parser {
	
	private Parser() {}

	public enum Export {
		PNG, SVG, UPPAAL, DOT
	}

	public static void exportSRTA(String route, SRTA a, Export option) {

		switch (option) {
		case PNG:
			writeLayoutInFile(Paths.get(route), a.toDOTLayout(), FileType.PNG);
			break;
		case SVG:
			writeLayoutInFile(Paths.get(route), a.toDOTLayout(), FileType.SVG);
			break;
		case UPPAAL:
			new UPPAAL(route, a);
			break;
		default:
			throw new RuntimeException("Export option not defined");
		}
	}

	public static void writeTraces(String dst, List<Trace> traces) {
		if (dst == null || dst.isBlank())
			throw new RuntimeException("Destination path is null or undefined");

		if (traces == null || traces.isEmpty()) {
			throw new IllegalArgumentException("Traces list is null or empty");
		}

		Path input = Paths.get(dst);

		Path file = input.toString().toLowerCase().endsWith(".json") ? input : input.resolve("traces.json");

		if (!file.toString().toLowerCase().endsWith(".json")) {
			throw new IllegalArgumentException("Destination filename must have .json extension");
		}

		Path parent = file.getParent();

		if (parent != null) {
			try {
				Files.createDirectories(parent);
			} catch (IOException e) {
				throw new RuntimeException("Cannot create directories: " + parent, e);
			}
		}

		try {
			JsonSupport.prettyWriter().writeValue(file.toFile(), traces);
		} catch (IOException e) {
			throw new RuntimeException("Cannot write traces to: " + file, e);
		}

	}

	private static void writeLayoutInFile(Path target, Graphviz g, FileType ft) {

		if (target == null)
			throw new IllegalArgumentException("target is null");
		if (g == null)
			throw new IllegalArgumentException("Graphviz instance is null");
		if (ft == null)
			throw new IllegalArgumentException("FileType is null");

		final Path dir;
		final String baseName;

		if (Files.exists(target) && Files.isDirectory(target)) {
			dir = target;
			baseName = "automaton";
		} else {
			dir = (target.getParent() != null) ? target.getParent() : Paths.get(".");
			Path fn = target.getFileName();
			String name = (fn == null || fn.toString().isBlank()) ? "automaton" : fn.toString();
			baseName = stripExtension(name);
		}

		try {
			Files.createDirectories(dir);
			g.toFile(ft).save(dir.toString(), baseName);
		} catch (IOException | ExecuteException e) {
			throw new RuntimeException("Cannot write layout (target=" + target + ", dir=" + dir + ", baseName="
					+ baseName + ", type=" + ft + ")", e);
		}

	}

	private static String stripExtension(String name) {
		int dot = name.lastIndexOf('.');
		return (dot > 0) ? name.substring(0, dot) : name;
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
			e.printStackTrace();
		}
	}
}
