package es.uma.morse.passta.io;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Comparator;
import java.util.TreeMap;

import es.uma.morse.passta.core.automaton.SRTA;
import es.uma.morse.passta.core.automaton.SRTAEdge;
import es.uma.morse.passta.core.automaton.SRTALocation;
import javafx.geometry.Point2D;

public final class SrtaGraphLayout {

	private static final double START_X = 180.0;
	private static final double START_Y = 120.0;

	private static final double MIN_HORIZONTAL_GAP = 180.0;
	private static final double MIN_VERTICAL_SPACING = 150.0;
	private static final double STATE_MARGIN = 50.0;

	private static final int ORDERING_PASSES = 4;

	private SrtaGraphLayout() {
	}

	public static Map<Integer, Point2D> calculatePositions(SRTA automaton) {
		if (automaton == null) {
			throw new IllegalArgumentException("The automaton cannot be null");
		}

		if (automaton.isEmpty()) {
			return Map.of();
		}

		Map<Integer, Integer> levels = calculateLevels(automaton);

		Map<Integer, List<SRTALocation>> locationsByLevel = groupLocationsByLevel(automaton.getAllLocations(), levels);

		orderLocationsWithinLevels(automaton, locationsByLevel, levels);

		return calculateLevelPositions(locationsByLevel);
	}

	private static Map<Integer, Integer> calculateLevels(SRTA automaton) {
		Map<Integer, Integer> levels = new HashMap<>();
		Queue<Integer> pendingLocations = new ArrayDeque<>();

		SRTALocation initialLocation = automaton.getLocation(0);

		if (initialLocation == null) {
			initialLocation = automaton.getAllLocations().iterator().next();
		}

		levels.put(initialLocation.getId(), 0);
		pendingLocations.add(initialLocation.getId());

		while (!pendingLocations.isEmpty()) {
			int sourceId = pendingLocations.remove();
			int sourceLevel = levels.get(sourceId);

			SRTALocation source = automaton.getLocation(sourceId);

			for (Integer edgeId : source.getOutEdges()) {
				SRTAEdge edge = automaton.getEdge(edgeId);

				if (edge == null) {
					continue;
				}

				int targetId = edge.getTargetId();

				if (!levels.containsKey(targetId)) {
					levels.put(targetId, sourceLevel + 1);
					pendingLocations.add(targetId);
				}
			}
		}

		assignUnreachableLocations(automaton.getAllLocations(), levels);

		return levels;
	}

	private static void assignUnreachableLocations(Collection<SRTALocation> locations, Map<Integer, Integer> levels) {
		int lastLevel = levels.values().stream().mapToInt(Integer::intValue).max().orElse(0);

		int unreachableLevel = lastLevel + 1;

		for (SRTALocation location : locations) {
			levels.putIfAbsent(location.getId(), unreachableLevel);
		}
	}

	private static Map<Integer, List<SRTALocation>> groupLocationsByLevel(Collection<SRTALocation> locations,
			Map<Integer, Integer> levels) {

		Map<Integer, List<SRTALocation>> result = new TreeMap<>();

		for (SRTALocation location : locations) {
			int level = levels.get(location.getId());

			result.computeIfAbsent(level, ignored -> new ArrayList<>()).add(location);
		}

		return result;
	}

	private static void orderLocationsWithinLevels(SRTA automaton, Map<Integer, List<SRTALocation>> locationsByLevel,
			Map<Integer, Integer> levels) {
		Map<Integer, Double> verticalOrder = new HashMap<>();

		for (List<SRTALocation> locations : locationsByLevel.values()) {
			updateVerticalOrder(locations, verticalOrder);
		}

		for (int pass = 0; pass < ORDERING_PASSES; pass++) {
			orderLevelsFromLeftToRight(automaton, locationsByLevel, levels, verticalOrder);

			orderLevelsFromRightToLeft(automaton, locationsByLevel, levels, verticalOrder);
		}
	}

