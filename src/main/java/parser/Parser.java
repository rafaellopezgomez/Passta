package parser;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
		String inititalAttrs = a.getState(0).getAttrs().stream().sorted().collect(Collectors.joining (","));
		attrsList.put(inititalAttrs, attrCode);

		Map<Integer, Location> locations = new HashMap<Integer, Location>();

		Document doc = new Document(new DocumentPrototype());

		Template t = doc.createTemplate(); // new TA template with defaults
		doc.insert(t, null).setProperty("name", "Template"); // insert and set the name
		
		int y = 0;

		for (var state : a.getAllStates()) {
			/* Only create if absent */
			
			// Create locations and set their position
			int x = 0;
			// Create the outcoming edges
			ArrayList<EDRTAEdge> edges = state.getOutEdges().stream().map(eid -> a.getEdge(eid))
					.collect(Collectors.toCollection(ArrayList::new));
			Location sourceLocation = locations.get(state.getId());

			if (sourceLocation == null) {
				String name = "L" + String.valueOf(state.getId());
				sourceLocation = t.addLocation();
				sourceLocation.setProperty("name", name).setXY(x + 30, y - 10);
				sourceLocation.setXY(x, y);
				
				if(state.getId() == 0) {
					sourceLocation.setProperty("init", true);
				}
				
				if (edges.size() > 1) {
					sourceLocation.setProperty("committed", true);
				} else {
					String invariant = "x<=" + edges.stream().max(Comparator.comparing(EDRTAEdge::getMax)).get().getMax();
					sourceLocation.setProperty("invariant", invariant).setXY(x + 30, y + 10);
				}
				locations.putIfAbsent(state.getId(), sourceLocation);
				
			} else if (sourceLocation != null && edges.size() == 1 && sourceLocation.getPropertyValue("invariant").toString().isBlank()) {
				String invariant = "x<=" + edges.stream().max(Comparator.comparing(EDRTAEdge::getMax)).get().getMax();
				sourceLocation.setProperty("invariant", invariant).setXY(sourceLocation.getX() + 30, sourceLocation.getY() + 10);
			}
			if (edges.size() == 1) {
				
				EDRTAEdge edge = edges.get(0);
				String guard = "x>=" + edge.getMin().toString();
				String event = edge.getEvent();
				if (events.get(event) == null) {
					eventN += 1;
					events.put(event, eventN);
				}
				
				Location targetLocation = locations.get(edge.getTargetId());
				EDRTAState targetState = a.getState(edge.getTargetId());
				if (targetLocation == null) {
					y += 150;
					String name = "L" + String.valueOf(targetState.getId());
					targetLocation = t.addLocation();
					targetLocation.setProperty("name", name).setXY(x + 30, y - 10);
					targetLocation.setXY(x, y);
					locations.putIfAbsent(targetState.getId(), targetLocation);
				}
				
//	New			
				String attrsName = targetState.getAttrs().stream().sorted().collect(Collectors.joining (","));
				//attrsName = attrsName.isBlank() ? "Empty" : attrsName;
				if (attrsList.get(attrsName) == null) {
					attrCode += 1;
					attrsList.put(attrsName, attrCode);
				}
				
	//
				
				try {
					String update = "x=0," + " event = "+ events.get(event) + ", attrs = " + attrsList.get(attrsName); // New
					Edge e = t.addEdge(sourceLocation, targetLocation);
					e.setProperty("guard", guard).setXY((sourceLocation.getX() + targetLocation.getX()) / 2 + 30, (sourceLocation.getY() + targetLocation.getY()) / 2);
					e.setProperty("assignment", update).setXY((sourceLocation.getX() + targetLocation.getX()) / 2 + 30, ((sourceLocation.getY() + targetLocation.getY()) / 2) + 30);
				} catch (ModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (edges.size() > 1) {
				if (sourceLocation.getPropertyValue("committed").toString() == "false") {
					// Quiere decir que se ha creado como target de un edge y necesita ser cambiado
					// a committed y crear cada rama con sus probabilidad
					sourceLocation.setProperty("committed", true);
					
					BranchPoint bp = t.addBranchPoint();
					x += 150;
					y += 150;
					bp.setXY(x, y);
					
					
					try {
						Edge e = t.addEdge(sourceLocation, bp);
					} catch (ModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					y += 150;
					int branch = 1;
					for (int edgeId : state.getOutEdges()) {
						EDRTAEdge edge = a.getEdge(edgeId);
						String event = edge.getEvent();
						String invariant = "x<=" + edge.getMax();

						if (events.get(event) == null) {
							eventN += 1;
							events.put(event, eventN);
						}
						String update = "x=0," + " event = "+ events.get(event);
						String guard = "x>=" + edge.getMin().toString();
						
						// Add branch location
						String name = "L" + String.valueOf(state.getId());
						name += "_" + branch;
						Location sourceBranchLocation = t.addLocation();
						sourceBranchLocation.setProperty("name", name).setXY(x + 30, y - 10);
						sourceBranchLocation.setProperty("invariant", invariant).setXY(x + 30, y + 10);
						sourceBranchLocation.setXY(x, y);
						
						try {
							String prob = edge.getProb().toString();
							t.addEdge(bp, sourceBranchLocation).setProperty("probability", prob).setXY((bp.getX() + sourceBranchLocation.getX()) / 2 + 30, (bp.getY() + sourceBranchLocation.getY()) / 2);
						} catch (ModelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						Location targetLocation = locations.get(edge.getTargetId());
						EDRTAState targetState = a.getState(edge.getTargetId());
						if (targetLocation == null) {
							name = "L" + String.valueOf(targetState.getId());
							targetLocation = t.addLocation();
							targetLocation.setProperty("name", name).setXY(x + 30, (y + 150) - 10);
							targetLocation.setXY(x, y + 150);
							locations.putIfAbsent(targetState.getId(), targetLocation);
						}

//						New		
						String attrsName = targetState.getAttrs().stream().sorted().collect(Collectors.joining (","));
						//attrsName = attrsName.isBlank() ? "Empty" : attrsName;
						if (attrsList.get(attrsName) == null) {
							attrCode += 1;
							attrsList.put(attrsName, attrCode);
						}
						
			//
						
						try {
							update = "x=0," + " event = "+ events.get(event) + ", attrs = " + attrsList.get(attrsName); // New
							Edge e = t.addEdge(sourceBranchLocation, targetLocation);
							e.setProperty("guard", guard).setXY((sourceBranchLocation.getX() + targetLocation.getX()) / 2 + 30, (sourceBranchLocation.getY() + targetLocation.getY()) / 2);
							e.setProperty("assignment", update).setXY((sourceBranchLocation.getX() + targetLocation.getX()) / 2 + 30, ((sourceBranchLocation.getY() + targetLocation.getY()) / 2) + 30);
						} catch (ModelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						y += 150;
						x += 300;
						branch++;
					}
				}
			}

			y += 150;
		}
		
		String attrsMeaning = "";	
		for(var kv : attrsList.entrySet()) {
			attrsMeaning += "System attributes " + kv.getKey() + "---> code: " + kv.getValue() + "\n";
		}
		
		String attrsInfo = "/*\n" + attrsMeaning + "*/\n";
		
		String eventsMeaning = "No event ---> code: -1 \n";
		for(var kv : events.entrySet()) {
			eventsMeaning += "Event " + kv.getKey() + "---> code: " + kv.getValue() + "\n";
		}
		
		String eventsInfo = "/*\n" + eventsMeaning + "*/\n";
		
		doc.setProperty("declaration","hybrid clock x;\nint attrs = " + attrsList.get(inititalAttrs) + ";\n" + attrsInfo + "\nint event = -1;\n" + eventsInfo); // shared global hybrid clock x
		doc.setProperty("system", "Process = Template();\n" + "system Process;");

		try {
			String pathString = Paths.get(route).getParent() == null ? "" : Paths.get(route).getParent().toString();
			Path path = Paths.get(route).getParent();
			if(path != null && Files.notExists(Paths.get(pathString))) {
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
			if(path != null && Files.notExists(path.getParent())) {
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
