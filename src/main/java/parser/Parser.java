package parser;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.graphper.api.FileType;
import org.graphper.api.Graphviz;
import org.graphper.draw.ExecuteException;

import com.uppaal.model.core2.*;

import automaton.EDRTA;
import edge.EDRTAEdge;
import state.EDRTAState;

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
			toUppaal(route.toString(), a);
			break;
		default:
			throw new RuntimeException("Export option not defined");
		}

	}

	private static void toUppaal(String route, EDRTA a) {

		if (!a.hasProbs()) {
			throw new RuntimeException("First you need to compute probabilities");
		}

		Map<String, Integer> events = new HashMap<String, Integer>();
		int eventN = -1;

		Map<String, Integer> attrsList = new HashMap<String, Integer>();
		int attrCode = -1;
		// Add initial system attrs
		String inititalAttrs = a.getState(0).getAttrs().stream().sorted().collect(Collectors.joining(","));
		attrsList.put(inititalAttrs, attrCode);

		Map<Integer, Location> locations = new HashMap<Integer, Location>();

		Document doc = new Document(new DocumentPrototype());

		Template t = doc.createTemplate(); // new TA template with defaults
		doc.insert(t, null).setProperty("name", "Template"); // insert and set the name

		int x = 0;
		int y = 0;

		for (var state : a.getAllStates()) {
			/* Only create if absent */

			// Create locations and set their position
			ArrayList<EDRTAEdge> edges = state.getOutEdges().stream().map(eid -> a.getEdge(eid))
					.collect(Collectors.toCollection(ArrayList::new));
			Location sourceL = locations.get(state.getId());

			if (sourceL == null) {
				String name = "L" + String.valueOf(state.getId());
				sourceL = t.addLocation();
				sourceL.setProperty("name", name).setXY(x + 30, y - 10);
				sourceL.setXY(x, y);

				if (state.getId() == 0) {
					sourceL.setProperty("init", true);
				}

				if (edges.size() > 1) {
					sourceL.setProperty("committed", true);
				} else {
					String invariant = "x<="
							+ edges.stream().max(Comparator.comparing(EDRTAEdge::getMax)).get().getMax();
					sourceL.setProperty("invariant", invariant).setXY(x + 30, y + 10);
				}
				locations.putIfAbsent(state.getId(), sourceL);

			} else if (sourceL != null && edges.size() == 1
					&& sourceL.getPropertyValue("invariant").toString().isBlank()) {
				String invariant = "x<=" + edges.stream().max(Comparator.comparing(EDRTAEdge::getMax)).get().getMax();
				sourceL.setProperty("invariant", invariant).setXY(sourceL.getX() + 30, sourceL.getY() + 10);
			}

			x = sourceL.getX();
			y = sourceL.getY();

			if (edges.size() == 1) {

				EDRTAEdge edge = edges.get(0);
				String guard = "x>=" + edge.getMin().toString();
				String event = edge.getEvent();
				if (events.get(event) == null) {
					eventN += 1;
					events.put(event, eventN);
				}

				// Add edges
				try {
					///////////////////////////////// New
					Location targetL = locations.get(edge.getTargetId());
					EDRTAState targetS = a.getState(edge.getTargetId());
					String attrsName = targetS.getAttrs().stream().sorted().collect(Collectors.joining(","));
					if (attrsList.get(attrsName) == null) {
						attrCode += 1;
						attrsList.put(attrsName, attrCode);
					}

					String updateAttrs = "x=0, " + "attrs = " + attrsList.get(attrsName);
					String eventArr = " event = " + events.get(event);

					if (targetL == null) {
						String name = "L" + String.valueOf(targetS.getId());
						targetL = t.addLocation();
						if (targetS.getOutEdges().size() > 1) {
							y += 150;
						} else {
							y += 300;
							targetL.setProperty("name", name).setXY(x + 30, y - 10);
						}
						targetL.setXY(x, y);
						locations.putIfAbsent(targetS.getId(), targetL);
					}
					/////////////////////////////
					int auxX = (sourceL.getX() + targetL.getX()) / 2;
					int auxY = (sourceL.getY() + targetL.getY()) / 2;

					if (targetS.getOutEdges().size() == 1) { // An auxiliary committed location is required
						Location commL = t.addLocation();

						commL.setProperty("committed", true);
						commL.setXY(auxX, auxY);
						Edge e = t.addEdge(sourceL, commL);
						e.setProperty("guard", guard).setXY((sourceL.getX() + commL.getX()) / 2 + 30,
								(sourceL.getY() + commL.getY()) / 2);
						e.setProperty("assignment", eventArr).setXY((sourceL.getX() + commL.getX()) / 2 + 30,
								((sourceL.getY() + commL.getY()) / 2) + 30);
						e = t.addEdge(commL, targetL);
						e.setProperty("assignment", updateAttrs).setXY((commL.getX() + targetL.getX()) / 2 + 30,
								((commL.getY() + targetL.getY()) / 2));
					} else { // The target location is committed already
						Edge e = t.addEdge(sourceL, targetL);
						e.setProperty("guard", guard).setXY((sourceL.getX() + targetL.getX()) / 2 + 30,
								(sourceL.getY() + targetL.getY()) / 2);
						e.setProperty("assignment", eventArr).setXY((sourceL.getX() + targetL.getX()) / 2 + 30,
								((sourceL.getY() + targetL.getY()) / 2) + 30);
					}
				} catch (ModelException e) {
					e.printStackTrace();
				}
			} else if (edges.size() > 1) {
				if (sourceL.getPropertyValue("committed").toString() == "false") {
					// Committed location that ends up in a branchpoint
					sourceL.setProperty("committed", true);
				}

				String attrsName = state.getAttrs().stream().sorted().collect(Collectors.joining(","));
				if (attrsList.get(attrsName) == null) {
					attrCode += 1;
					attrsList.put(attrsName, attrCode);
				}

				String updateAttrs = "x=0, " + "attrs = " + attrsList.get(attrsName);

				x = sourceL.getX();
				y = sourceL.getY();

				BranchPoint bp = t.addBranchPoint();
				x += 150;
				y += 150;

				bp.setXY(x, y);

				try {
					Edge e = t.addEdge(sourceL, bp);
					e.setProperty("assignment", updateAttrs).setXY((sourceL.getX() + bp.getX()) / 2 + 30,
							((sourceL.getY() + bp.getY()) / 2));/////////////////////////////////////////////////
				} catch (ModelException e) {
					e.printStackTrace();
				}

				y += 150;
				int branch = 1;
				// Create outgoing branches
				int currentX = x;
				int currentY = y;
				for (int edgeId : state.getOutEdges()) {
					EDRTAEdge edge = a.getEdge(edgeId);
					String event = edge.getEvent();
					String invariant = "x<=" + edge.getMax();

					if (events.get(event) == null) {
						eventN += 1;
						events.put(event, eventN);
					}
					String update = "x=0," + " event = " + events.get(event);
					String guard = "x>=" + edge.getMin().toString();

					// Add branch location
					String name = "L" + String.valueOf(state.getId());
					name += "_" + branch;
					Location sourceBL = t.addLocation();
					sourceBL.setProperty("name", name).setXY(currentX + 30, currentY - 10);
					sourceBL.setProperty("invariant", invariant).setXY(currentX + 30, currentY + 10);
					sourceBL.setXY(currentX, currentY);

					try {
						String prob = new DecimalFormat("#########.####", new DecimalFormatSymbols(Locale.ENGLISH))
								.format(edge.getProb());
						t.addEdge(bp, sourceBL).setProperty("probability", prob)
								.setXY((bp.getX() + sourceBL.getX()) / 2 + 30, (bp.getY() + sourceBL.getY()) / 2);
					} catch (ModelException e) {
						e.printStackTrace();
					}

					Location targetL = locations.get(edge.getTargetId());
					EDRTAState targetS = a.getState(edge.getTargetId());
					/////////
					if (targetL == null) {
						name = "L" + String.valueOf(targetS.getId());
						targetL = t.addLocation();
						if (targetS.getOutEdges().size() > 1) {
							currentY += 150;
						} else {
							currentY += 300;

							targetL.setProperty("name", name).setXY(currentX + 30, currentY - 10);
						}
						targetL.setXY(currentX, currentY);
						locations.putIfAbsent(targetS.getId(), targetL);
					}

					attrsName = targetS.getAttrs().stream().sorted().collect(Collectors.joining(","));
					if (attrsList.get(attrsName) == null) {
						attrCode += 1;
						attrsList.put(attrsName, attrCode);
					}

					updateAttrs = "x=0, " + "attrs = " + attrsList.get(attrsName);
					String eventArr = " event = " + events.get(event);

					//////////////////////// New
					int auxX = (sourceBL.getX() + targetL.getX()) / 2;
					int auxY = (sourceBL.getY() + targetL.getY()) / 2;
					if (targetS.getOutEdges().size() == 1) { // An auxiliary committed location is required
						try {
							Location commL = t.addLocation();

							commL.setProperty("committed", true);
							commL.setXY(auxX, auxY);
							Edge e = t.addEdge(sourceBL, commL);
							e.setProperty("guard", guard).setXY((sourceBL.getX() + commL.getX()) / 2 + 30,
									(sourceBL.getY() + commL.getY()) / 2);
							e.setProperty("assignment", eventArr).setXY((sourceBL.getX() + commL.getX()) / 2 + 30,
									((sourceBL.getY() + commL.getY()) / 2) + 30);
							e = t.addEdge(commL, targetL);
							e.setProperty("assignment", updateAttrs).setXY((commL.getX() + targetL.getX()) / 2 + 30,
									((commL.getY() + targetL.getY()) / 2));
						} catch (ModelException e) {
							e.printStackTrace();
						}
					} else { // The target location is committed already
						try {
							Edge e = t.addEdge(sourceBL, targetL);
							e.setProperty("guard", guard).setXY((sourceBL.getX() + targetL.getX()) / 2 + 30,
									(sourceBL.getY() + targetL.getY()) / 2);
							e.setProperty("assignment", eventArr).setXY((sourceBL.getX() + targetL.getX()) / 2 + 30,
									((sourceBL.getY() + targetL.getY()) / 2) + 30);
						} catch (ModelException e) {

							e.printStackTrace();
						}
					}
					////////////////////////
					currentY += 150;
					currentX += 300;
					branch++;
				}
				/////////////////// Branch
			}
			y += 150;
		}

		String attrsMeaning = "";
		for (var kv : attrsList.entrySet()) {
			attrsMeaning += "System attributes " + kv.getKey() + "---> code: " + kv.getValue() + "\n";
		}

		String attrsInfo = "/*\n" + attrsMeaning + "*/\n";

		String eventsMeaning = "No event ---> code: -1 \n";
		for (var kv : events.entrySet()) {
			eventsMeaning += "Event " + kv.getKey() + "---> code: " + kv.getValue() + "\n";
		}

		String eventsInfo = "/*\n" + eventsMeaning + "*/\n";

		doc.setProperty("declaration", "hybrid clock x;\nint attrs = " + attrsList.get(inititalAttrs) + ";\n"
				+ attrsInfo + "\nint event = -1;\n" + eventsInfo); // shared global hybrid clock x
		doc.setProperty("system", "Process = Template();\n" + "system Process;");

		try {
			String pathString = Paths.get(route).getParent() == null ? "" : Paths.get(route).getParent().toString();
			Path path = Paths.get(route).getParent();
			if (path != null && Files.notExists(Paths.get(pathString))) {
				Files.createDirectories(path);
			}
			String filename = Paths.get(route).getFileName().toString();
			filename = filename.endsWith(".xml") ? filename : filename + ".xml";
			doc.save(Paths.get(pathString, filename).toString());
		} catch (IOException err) {
			err.printStackTrace(System.err);
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
