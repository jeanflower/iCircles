package icircles.input;

import icircles.abstractDescription.*;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.*;

public class Zone {
	private Set <String> in;
	
	/**
	 * We need to construct Zones for testing, so this provides the constructor.
	 */
	@JsonCreator
	public Zone (@JsonProperty(value="in") Set <String> in) {
	    this.in = in;
	}

	/**
	 * A Zone is a simple structure that allows us to, eventually create an
	 * {@see abstractDescription.AbstractBasicRegion}.
	 * 
	 * @return The AbstractBasicRegion for which this Zone is, in essence, a facade.
	 */
	public AbstractBasicRegion toAbstractBasicRegion (Set <AbstractCurve> contours) {
		Set<AbstractCurve> ts     = new TreeSet<AbstractCurve>();
		for (String label : in) {
			CurveLabel    cl = CurveLabel.get(label);
			AbstractCurve ac = getAbstractCurve(contours, cl);
			ts.add(ac);
		}
		
		return AbstractBasicRegion.get(ts);
	}
	
	/**
	 * Verify whether each of the listed contours of this zone is actually a
	 * defined contour.
	 * @param contours The set of contrours to check against.
	 * @return True if everything in 'in' is a contour, false otherwise.
	 */
	public boolean verify (Set <String> contours) {
	    // if this is the outside zone, return true
	    if(in.size() > 0) {
	        String [] a = in.toArray(new String[0]);
	        if("" == a[0]) {
	            return true;
	        }
	    }

	    // otherwise check to see if all the labels in 'in' are contours
	    return contours.containsAll(in);
	}
	
	/**
	 * Return a Zone string in JSON format.
	 * We eschew the use of the object mapper here as it can throw a
	 * JsonMatchingException in the writeValue call as seen below:
	 * <pre>  
	 * {@code
	 *  ObjectMapper m = new ObjectMapper();
     *  StringWriter w = new StringWriter();
     *  
     *  m.writeValue(w, this);
     *  
     *  return w.toString(); 
	 * }
	 * </pre>
	 * @return A string representation of this Zone as JSON.
	 */
	public String toString() {
	    StringBuilder     builder = new StringBuilder("{\"in\" : [");
	    Iterator <String> iter    = in.iterator();

	    while (iter.hasNext()) {
	         builder.append("\"" + iter.next() + "\"");
	         if (!iter.hasNext()) {
	           break;                  
	         }
	         builder.append(",");
	     }

	    builder.append("]}");
	    return builder.toString();
	}

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Zone))
            return false;
        
        Zone other = (Zone) obj;
        return in.equals(other.in);
    }
    
    @Override
    public int hashCode() {
    	// Just return the code of the underlying set
    	return in.hashCode();
    }

	/**
	 * Finds a previously defined AbstractCurve in the contour set of the diagram.
	 * {@see AbstractCurve}s are tuples of an {@see ContourLabel} and an id.  Thus
	 * we cannot simply create new {@see AbstractCurve}s.  We must re-use a 
	 * previously created AbstractCurve i.e. the one with the correct id.
	 * 
	 * FIXME: This design suggests a bug in AbstractCurve, particularly when we
	 * wish to define contours ("A", 1) and ("A", 2) and pick a specific one. 
	 * 
	 * @param contours The contour set of the diagram.
	 * @param label The label of the curve that we are looking for.
	 * @return A previously defined AbstractCurve that matches the label.
	 */
	private AbstractCurve getAbstractCurve (Set <AbstractCurve> contours, CurveLabel label) {
		for(AbstractCurve ac : contours) {
			if(ac.getLabel().getLabel() == label.getLabel()) {
				return ac;
			}
		}
		// This can only be reached if we're looking for a zone that is defined
		// using a contour that has not been defined. 
		return null;
	}

}