package icircles.abstractDescription;
/*
 * @author Jean Flower <jeanflower@rocketmail.com>
 * Copyright (c) 2012
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of the iCircles Project.
 */

import icircles.util.DEB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
// For makeForTesting and friends

/**
 * An AbstractDescription encapsulates the elements of a diagram, with no drawn information.
 * A diagram comprises a set of AbstractCurves (the contours).
 * A set of AbstractBasicRegions is given (zones which must be present.
 * <p>
 * An AbstractDiagram is consistent if
 * <ol>
 * <li>The contours in each of the AbstractBasicRegions match those
 * in m_contours.</li>
 * <li>Every valid diagram includes the "outside" zone.</li>
 * <li>Every shaded zone is also a zone.</li>
 * <li>Every contour must have a zone inside it.</li>
 * </ol>
 * </p>
 * Currently, there is no checking done to ensure that conditions 1--4 are
 * adhered to.  As such, you can create invalid diagrams.
 *
 * TODO: add a coherence check on these internal checks.
 */
public class AbstractDescription {

    TreeSet<AbstractCurve> m_contours;
    Set<AbstractBasicRegion> m_zones;
    Set<AbstractBasicRegion> m_shaded_zones;

    List<AbstractSpider> m_spiders;
    
    public AbstractDescription(Set<AbstractCurve> contours,
               Set<AbstractBasicRegion> zones,
               Set<AbstractBasicRegion> shaded_zones,
               List<AbstractSpider> spiders) {
        this(contours, zones, shaded_zones);
        m_spiders = spiders;
    }
    
    public AbstractDescription(Set<AbstractCurve> contours,
                   Set<AbstractBasicRegion> zones,
                   Set<AbstractBasicRegion> shaded_zones) {
        this(contours, zones);
        m_shaded_zones = new TreeSet<AbstractBasicRegion>(shaded_zones);
        m_spiders = new ArrayList<AbstractSpider>();
    }

    public AbstractDescription(Set<AbstractCurve> contours,
                   Set<AbstractBasicRegion> zones) {
        m_contours = new TreeSet<AbstractCurve>(contours);
        m_zones = new TreeSet<AbstractBasicRegion>(zones);
        m_shaded_zones = new TreeSet<AbstractBasicRegion>();
        m_spiders = new ArrayList<AbstractSpider>();
    }

    public void addSpider(AbstractSpider s){
        // TODO : check that feet are indeed AbstractBasicRegions of the diagram
        m_spiders.add(s);
    }

    public AbstractCurve getFirstContour() {
        if (m_contours.size() == 0) {
            return null;
        }
        return m_contours.first();
    }

    public AbstractCurve getLastContour() {
        if (m_contours.size() == 0) {
            return null;
        }
        return m_contours.last();
    }

    public Iterator<AbstractCurve> getContourIterator() {
        return m_contours.iterator();
    }

    public int getNumContours() {
        return m_contours.size();
    }

    public Iterator<AbstractBasicRegion> getZoneIterator() {
        return m_zones.iterator();
    }
    // expensive - do not use just for querying
    public TreeSet<AbstractCurve> getCopyOfContours() {
        return new TreeSet<AbstractCurve>(m_contours);
    }
    // expensive - do not use just for querying
    public TreeSet<AbstractBasicRegion> getCopyOfZones() {
        return new TreeSet<AbstractBasicRegion>(m_zones);
    }

    public Iterator<AbstractSpider> getSpiderIterator() {
        return m_spiders.iterator();
    }

