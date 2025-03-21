package edge;

import java.util.ArrayList;
import java.util.List;

public class EDRTAEdge {
	private int id;
	private int sourceId;
	private int targetId;
	private List<Double> guard;
	private String event;
	private List<Double> samples; // new
	private Double prob;
	
	public EDRTAEdge(int id, int sourceId, int targetId, double min, double max, String event) {
		this.id = id;
		this.sourceId = sourceId;
		this.targetId = targetId;
		guard = new ArrayList<>(2);
		guard.add(min);
		guard.add(max);
		this.event = event;
		samples = new ArrayList<>(); // New
		prob = null;
	}

	public int getSourceId() {
		return sourceId;
	}

	public void setSourceId(int sourceId) {
		this.sourceId = sourceId;
	}

	public int getTargetId() {
		return targetId;
	}

	public void setTargetId(int targetId) {
		this.targetId = targetId;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Double> getGuard() {
		return guard;
	}
	
	public Double getMin() {
		return guard.get(0);
	}
	
	public Double getMax() {
		return guard.get(1);
	}

	public void setGuard(List<Double> guard) {
		this.guard = guard;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}
	
	public void setMax(double newMax) {
		guard.set(1, newMax);	
	}
	
	public void setMin(double newMin) {
		guard.set(0, newMin);
	}
	
	public List<Double> getSamples() { // New
		return samples;
	}
	
	public void addSample(double sample) { // New
		samples.add(sample);
	}
	
	public void addSamples(List<Double> newSamples) { // New
		samples.addAll(newSamples);
	}

	public Double getProb() {
		return prob;
	}

	public void setProb(Double prob) {
		this.prob = prob;
	}

	@Override
	public String toString() {
		return "Edge " + id + " [ sourceStateId=" + sourceId + ", targetStateId=" + targetId
				+ ", interval=" + guard.toString() + ", event=" + event + " ]";
	}
	
}
