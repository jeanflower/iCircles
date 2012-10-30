package icircles.input;

import icircles.abstractDescription.*;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.*;

public class Spider {
	private String name;
	private Set <Zone> habitat;

	@JsonCreator
	public Spider(@JsonProperty(value="name")    String name,
	              @JsonProperty(value="habitat") Set <Zone> habitat) {
	    this.name = name;
	    this.habitat = habitat;
	}
	
	public AbstractSpider toAbstractSpider (Set <AbstractCurve> contours) {
		TreeSet<AbstractBasicRegion> feet = new TreeSet<AbstractBasicRegion>();
		for(Zone z : habitat) {
			feet.add(z.toAbstractBasicRegion(contours));
		}
		
		return new AbstractSpider(feet, name);
	}

	public boolean verify(Set <Zone> zones) {
	    // Do this the long way
	    for(Zone z1 : habitat) {
	        boolean foundFlag = false;
	        for(Zone z2 : zones) {
	            if(z1.equals(z2)) {
	                foundFlag = true;
	            }
	        }
	        if(!foundFlag) {
	            return false;
	        }
	    }
	    return true;
	    
	    // TODO: Find out why this doesn't work.
	    //return zones.containsAll(habitat);
	}

	public String toString() {
	    StringBuilder     builder = new StringBuilder("{\"name\" : \"" + name + "\", \"habitat\" : [");
	    Iterator <Zone>   iter    = habitat.iterator();

	    while (iter.hasNext()) {
	        builder.append(iter.next().toString());
	        if (!iter.hasNext()) {
	            break;
	        }
	        builder.append(",");
	    }

	    builder.append("]}");
	    return builder.toString();
	}
}