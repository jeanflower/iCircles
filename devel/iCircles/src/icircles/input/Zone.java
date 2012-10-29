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