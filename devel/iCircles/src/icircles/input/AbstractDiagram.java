package icircles.input;

import java.util.List;
import java.util.Vector;

import com.fasterxml.jackson.annotation.*;

// Make ShadowAbstractDiagram appear in serialised JSON as "AbstractDiagram"
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT, property="AbstractDiagram")
public class AbstractDiagram {
	@JsonProperty(value="Contours")
	private List<String> contours;
	
	@JsonProperty(value="Zones")
	private List<Zone> zones;

	@JsonProperty(value="ShadedZones")
	private List<Zone> shadedZones;

	
	@JsonProperty(value="Spiders")
	private List<Spider> spiders;
	
	/**
	public ShadowAbstractDiagram (List<String> contours, 
			List<Zone> zones,
			List<Zone> shadedZones,
			List<Spider> spiders) throws IllegalArgumentException {
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
	*/
}
