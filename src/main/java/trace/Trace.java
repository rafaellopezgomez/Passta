package trace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Trace {
	
	ArrayList<Observation> obs;
	
	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public Trace(@JsonProperty("observations") ArrayList<Observation> obs) {
		this.obs = obs;
	}

	public ArrayList<Observation> getObs() {
		return obs;
	}

	public void setObs(ArrayList<Observation> obs) {
		this.obs = obs;
	}

	@Override
	public String toString() {
		return obs.stream().map(Object::toString).collect(Collectors.joining("\n"));
	}
	
}