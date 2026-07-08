package es.uma.morse.passta.io;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.core.automaton.SRTAEdge;
import es.uma.morse.passta.core.automaton.SRTALocation;

public class UPPAAL {
	private DocumentBuilderFactory factory;
	private DocumentBuilder builder;
	private Document document;
	private Element nta;
	private Element declaration;
	private Element template;
	private Element system;
	private Element queries;
	Path outputPath;
	SRTA a;

	private int uppaalId = 0;

	private class Location {

		private String id;
		private int x;
		private int y;
		private String name = "";
		private int nameX;
		private int nameY;
		private boolean committed = false;
		private String invariant = "";
		private int invX;
		private int invY;
		private int attrCode = -1;

		public Location() {
			id = "id" + uppaalId;
			uppaalId++;
		}

		public Location(int x, int y, String name, int nameX, int nameY) {
			id = "id" + uppaalId;
			uppaalId++;
			this.name = name;
			this.x = x;
			this.y = y;
			this.nameX = nameX;
			this.nameY = nameY;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getEnclosingInstance().hashCode();
			result = prime * result + Objects.hash(id);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Location other = (Location) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
				return false;
			return Objects.equals(id, other.id);
		}

		public void setXY(int x, int y) {
			this.x = x;
			this.y = y;

		}

		public void setName(String name, int nameX, int nameY) {
			this.name = name;
			this.nameX = nameX;
			this.nameY = nameY;
		}

		public void setCommitted(boolean commited) {
			this.committed = commited;
		}

		public void setInvariant(String invariant, int invX, int invY) {
			this.invariant = invariant;
			this.invX = invX;
			this.invY = invY;
		}

		public String getId() {
			return id;
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		public String getName() {
			return name;
		}

		public int getNameX() {
			return nameX;
		}

		public int getNameY() {
			return nameY;
		}

		public boolean isCommitted() {
			return committed;
		}

		public String getInvariant() {
			return invariant;
		}

		public int getInvX() {
			return invX;
		}

		public int getInvY() {
			return invY;
		}

		private UPPAAL getEnclosingInstance() {
			return UPPAAL.this;
		}
		
		public int getAttrCode() {
			return attrCode;
		}
		
		public void setAttrCode(int attrCode) {
			this.attrCode = attrCode;
		}

	}

	private class Edge {
		private String id;
		private String source;
		private String target;

		private String assignment = "";
		private int assigX;
		private int assigY;

		public int getAssigX() {
			return assigX;
		}

		public int getAssigY() {
			return assigY;
		}

		public int getGuardX() {
			return guardX;
		}

		public int getGuardY() {
			return guardY;
		}

		public int getProbX() {
			return probX;
		}

		public int getProbY() {
			return probY;
		}

		private String guard = "";
		private int guardX;
		private int guardY;

		private String prob = "";
		private int probX;
		private int probY;

		public Edge(String source, String target) {
			id = "id" + uppaalId;
			uppaalId++;
			this.source = source;
			this.target = target;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getEnclosingInstance().hashCode();
			result = prime * result + Objects.hash(id);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Edge other = (Edge) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
				return false;
			return Objects.equals(id, other.id);
		}

		public String getId() {
			return id;
		}

		public String getSource() {
			return source;
		}

		public String getTarget() {
			return target;
		}

		public String getAssignment() {
			return assignment;
		}

		public String getGuard() {
			return guard;
		}

		public String getProb() {
			return prob;
		}

		public void setAssignment(String assignment, int assigX, int assigY) {
			this.assignment = assignment;
			this.assigX = assigX;
			this.assigY = assigY;
		}

		public void setGuard(String guard, int guardX, int guardY) {
			this.guard = guard;
			this.guardX = guardX;
			this.guardY = guardY;
		}

		public void setProb(String prob, int probX, int probY) {
			this.prob = prob;
			this.probX = probX;
			this.probY = probY;
		}

		private UPPAAL getEnclosingInstance() {
			return UPPAAL.this;
		}

	}

	private class Branchpoint {
		private String id;
		private int x;
		private int y;

		public Branchpoint(int x, int y) {
			id = "id" + uppaalId;
			uppaalId++;
			this.x = x;
			this.y = y;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getEnclosingInstance().hashCode();
			result = prime * result + Objects.hash(id);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Branchpoint other = (Branchpoint) obj;
			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
				return false;
			return Objects.equals(id, other.id);
		}

		public String getId() {
			return id;
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		private UPPAAL getEnclosingInstance() {
			return UPPAAL.this;
		}

	}