	private static void updateVerticalOrder(List<SRTALocation> locations, Map<Integer, Double> verticalOrder) {
		for (int index = 0; index < locations.size(); index++) {

			verticalOrder.put(locations.get(index).getId(), (double) index);
		}
	}

	private static void orderLevelsFromLeftToRight(SRTA automaton, Map<Integer, List<SRTALocation>> locationsByLevel,
			Map<Integer, Integer> levels, Map<Integer, Double> verticalOrder) {
		List<Integer> levelNumbers = new ArrayList<>(locationsByLevel.keySet());

		levelNumbers.sort(Comparator.naturalOrder());

		for (Integer level : levelNumbers) {
			if (level == 0) {
				continue;
			}

			List<SRTALocation> locations = locationsByLevel.get(level);

			Comparator<SRTALocation> comparator = Comparator
					.comparingDouble(
							(SRTALocation location) -> calculateParentOrder(automaton, location, levels, verticalOrder))
					.thenComparingInt(SRTALocation::getId);

			locations.sort(comparator);

			updateVerticalOrder(locations, verticalOrder);
		}
	}

	private static void orderLevelsFromRightToLeft(SRTA automaton, Map<Integer, List<SRTALocation>> locationsByLevel,
			Map<Integer, Integer> levels, Map<Integer, Double> verticalOrder) {
		List<Integer> levelNumbers = new ArrayList<>(locationsByLevel.keySet());

		levelNumbers.sort(Comparator.reverseOrder());

		for (Integer level : levelNumbers) {
			if (level == 0) {
				continue;
			}

			List<SRTALocation> locations = locationsByLevel.get(level);

			Comparator<SRTALocation> comparator = Comparator
					.comparingDouble(
							(SRTALocation location) -> calculateChildOrder(automaton, location, levels, verticalOrder))
					.thenComparingInt(SRTALocation::getId);

			locations.sort(comparator);

			updateVerticalOrder(locations, verticalOrder);
		}
	}

	private static double calculateChildOrder(SRTA automaton, SRTALocation location, Map<Integer, Integer> levels,
			Map<Integer, Double> verticalOrder) {
		Double childOrder = calculateAverageChildOrder(automaton, location, levels, verticalOrder);

		if (childOrder != null) {
			return childOrder;
		}

		return verticalOrder.getOrDefault(location.getId(), (double) location.getId());
	}

	private static Double calculateAverageParentOrder(SRTA automaton, SRTALocation location,
			Map<Integer, Integer> levels, Map<Integer, Double> verticalOrder) {
		int locationLevel = levels.get(location.getId());

		double total = 0.0;
		int count = 0;

		for (Integer edgeId : location.getInEdges()) {
			SRTAEdge edge = automaton.getEdge(edgeId);

			if (edge == null) {
				continue;
			}

			int sourceId = edge.getSourceId();

			Integer sourceLevel = levels.get(sourceId);
			Double sourceOrder = verticalOrder.get(sourceId);

			if (sourceLevel == null || sourceOrder == null) {
				continue;
			}

			if (sourceLevel >= locationLevel) {
				continue;
			}

			total += sourceOrder;
			count++;
		}

		if (count == 0) {
			return null;
		}

		return total / count;
	}

	private static Double calculateAverageChildOrder(SRTA automaton, SRTALocation location,
			Map<Integer, Integer> levels, Map<Integer, Double> verticalOrder) {
		int locationLevel = levels.get(location.getId());

		double total = 0.0;
		int count = 0;

		for (Integer edgeId : location.getOutEdges()) {
			SRTAEdge edge = automaton.getEdge(edgeId);

			if (edge == null) {
				continue;
			}

			int targetId = edge.getTargetId();

			Integer targetLevel = levels.get(targetId);
			Double targetOrder = verticalOrder.get(targetId);

			if (targetLevel == null || targetOrder == null) {
				continue;
			}

			if (targetLevel <= locationLevel) {
				continue;
			}

			total += targetOrder;
			count++;
		}

		if (count == 0) {
			return null;
		}

		return total / count;
	}

