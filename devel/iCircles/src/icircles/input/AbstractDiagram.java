package icircles.input;

import java.util.List;
import java.util.Vector;

import com.fasterxml.jackson.annotation.*;

// Make ShadowAbstractDiagram appear in serialised JSON as "AbstractDiagram"
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT, property="AbstractDiagram")
public class AbstractDiagram {
	private List<String> contours;
	private List<Zone> zones;
	private List<Zone> shadedZones;
	private List<Spider> spiders;
	
	@JsonCreator
	public AbstractDiagram (
			@JsonProperty(value="Contours")    List<String> contours, 
			@JsonProperty(value="Zones")       List<Zone> zones,
			@JsonProperty(value="ShadedZones") List<Zone> shadedZones,
			@JsonProperty(value="Spiders")     List<Spider> spiders) throws IllegalArgumentException {
		this.contours    = contours;
		this.zones       = zones;
		this.shadedZones = shadedZones;
		this.spiders     = spiders;
		
		if(!verify()) {
			throw new IllegalArgumentException();
		}
	}
	
	private boolean verify() {
		return true;
	}
}
