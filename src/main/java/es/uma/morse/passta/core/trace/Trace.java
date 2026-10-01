package es.uma.morse.passta.core.trace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Trace {
	
	private List<Observation> observations;
	
	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public Trace(@JsonProperty("obs") List<Observation> observations) {
		this.observations = Objects.requireNonNull(observations, "Observations list is null");
	}
	
	@JsonProperty("obs")
	public List<Observation> getObs() {
		return observations;
	}

	public void setObs(List<Observation> observations) {
		this.observations = Objects.requireNonNull(observations, "Observations list is null");
	}

	@Override
	public String toString() {
		return observations.stream().map(Object::toString).collect(Collectors.joining("\n"));
	}

	public boolean isEmpty() {
		return observations.isEmpty();
	}
	
}