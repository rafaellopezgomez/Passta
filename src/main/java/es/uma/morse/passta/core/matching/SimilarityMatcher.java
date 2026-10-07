package es.uma.morse.passta.core.matching;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import es.uma.morse.passta.core.automaton.SRTAEdge;
import es.uma.morse.passta.core.automaton.SRTALocation;

public final class SimilarityMatcher {

	private SimilarityMatcher() {
	}

	/**
	 * Method used to compare two futures with respect to a specific similarity type
	 */
	public static boolean compareFutures(List<Object> future1, List<Object> future2, SimilarityType similarity) {
		Objects.requireNonNull(future1, "future1 is null");
		Objects.requireNonNull(future2, "future2 is null");
		Objects.requireNonNull(similarity, "similarity is null");

		if (future1.size() != future2.size()) {
			return false;
		}

		Iterator<Object> iterator1 = future1.iterator();
		Iterator<Object> iterator2 = future2.iterator();

		while (iterator1.hasNext()) {
			Object element1 = iterator1.next();
			Object element2 = iterator2.next();

			if (element1 instanceof SRTALocation location1 && element2 instanceof SRTALocation location2) {
				if (!location1.getAttrs().equals(location2.getAttrs())) {
					return false;
				}
			} else if (element1 instanceof SRTAEdge edge1 && element2 instanceof SRTAEdge edge2) {
				boolean similar = switch (similarity) {
				case STRUCTURAL -> structuralSimilarity(edge1, edge2);
				case WEAK_TIME -> weakTimeSimilarity(edge1, edge2);
				case STRONG_TIME -> strongTimeSimilarity(edge1, edge2);
				default -> throw new IllegalArgumentException("Not defined similarity: " + similarity);
				};
				if (!similar) {
					return false;
				}
			} else {
				return false;
			}
		}

		return true;
	}

	/**
	 * Method that performs a comparison between the k-futures of two locations
	 *
	 * @param fs1
	 * @param fs2
	 * @return true if both k-futures are similar, false otherwise
	 */
	public static boolean compareKFutures(List<List<Object>> futures1, List<List<Object>> futures2) {

		Objects.requireNonNull(futures1, "futures1 is null");
		Objects.requireNonNull(futures2, "futures2 is null");

		if (futures1.isEmpty() || futures2.isEmpty()) {
			return false;
		}

		boolean weakTimeSimilarityF1 = futures1.stream().allMatch(future1 -> futures2.stream()
				.anyMatch(future2 -> compareFutures(future1, future2, SimilarityType.WEAK_TIME)));
		boolean weakTimeSimilarityF2 = futures2.stream().allMatch(future2 -> futures1.stream()
				.anyMatch(future1 -> compareFutures(future1, future2, SimilarityType.WEAK_TIME)));

		if (weakTimeSimilarityF1 && weakTimeSimilarityF2) {
			return true;
		}

		boolean strongTimeSimilarityF1 = futures1.stream().allMatch(future1 -> futures2.stream()
				.anyMatch(future2 -> compareFutures(future1, future2, SimilarityType.STRONG_TIME)));

		boolean strongTimeSimilarityF2 = futures2.stream().allMatch(future2 -> futures1.stream()
				.anyMatch(future1 -> compareFutures(future1, future2, SimilarityType.STRONG_TIME)));

		return strongTimeSimilarityF1 || strongTimeSimilarityF2;

	}

	private static boolean structuralSimilarity(SRTAEdge edge1, SRTAEdge edge2) {
		return edge1.getEvent().equals(edge2.getEvent());
	}

	private static boolean weakTimeSimilarity(SRTAEdge edge1, SRTAEdge edge2) {
		var min1 = edge1.getMin();
		var min2 = edge2.getMin();
		var max1 = edge1.getMax();
		var max2 = edge2.getMax();
		return intervalsOverlap(min1, max1, min2, max2);
	}

	private static boolean strongTimeSimilarity(SRTAEdge edge1, SRTAEdge edge2) {
		var min1 = edge1.getMin();
		var min2 = edge2.getMin();
		var max1 = edge1.getMax();
		var max2 = edge2.getMax();
		return intervalContainsOther(min1, max1, min2, max2);
	}

	public static boolean intervalsOverlap(double min1, double max1, double min2, double max2) {

		if (min1 > max1 || min2 > max2) {
			throw new IllegalArgumentException("Invalid interval: minimum must not exceed maximum");
		}

		return Math.max(min1, min2) <= Math.min(max1, max2);
	}

	private static boolean intervalContainsOther(double min1, double max1, double min2, double max2) {
		boolean firstContainsSecond = min1 <= min2 && max1 >= max2;

		boolean secondContainsFirst = min2 <= min1 && max2 >= max1;

		return firstContainsSecond || secondContainsFirst;
	}
}