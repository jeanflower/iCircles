package icircles.input;

import icircles.abstractDescription.AbstractBasicRegion;
import icircles.abstractDescription.AbstractCurve;
import icircles.abstractDescription.AbstractSpider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Spider {
    private String name;
    private Set <Zone> habitat;

    /*
     * Like all classes in icircles.input the Spider constructor takes a Zone[]
     * as input rather than a Set<Zone> due to Java's erasure of generic type
     * information.  The removal of generic type information makes it *really*
     * difficult for calling code to recreate these types when given just the
     * .class files.  To do otherwise would, effectively, require calling code
     * to use iCircles as a compile-time depencency.
     */
    @JsonCreator
    public Spider(@JsonProperty(value="name")    String name,
                  @JsonProperty(value="habitat") Zone[] habitat) {
        this.name = name;
        this.habitat = new HashSet<Zone>(Arrays.asList(habitat));
    }
    
    public AbstractSpider toAbstractSpider (Set <AbstractCurve> contours) {
        TreeSet<AbstractBasicRegion> feet = new TreeSet<AbstractBasicRegion>();
        for(Zone z : habitat) {
            feet.add(z.toAbstractBasicRegion(contours));
        }

        return new AbstractSpider(feet, name);
    }

    public boolean verify(Set <Zone> zones) {
        return zones.containsAll(habitat);
    }

    public String toString() {
        StringBuilder     builder = new StringBuilder("{\"name\" : ");
        if(null == name) {
            builder.append("null"); // Deliberately unquoted JSON null object
        } else {
            builder.append("\"" + name + "\"");
        }
        builder.append(", \"habitat\" : [");
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