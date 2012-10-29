package icircles.input;

import icircles.abstractDescription.*;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Zone {
	@JsonProperty(value="in")
	private List <String> in;
	
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