package icircles.input;

import icircles.abstractDescription.*;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Spider {
	@JsonProperty(value="name")
	private String name;
	
	@JsonProperty(value="habitat")
	private List <Zone> habitat;
	
	public AbstractSpider toAbstractSpider (Set <AbstractCurve> contours) {
		TreeSet<AbstractBasicRegion> feet = new TreeSet<AbstractBasicRegion>();
		for(Zone z : habitat) {
			feet.add(z.toAbstractBasicRegion(contours));
		}
		
		return new AbstractSpider(feet, name);
	}
}