    public String debug() {
        if (DEB.level == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        b.append("labels:");
        boolean first = true;
        if (DEB.level > 1) {
            b.append("{");
        }
        for (AbstractCurve c : m_contours) {
            if (!first) {
                b.append(",");
            }
            b.append(c.debug());
            first = false;
        }
        if (DEB.level > 1) {
            b.append("}");
        }
        b.append("\n");
        b.append("zones:");
        if (DEB.level > 1) {
            b.append("{");
        }
        first = true;
        for (AbstractBasicRegion z : m_zones) {
            if (!first) {
                b.append(",");
            }
            if (DEB.level > 1) {
                b.append("\n");
            }
            b.append(z.debug());
            first = false;
        }
        if (DEB.level > 1) {
            b.append("}");
        }
        b.append(" shading:");
        first = true;
        for (AbstractBasicRegion z : m_shaded_zones) {
            if (!first) {
                b.append(",");
            }
            if (DEB.level > 1) {
                b.append("\n");
            }
            b.append(z.debug());
            first = false;
        }
        if (DEB.level > 1) {
            b.append("}");
        }
        b.append("\n");

        return b.toString();
    }

    public String debugAsSentence() {
        HashMap<AbstractCurve, String> printable = new HashMap<AbstractCurve, String>();
        for (AbstractCurve c : m_contours) {
            printable.put(c, print_contour(c));
        }
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (AbstractBasicRegion z : m_zones) {
            if (!first) {
                b.append(",");
            }
            Iterator<AbstractCurve> c_it = z.getContourIterator();
            boolean printed_something = false;
            while (c_it.hasNext()) {
                AbstractCurve c = c_it.next();
                b.append(printable.get(c));
                printed_something = true;
            }
            if (!printed_something) {
                b.append("0");
            }
            first = false;
        }
        return b.toString();
    }

    public String print_contour(AbstractCurve c) {
        if (one_of_multiple_instances(c)) {
            return c.debugWithId();
        } else {
            return c.debug();
        }
    }

    boolean one_of_multiple_instances(AbstractCurve c) {
        for (AbstractCurve cc : m_contours) {
            if (cc != c && cc.matches_label(c)) {
                return true;
            }
        }
        return false;
    }

    public int getNumZones() {
        return m_zones.size();
    }

    public double checksum() {
        double scaling = 2.1;
        double result = 0.0;
        for (AbstractCurve c : m_contours) {
            result += c.checksum() * scaling;
            scaling += 0.07;
            scaling += 0.05;
            for (AbstractBasicRegion z : m_zones) {
                if (z.is_in(c)) {
                    result += z.checksum() * scaling;
                    scaling += 0.09;
                }
            }
        }
        return result;
    }

    public boolean includesLabel(CurveLabel l) {
        for (AbstractCurve c : m_contours) {
            if (c.getLabel() == l) {
                return true;
            }
        }
        return false;
    }

    public AbstractBasicRegion getLabelEquivalentZone(AbstractBasicRegion z) {
        for (AbstractBasicRegion zone : m_zones) {
            if (zone.isLabelEquivalent(z)) {
                return zone;
            }
        }
        return null;
    }
    
    public boolean hasShadedZone(AbstractBasicRegion z){
        return m_shaded_zones.contains(z);
    }
    
    /*
     * makeForTesting family of methods is maintained for backward-compatibility of tests,
     * until we put in place a cleaner way to run the graphical tests.
     */
    /*
     * TODO This method will not be needed after the TestCode class is converted to use the 
     * JSON input format and junit test framework.
     */
    public static AbstractDescription makeForTesting(String s) {
    	return makeForTesting(s, false);
    }
    /*
     * TODO This method will not be needed after the TestCode class is converted to use the 
     * JSON input format and junit test framework.
     */
    private static ArrayList<String> getDescriptors(String input_s)
    {
    	ArrayList<String> strings = new ArrayList<String>();
    	strings.add(""); // diagram zones
    	strings.add(""); // shaded zones
    	// any more are spider descriptions
    	
        StringTokenizer st = new StringTokenizer(input_s, ",", true); // split by commas, return commas as tokens
    	if(!st.hasMoreTokens())
    		return strings;
    	String s = st.nextToken();
    	if(!s.equals(","))
    	{
    		strings.set(0,s);
        	if(!st.hasMoreTokens())
        		return strings;
    		s = st.nextToken();
    	}
    	if(!st.hasMoreTokens())
    		return strings;
    	s = st.nextToken();
    	if(!s.equals(","))
    	{
    		strings.set(1,s);
        	if(!st.hasMoreTokens())
        		return strings;
    		s = st.nextToken();
    	}
    	while(true)
    	{
	    	if(!st.hasMoreTokens())
	    		return strings;
	    	s = st.nextToken();
	    	if(!s.equals(","))
	    	{
	    		strings.add(s);
	        	if(!st.hasMoreTokens())
	        		return strings;
	    		s = st.nextToken();
	    	}
    	}
    }
    /*
     * TODO This method will not be needed after the TestCode class is converted to use the 
     * JSON input format and junit test framework.
     */
    public String makeForTesting(){
    	StringBuilder b = new StringBuilder();
    	for(AbstractBasicRegion zone : m_zones){
    		if(!zone.m_in_set.isEmpty()){ // don't journal out "." for empty zone - it's assumed
    		    b.append(zone.journalString());
    		    b.append(" ");
    		}
    	}
    	b.append(", ");
    	for(AbstractBasicRegion zone : m_shaded_zones){
    		b.append(zone.journalString());
    		b.append(" ");
    	}
    	for(AbstractSpider s : m_spiders){
    		b.append(", ");
    		b.append(s.journalString());
    	}
    	return b.toString();
    }
    /*
     * TODO This method will not be needed after the TestCode class is converted to use the 
     * JSON input format and junit test framework.
     */
    public static AbstractDescription makeForTesting(String s, boolean random_shaded_zones) {
    	
    	ArrayList<String> descriptors = getDescriptors(s);
        String diagString = descriptors.get(0);
        String shadingString = descriptors.get(1);
        
        descriptors.remove(0);
        descriptors.remove(0);
        ArrayList<String> spiderStrings = descriptors;
    	
        TreeSet<AbstractBasicRegion> ad_zones = new TreeSet<AbstractBasicRegion>();
        AbstractBasicRegion outsideZone = AbstractBasicRegion.get(new TreeSet<AbstractCurve>());
        ad_zones.add(outsideZone);
        HashMap<CurveLabel, AbstractCurve> contours = new HashMap<CurveLabel, AbstractCurve>();
        if(diagString != null)
        {
	        StringTokenizer st = new StringTokenizer(diagString); // for spaces
	        while (st.hasMoreTokens()) {
	            String word = st.nextToken();
	            TreeSet<AbstractCurve> zoneContours = new TreeSet<AbstractCurve>();
	            for (int i = 0; i < word.length(); i++) {
	                String character = "" + word.charAt(i);
	                CurveLabel cl = CurveLabel.get(character);
	                if (!contours.containsKey(cl)) {
	                    contours.put(cl, new AbstractCurve(cl));
	                }
	                zoneContours.add(contours.get(cl));
	            }
	            AbstractBasicRegion thisZone = AbstractBasicRegion.get(zoneContours);
	            ad_zones.add(thisZone);
	        }
        }
        TreeSet<AbstractCurve> ad_contours = new TreeSet<AbstractCurve>(contours.values());
        
        // set some shaded zones
        TreeSet<AbstractBasicRegion> ad_shaded_zones = new TreeSet<AbstractBasicRegion>();
        if(random_shaded_zones)
        {
        	Random r = new Random();
	        for(AbstractBasicRegion abr: ad_zones)
	        {
	        	if(random_shaded_zones)
	        	{
	        		if(r.nextBoolean())
	        			ad_shaded_zones.add(abr);
	        	}
	        }
        }
        else if(shadingString != null)
        {
        	StringTokenizer st = new StringTokenizer(shadingString); // for spaces
            while (st.hasMoreTokens()) {
                String word = st.nextToken();
                AbstractBasicRegion thisZone = null;
                if(word.equals("."))
                {
                	// this means the outside zone
                	thisZone = outsideZone;
                }
                else
                {
	                TreeSet<AbstractCurve> zoneContours = new TreeSet<AbstractCurve>();
	                for (int i = 0; i < word.length(); i++) {
	                    String character = "" + word.charAt(i);
	                    CurveLabel cl = CurveLabel.get(character);
	                    AbstractCurve ac = contours.get(cl);
	                    if(ac == null)
	                    	throw new RuntimeException("malformed diagram spec : contour "+ac+"\n");
	                    zoneContours.add(ac);
	                }
	                thisZone = AbstractBasicRegion.get(zoneContours);
                }
                if(!ad_zones.contains(thisZone))
                {
                	throw new RuntimeException("malformed diagram spec : zone "+thisZone+"\n");
                }
                ad_shaded_zones.add(thisZone);
            }        	
        }
        AbstractDescription result = new AbstractDescription(ad_contours, ad_zones, ad_shaded_zones);
        
        // add some Spiders
        for(String spiderString: spiderStrings)
        {
        	StringTokenizer st = new StringTokenizer(spiderString); // for spaces
        	TreeSet<AbstractBasicRegion> habitat = new TreeSet<AbstractBasicRegion>();
        	String spiderLabel = null;
            while (st.hasMoreTokens()) {
                String word = st.nextToken();
                AbstractBasicRegion thisZone = null;
                if(word.charAt(0) == '\'')
                {
                	// this string represents the spider's label
                	String name = word.substring(1);
                	spiderLabel = name;
                	continue;
                }
                else if(word.equals("."))
                {
                	// this means the outside zone
                	thisZone = outsideZone;
                }
                else
                {
	                TreeSet<AbstractCurve> zoneContours = new TreeSet<AbstractCurve>();
	                for (int i = 0; i < word.length(); i++) {
	                    String character = "" + word.charAt(i);
	                    CurveLabel cl = CurveLabel.get(character);
	                    AbstractCurve ac = contours.get(cl);
	                    if(ac == null)
	                    	throw new RuntimeException("malformed diagram spec : contour "+ac+"\n");
	                    zoneContours.add(ac);
	                }
	                thisZone = AbstractBasicRegion.get(zoneContours);
                }
                if(!ad_zones.contains(thisZone))
                {
                	throw new RuntimeException("malformed diagram spec : zone "+thisZone+"\n");
                }
                habitat.add(thisZone);
            }        	
            AbstractSpider spider = new AbstractSpider(habitat, spiderLabel);
            result.addSpider(spider);        	
        }
                
        return result;
    }

    /*
     * TODO This method will not be needed after the TestCode class is converted to use the 
     * JSON input format and junit test framework.
     * 
     * Build an AbstractDescription given a list of zones (no shaded zones or spiders).
     * Initial version to allow Strings (longer than one char) as labels.
     */
    public static AbstractDescription makeForTesting(ArrayList<AbstractBasicRegion> zones) {

        TreeSet<AbstractBasicRegion> ad_zones = new TreeSet<AbstractBasicRegion>();
        AbstractBasicRegion outsideZone = AbstractBasicRegion.get(new TreeSet<AbstractCurve>());
        ad_zones.add(outsideZone);
        HashMap<CurveLabel, AbstractCurve> contours = new HashMap<CurveLabel, AbstractCurve>();
        if(zones != null)
        {
        	for(AbstractBasicRegion z: zones) {
        		TreeSet<AbstractCurve> zoneContours = new TreeSet<AbstractCurve>();
        		for(AbstractCurve c: z.m_in_set) {
        			CurveLabel cl = c.m_label;
        			if (!contours.containsKey(cl)) {
	                    contours.put(cl, c);
	                }
        			zoneContours.add(contours.get(cl));
        		}
	            ad_zones.add(z);
        	}
        }
        TreeSet<AbstractCurve> ad_contours = new TreeSet<AbstractCurve>(contours.values());
        
        // no shaded zones for now
        TreeSet<AbstractBasicRegion> ad_shaded_zones = new TreeSet<AbstractBasicRegion>();
        AbstractDescription result = new AbstractDescription(ad_contours, ad_zones, ad_shaded_zones);     
        return result;
    }
}
