package icircles.input;

import icircles.abstractDescription.*;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import com.fasterxml.jackson.annotation.*;

// Make ShadowAbstractDiagram appear in serialised JSON as "AbstractDiagram"
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT, property="AbstractDiagram")
public class AbstractDiagram {
    private int version = 0;
    private Set<String> contours;
    private Set<Zone> zones;
    private Set<Zone> shadedZones;
    private List<Spider> spiders;

    /**
     * Constructor used by Jackson to deserialise JSON to AbstractDiagram
     * Contours, Zones and ShadedZones cannot contain duplicates, Spiders can. 
     * @param version
     * @param contours
     * @param zones
     * @param shadedZones
     * @param spiders
     * @throws IllegalArgumentException
     */
    @JsonCreator
    public AbstractDiagram (
            @JsonProperty(value="Version")     int version,
            @JsonProperty(value="Contours")    Set<String> contours, 
            @JsonProperty(value="Zones")       Set<Zone> zones,
            @JsonProperty(value="ShadedZones") Set<Zone> shadedZones,
            @JsonProperty(value="Spiders")     List<Spider> spiders) throws IllegalArgumentException {
        this.version     = version;
        this.contours    = contours;
        this.zones       = zones;
        this.shadedZones = shadedZones;
        this.spiders     = spiders;

        try {
            verify();
        } catch (IllegalArgumentException iae) {
            throw iae;
        }
    }
    
    /**
     * Verifies that the deserialised JSON objects are valid AbstractDiagrams.
     * Verification occurs in three steps:
     * <ul>
     * <li>Check that each zone is composed of defined contours.</li>
     * <li>Check that shadedZones is a subset of zones.</li>
     * <li>Check that the habitat of each spider is a subset of zones.</li>
     * </ul>
     * @throws IllegalArgumentException
     */
    private void verify() throws IllegalArgumentException {
        // TODO: Check that the outside zone is defined.

        // Check that each zone is composed of defined contours.
        for(Zone z : zones) {
            if(!z.verify(contours)) {
                throw new IllegalArgumentException("All contours of zone " + z.toString() + " are not defined.");
            }
        }

        // Check that shadedZones is a subset of zones.
        if(!zones.containsAll(shadedZones)) {
            throw new IllegalArgumentException("All shaded zones must be defined as zones.");
        }

        // Check that the habitat of each spider is a subset of zones.
        for(Spider s : spiders) {
            if(!s.verify(zones)) {
                throw new IllegalArgumentException("The habitat of Spider " + s.toString() + " contains an zone not defined in " + zones.toString() + ".");
            }
        }
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
        List<AbstractSpider>     ss  = new Vector<AbstractSpider> ();

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

    /**
     * Implements deep equality of AbstractDiagrams
     */
    public boolean equals (Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof AbstractDiagram))
            return false;

        // Cast so that we can get private class variables
        AbstractDiagram other = (AbstractDiagram) obj;
        return contours.equals(other.contours) 
                && zones.equals(other.zones)
                && shadedZones.equals(other.shadedZones)
                && spiders.equals(other.spiders);
    }
}
