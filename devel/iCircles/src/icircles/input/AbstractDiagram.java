package icircles.input;

import icircles.abstractDescription.*;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.*;

// Make ShadowAbstractDiagram appear in serialised JSON as "AbstractDiagram"
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT, property="AbstractDiagram")
public class AbstractDiagram {
    private int version = 0;
	private List<String> contours;
	private List<Zone> zones;
	private List<Zone> shadedZones;
	private List<Spider> spiders;
	
	@JsonCreator
	public AbstractDiagram (
	        @JsonProperty(value="Version")     int version,
			@JsonProperty(value="Contours")    List<String> contours, 
			@JsonProperty(value="Zones")       List<Zone> zones,
			@JsonProperty(value="ShadedZones") List<Zone> shadedZones,
			@JsonProperty(value="Spiders")     List<Spider> spiders) throws IllegalArgumentException {
	    this.version     = version;
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

	/**
	 * Creates an AbstractDescription from this AbstractDiagram.
	 * 
	 * @return the AbstractDescription represented by this AbstractDiagram facade.
	 */
	public AbstractDescription toAbstractDescription () {
		Set<AbstractCurve>       cs  = new TreeSet<AbstractCurve> ();
		Set<AbstractBasicRegion> zs  = new TreeSet<AbstractBasicRegion> ();
		Set<AbstractBasicRegion> szs = new TreeSet<AbstractBasicRegion> ();
		Set<AbstractSpider>      ss  = new TreeSet<AbstractSpider> ();

		for (String c : contours) {
			cs.add(new AbstractCurve(CurveLabel.get(c)));
		}
		
		for (Zone z : zones) {
			zs.add(z.toAbstractBasicRegion(cs));
		}
		
		for (Zone z : shadedZones) {
			szs.add(z.toAbstractBasicRegion(cs));
		}
		
		for (Spider s: spiders) {
			ss.add(s.toAbstractSpider(cs));
		}
	
		return new AbstractDescription(cs, zs, szs, ss);
	}
}
