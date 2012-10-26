package icircles.input;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Spider {
	@JsonProperty(value="count")
	private int count;
	
	@JsonProperty(value="habitat")
	private List <Zone> habitat;
}