	private static double calculateParentOrder(SRTA automaton, SRTALocation location, Map<Integer, Integer> levels,
			Map<Integer, Double> verticalOrder) {
		Double parentOrder = calculateAverageParentOrder(automaton, location, levels, verticalOrder);

		if (parentOrder != null) {
			return parentOrder;
		}

		return location.getId();
	}

	private static Map<Integer, Point2D> calculateLevelPositions(Map<Integer, List<SRTALocation>> locationsByLevel) {
		Map<Integer, Point2D> positions = new HashMap<>();

		Map<Integer, Double> levelHeights = new HashMap<>();
		Map<Integer, Double> levelWidths = new HashMap<>();

		double maximumLevelHeight = 0.0;

		for (Map.Entry<Integer, List<SRTALocation>> entry : locationsByLevel.entrySet()) {

			int level = entry.getKey();
			List<SRTALocation> locations = entry.getValue();

			double levelHeight = calculateLevelHeight(locations);

			double levelWidth = calculateLevelWidth(locations);

			levelHeights.put(level, levelHeight);
			levelWidths.put(level, levelWidth);

			maximumLevelHeight = Math.max(maximumLevelHeight, levelHeight);
		}

		double currentX = START_X;

		for (Map.Entry<Integer, List<SRTALocation>> entry : locationsByLevel.entrySet()) {

			int level = entry.getKey();
			List<SRTALocation> locations = entry.getValue();

			double levelHeight = levelHeights.get(level);

			double levelWidth = levelWidths.get(level);

			double centerX = currentX + levelWidth / 2.0;

			double currentY = START_Y + (maximumLevelHeight - levelHeight) / 2.0;

			for (SRTALocation location : locations) {
				double estimatedDiameter = estimateStateDiameter(location);

				double centerY = currentY + estimatedDiameter / 2.0;

				positions.put(location.getId(), new Point2D(centerX, centerY));

				currentY += Math.max(estimatedDiameter + STATE_MARGIN, MIN_VERTICAL_SPACING);
			}

			currentX += levelWidth + MIN_HORIZONTAL_GAP;
		}

		return positions;
	}

	private static double calculateLevelWidth(List<SRTALocation> locations) {
		double maximumDiameter = 0.0;

		for (SRTALocation location : locations) {
			maximumDiameter = Math.max(maximumDiameter, estimateStateDiameter(location));
		}

		return maximumDiameter;
	}

	private static double calculateLevelHeight(List<SRTALocation> locations) {
		double height = 0.0;

		for (SRTALocation location : locations) {
			double estimatedDiameter = estimateStateDiameter(location);

			height += Math.max(estimatedDiameter + STATE_MARGIN, MIN_VERTICAL_SPACING);
		}

		if (!locations.isEmpty()) {
			height -= STATE_MARGIN;
		}

		return height;
	}

	private static double estimateStateDiameter(SRTALocation location) {
		int numberOfLines = 1;
		int longestLineLength = Integer.toString(location.getId()).length() + 1;

		if (location.getAttrs() != null && !location.getAttrs().isEmpty()) {

			numberOfLines++;

			String attributes = String.join(", ", location.getAttrs());

			longestLineLength = Math.max(longestLineLength, attributes.length());
		}

		Double invariant = location.getInvariant();

		if (invariant != null && invariant >= 0.0) {
			numberOfLines++;

			String invariantText = "x <= " + invariant;

			longestLineLength = Math.max(longestLineLength, invariantText.length());
		}

		double estimatedRadius = Math.max(38.0, Math.max(18.0 + longestLineLength * 3.7, 24.0 + numberOfLines * 8.0));

		estimatedRadius = Math.min(estimatedRadius, 90.0);

		return estimatedRadius * 2.0;
	}
}