	public UPPAAL(Path outputPath, SRTA a) {
		this.outputPath = outputPath;
		this.a = a;
		initialize();
		toUppaal();
		save();
	}

	private void initialize() {
		try {
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			document = builder.newDocument();
			nta = document.createElement("nta");
			document.appendChild(nta);
			declaration = document.createElement("declaration");
			nta.appendChild(declaration);
			template = document.createElement("template");
			Element name = document.createElement("name");
			name.appendChild(document.createTextNode("Template"));
			template.appendChild(name);
			nta.appendChild(template);
			system = document.createElement("system");
			nta.appendChild(system);
			queries = document.createElement("queries");
			nta.appendChild(queries);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	private void addLocation(Location loc) {
		Element location = document.createElement("location");
		location.setAttribute("id", loc.getId());
		location.setAttribute("x", String.valueOf(loc.getX()));
		location.setAttribute("y", String.valueOf(loc.getY()));
		if (!loc.getName().isBlank()) {
			Element name = document.createElement("name");
			name.setAttribute("x", String.valueOf(loc.getNameX()));
			name.setAttribute("y", String.valueOf(loc.getNameY()));
			name.appendChild(document.createTextNode(loc.getName()));
			location.appendChild(name);
		}

		if (!loc.getInvariant().isBlank()) {
			Element label = document.createElement("label");
			label.setAttribute("kind", "invariant");
			label.setAttribute("x", String.valueOf(loc.getInvX()));
			label.setAttribute("y", String.valueOf(loc.getInvY()));
			label.appendChild(document.createTextNode(loc.getInvariant()));
			location.appendChild(label);
		}

		if (loc.isCommitted()) {
			Element committed = document.createElement("committed");
			location.appendChild(committed);
		}

		template.appendChild(location);
	}

	private void addBranchpoint(Branchpoint b) {
		Element branchpoint = document.createElement("branchpoint");
		branchpoint.setAttribute("id", b.getId());
		branchpoint.setAttribute("x", String.valueOf(b.getX()));
		branchpoint.setAttribute("y", String.valueOf(b.getY()));

		template.appendChild(branchpoint);
	}

	private void addEdge(Edge e) {
		Element edge = document.createElement("transition");
		edge.setAttribute("id", e.getId());

		Element source = document.createElement("source");
		source.setAttribute("ref", e.getSource());
		edge.appendChild(source);
		Element target = document.createElement("target");
		target.setAttribute("ref", e.getTarget());
		edge.appendChild(target);

		if (!e.getGuard().isBlank()) {
			Element label = document.createElement("label");
			label.setAttribute("kind", "guard");
			label.setAttribute("x", String.valueOf(e.getGuardX()));
			label.setAttribute("y", String.valueOf(e.getGuardY()));
			label.appendChild(document.createTextNode(e.getGuard()));
			edge.appendChild(label);
		}

		if (!e.getAssignment().isBlank()) {
			Element label = document.createElement("label");
			label.setAttribute("kind", "assignment");
			label.setAttribute("x", String.valueOf(e.getAssigX()));
			label.setAttribute("y", String.valueOf(e.getAssigY()));
			label.appendChild(document.createTextNode(e.getAssignment()));
			edge.appendChild(label);
		}

		if (!e.getProb().isBlank()) {
			Element label = document.createElement("label");
			label.setAttribute("kind", "probability");
			label.setAttribute("x", String.valueOf(e.getProbX()));
			label.setAttribute("y", String.valueOf(e.getProbY()));
			label.appendChild(document.createTextNode(e.getProb()));
			edge.appendChild(label);
		}

		template.appendChild(edge);
	}

	private void toUppaal() {

		Map<String, Integer> events = new HashMap<String, Integer>();
		int eventN = -1;
		
		Map<String, Integer> attrsList = new HashMap<>();

		String inititalAttrs = attrsKey(a.getLocation(0));
		attrsList.put(inititalAttrs, 0);

		Map<Integer, Location> locationMapping = new HashMap<Integer, Location>(); // Map that stores main locations (mapped
																				// to a SRTA locations)
		Set<Location> locationSet = new HashSet<>();
		Set<Edge> edgeSet = new HashSet<>();
		Set<Branchpoint> bpSet = new HashSet<>();

		int x = 0;
		int y = 0;

		for (var location : a.getAllLocations()) {
		    String sourceAttrsName = attrsKey(location);
		    int sourceAttrCode = getOrCreateAttrCode(attrsList, sourceAttrsName);

		    ArrayList<SRTAEdge> edges = location.getOutEdges().stream()
		            .map(eid -> a.getEdge(eid))
		            .collect(Collectors.toCollection(ArrayList::new));

		    Location sourceL = locationMapping.get(location.getId());
		    String invariant = "x<=" + formatNumber(location.getInvariant());

			if (sourceL == null) { // First location to be created (not has to be initial)
				String name = "L" + String.valueOf(location.getId());
				sourceL = new Location(x, y, name, x + 30, y - 10);

				if (edges.size() > 1) {
				    sourceL.setCommitted(true);
				    sourceL.setName("", x + 30, y - 10);
				} else {
				    sourceL.setInvariant(invariant, x + 30, y + 10);
				}
				locationMapping.putIfAbsent(location.getId(), sourceL);
				locationSet.add(sourceL);

			} else if (edges.size() == 1 && sourceL.getInvariant().isBlank()) { // If there is only 1 outgoing
																				// transition and there is not
				sourceL.setInvariant(invariant, sourceL.getX() + 30, sourceL.getY() + 10);
			}
			sourceL.setAttrCode(sourceAttrCode);
			x = sourceL.getX();
			y = sourceL.getY();

			// Add outgoing transitions of the location
			if (edges.size() == 1) { // If there is only one outgoing transition, the transformation is source
										// location -> committed location -> target location

				SRTAEdge edge = edges.get(0);
				String guard = "x>=" + formatNumber(edge.getMin());
				String event = edge.getEvent();
				if (events.get(event) == null) {
					eventN += 1;
					events.put(event, eventN);
				}

				Location targetL = locationMapping.get(edge.getTargetId());
				SRTALocation targetS = a.getLocation(edge.getTargetId());

				String attrsName = attrsKey(targetS);
				int targetAttrCode = getOrCreateAttrCode(attrsList, attrsName);

				String updateAttrs = "x=0, attrs = " + targetAttrCode;
				
				String eventArr = "event = " + events.get(event);

				if (targetL == null) { // If this target location is not created yet, it is created here
					String name = "L" + String.valueOf(targetS.getId());
					targetL = new Location();
					
					if (targetS.getOutEdges().size() > 1) { // If there is more than one outgoing transition, it is
						// converted to committed the name is not set
						y += 150;
						targetL.setCommitted(true);
						targetL.setXY(x, y);
					} else { // If the target location has only one outgoing transition
						y += 300;
						targetL.setName(name, x + 30, y - 10);
					}
					targetL.setXY(x, y);
					locationMapping.putIfAbsent(targetS.getId(), targetL);
					locationSet.add(targetL);
				}
				targetL.setAttrCode(targetAttrCode);
				
				if (targetS.getOutEdges().size() > 1) {  // The target location is committed (more than 1 outgoing edge)
					targetL.setCommitted(true);
					Edge e = new Edge(sourceL.getId(), targetL.getId());
					e.setGuard(guard, (sourceL.getX() + targetL.getX()) / 2 + 30,
							(sourceL.getY() + targetL.getY()) / 2);
					e.setAssignment(eventArr, (sourceL.getX() + targetL.getX()) / 2 + 30,
							((sourceL.getY() + targetL.getY()) / 2) + 30);
					
					edgeSet.add(e);
				} else { // If the target location has only one outgoing transition
					
					int auxX = (sourceL.getX() + targetL.getX()) / 2;
					int auxY = (sourceL.getY() + targetL.getY()) / 2;
					
					Location commL = new Location(); // An auxiliary location is required
					commL.setCommitted(true);
					commL.setXY(auxX, auxY);
					locationSet.add(commL);

					Edge e = new Edge(sourceL.getId(), commL.getId());
					e.setGuard(guard, (sourceL.getX() + commL.getX()) / 2 + 30, (sourceL.getY() + commL.getY()) / 2);
					e.setAssignment(eventArr, (sourceL.getX() + commL.getX()) / 2 + 30,
							((sourceL.getY() + commL.getY()) / 2) + 30);
					edgeSet.add(e);

					e = new Edge(commL.getId(), targetL.getId());
					e.setAssignment(updateAttrs, (commL.getX() + targetL.getX()) / 2 + 30,
							((commL.getY() + targetL.getY()) / 2));
					edgeSet.add(e);
				}

			} else if (edges.size() > 1) { // If there are many outgoing transitions

				String attrsName = attrsKey(location);

				String updateAttrs = "x=0, attrs = " + sourceAttrCode;

				x = sourceL.getX();
				y = sourceL.getY();

				x += 150;
				y += 150;

				Branchpoint bp = new Branchpoint(x, y);
				bpSet.add(bp);

				Edge e = new Edge(sourceL.getId(), bp.getId());
				e.setAssignment(updateAttrs, (sourceL.getX() + bp.getX()) / 2 + 30, ((sourceL.getY() + bp.getY()) / 2));
				edgeSet.add(e);

				y += 150;
				int branch = 1;
				// Create outgoing branches
				int currentX = x;
				int currentY = y;

				for (int edgeId : location.getOutEdges()) {

					SRTAEdge edge = a.getEdge(edgeId);
					String event = edge.getEvent();
					invariant = "x<=" + formatNumber(edge.getMax());

					if (events.get(event) == null) {
						eventN += 1;
						events.put(event, eventN);
					}

					String guard = "x>=" + formatNumber(edge.getMin());

					// Add branch location
					String name = "L" + String.valueOf(location.getId());
					name += "_" + branch;
					Location sourceBL = new Location(currentX, currentY, name, currentX + 30, currentY - 10);
					sourceBL.setInvariant(invariant, currentX + 30, currentY + 10);
					sourceBL.setAttrCode(sourceAttrCode);
					locationSet.add(sourceBL);

					String prob = formatNumber(edge.getProb());
					Edge eAux = new Edge(bp.getId(), sourceBL.getId());
					eAux.setProb(prob, (bp.getX() + sourceBL.getX()) / 2 + 30, (bp.getY() + sourceBL.getY()) / 2);
					edgeSet.add(eAux);

					Location targetL = locationMapping.get(edge.getTargetId());
					SRTALocation targetS = a.getLocation(edge.getTargetId());

					if (targetL == null) {
						name = "L" + String.valueOf(targetS.getId());
						targetL = new Location();
						if (targetS.getOutEdges().size() > 1) {
							currentY += 150;
							targetL.setCommitted(true);
						} else {
							currentY += 300;
							targetL.setName(name, currentX + 30, currentY - 10);
						}
						targetL.setXY(currentX, currentY);
						locationMapping.putIfAbsent(targetS.getId(), targetL);
						locationSet.add(targetL);
					}

					attrsName = attrsKey(targetS);
					int targetAttrCode = getOrCreateAttrCode(attrsList, attrsName);

					targetL.setAttrCode(targetAttrCode);

					updateAttrs = "x=0, attrs = " + targetAttrCode;
					
					String eventArr = " event = " + events.get(event);

					int auxX = (sourceBL.getX() + targetL.getX()) / 2;
					int auxY = (sourceBL.getY() + targetL.getY()) / 2;
					if (targetS.getOutEdges().size() == 1) { // An auxiliary committed location is required
						Location commL = new Location();
						commL.setCommitted(true);
						commL.setXY(auxX, auxY);
						locationSet.add(commL);

						eAux = new Edge(sourceBL.getId(), commL.getId());
						eAux.setGuard(guard, (sourceBL.getX() + commL.getX()) / 2 + 30,
								(sourceBL.getY() + commL.getY()) / 2);
						eAux.setAssignment(eventArr, (sourceBL.getX() + commL.getX()) / 2 + 30,
								((sourceBL.getY() + commL.getY()) / 2) + 30);
						edgeSet.add(eAux);

						eAux = new Edge(commL.getId(), targetL.getId());
						eAux.setAssignment(updateAttrs, (commL.getX() + targetL.getX()) / 2 + 30,
								((commL.getY() + targetL.getY()) / 2));
						edgeSet.add(eAux);
						targetL.setAttrCode(targetAttrCode);
					} else { // The target location is committed already
						e = new Edge(sourceBL.getId(), targetL.getId());
						e.setGuard(guard, (sourceBL.getX() + targetL.getX()) / 2 + 30,
								(sourceBL.getY() + targetL.getY()) / 2);
						e.setAssignment(eventArr, (sourceBL.getX() + targetL.getX()) / 2 + 30,
								((sourceBL.getY() + targetL.getY()) / 2) + 30);
						edgeSet.add(e);
					}
					currentY += 150;
					currentX += 300;
					branch++;
				}
			}

			y += 150;
		}
		
		StringBuilder attrsMeaning = new StringBuilder();

		for (var kv : attrsList.entrySet()) {
		    attrsMeaning.append("System attributes ")
		            .append(kv.getKey())
		            .append(" ---> code: ")
		            .append(kv.getValue())
		            .append("\n");
		}

		String attrsInfo = "/*\n" + attrsMeaning + "*/\n";
		
		StringBuilder eventsMeaning = new StringBuilder();
		
		eventsMeaning.append("No event ---> code: -1 \n");

		for (var kv : events.entrySet()) {
			eventsMeaning.append("Event ")
		            .append(kv.getKey())
		            .append(" ---> code: ")
		            .append(kv.getValue())
		            .append("\n");
		}

		String eventsInfo = "/*\n" + eventsMeaning + "*/\n";
		
		String locationsInfo = "/*\n"
		        + computeLocationsLegend(locationSet, attrsList)
		        + "*/\n";

		String declaration = "\nhybrid clock x;\n"
		        + "int attrs = " + attrsList.get(inititalAttrs) + ";\n"
		        + attrsInfo
		        + "\nint event = -1;\n"
		        + eventsInfo
		        + "\n"
		        + locationsInfo;

		addDeclaration(declaration);

		locationSet.forEach(this::addLocation);
		bpSet.forEach(this::addBranchpoint);
		Element init = document.createElement("init");
		init.setAttribute("ref", locationMapping.get(0).getId());
		template.appendChild(init);
		edgeSet.forEach(this::addEdge);

		String system = "\nProcess = Template();\n" + "system Process;\n";

		addSystem(system);
	}

	private void addDeclaration(String declarationString) {
		declaration.appendChild(document.createTextNode(declarationString));
	}

	private void addSystem(String systemString) {
		system.appendChild(document.createTextNode(systemString));
	}
	
	private String computeLocationsLegend(Set<Location> uppaalLocs, Map<String, Integer> attrsList) {
	    Map<Integer, String> codeToAttrs = attrsList.entrySet().stream()
	            .collect(Collectors.toMap(
	                    Map.Entry::getValue,
	                    Map.Entry::getKey
	            ));

	    StringBuilder legend = new StringBuilder();

	    legend.append("Location legend:\n");

	    uppaalLocs.stream()
	            .filter(ul -> !ul.isCommitted())
	            .filter(ul -> ul.getAttrCode() >= 0)
	            .sorted((l1, l2) -> {
	                String name1 = l1.getName().isBlank() ? l1.getId() : l1.getName();
	                String name2 = l2.getName().isBlank() ? l2.getId() : l2.getName();

	                return name1.compareTo(name2);
	            })
	            .forEach(ul -> {
	                String attrs = codeToAttrs.getOrDefault(ul.getAttrCode(), "Unknown attributes");

	                String locationName = ul.getName().isBlank()
	                        ? ul.getId()
	                        : ul.getName();

	                legend.append("Location ")
	                        .append(locationName)
	                        .append(" ---> attrs code: ")
	                        .append(ul.getAttrCode())
	                        .append(" ---> ")
	                        .append(attrs)
	                        .append("\n");
	            });

	    return legend.toString();
	}

	private void save() {
	    try {
	        Path target = outputPath.toAbsolutePath().normalize();

	        String fileName = target.getFileName().toString();

	        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xml")) {
	            target = target.resolveSibling(fileName + ".xml");
	        }

	        Path parent = target.getParent();
	        if (parent != null) {
	            Files.createDirectories(parent);
	        }

	        Transformer transformer = TransformerFactory.newInstance().newTransformer();

	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

	        transformer.transform(
	                new DOMSource(document),
	                new StreamResult(target.toFile())
	        );

	    } catch (IOException | TransformerException e) {
	        throw new RuntimeException("Cannot save UPPAAL model to: " + outputPath, e);
	    }
	}
	
	private static String formatNumber(double value) {
	    if (Double.isNaN(value) || Double.isInfinite(value)) {
	        return Double.toString(value);
	    }

	    if (Math.abs(value) < 1e-12) {
	        return "0";
	    }

	    return java.math.BigDecimal.valueOf(value)
	            .round(new java.math.MathContext(8, java.math.RoundingMode.HALF_UP))
	            .stripTrailingZeros()
	            .toPlainString();
	}
	
	private static String attrsKey(SRTALocation location) {
	    String attrs = location.getAttrs().stream()
	            .filter(Objects::nonNull)
	            .map(String::trim)
	            .filter(attr -> !attr.isBlank())
	            .sorted()
	            .collect(Collectors.joining(", "));

	    return attrs.isBlank() ? "No attributes" : attrs;
	}

	private static int getOrCreateAttrCode(Map<String, Integer> attrsList, String attrsName) {
	    return attrsList.computeIfAbsent(attrsName, ignored -> attrsList.size());
	}

}
