package icircles.input;

import java.io.IOException;

import org.junit.*;
import static org.junit.Assert.*;
import com.fasterxml.jackson.databind.*;

/**
 * 
 * @author Aidan Delaney <aidan@phoric.eu>
 *
 */
//@RunWith(org.jcheck.runners.JCheckRunner.class)
public class TestInputParser {

    @Test(expected = JsonMappingException.class)
    public void testNullDiagram () throws JsonMappingException {
        String          d  = "{\"AbstractDiagram\" : {}}";
        ObjectMapper    m  = new ObjectMapper();
        AbstractDiagram ad = null;
        
        try {
            ad = m.readValue(d, AbstractDiagram.class);
        } catch (JsonMappingException jme) {
            throw jme;
        } catch (IOException e) {
            
        }
        ad.getClass(); // keeps the compiler happy if we use ad for something
        
        fail();
    }
    
    @Test
    public void testEmptyDiagram () {
        String          d  = "{\"AbstractDiagram\" : {\"Version\" : 0, \"Contours\" : [], "
                           + "\"Zones\" : [], \"ShadedZones\" : [], \"Spiders\" : [] }}";
        ObjectMapper    m  = new ObjectMapper();
        AbstractDiagram ad = null;
        
        try {
            ad = m.readValue(d, AbstractDiagram.class);
        } catch (Exception e) {
            fail();
        }

        AbstractDiagram expected = new AbstractDiagram(0, new String[]{}, 
        		                  new Zone[]{}, new Zone[]{}, new Spider[]{});
        assertEquals(expected, ad);
    }
    
    /*
    @Test
    @Configuration(tests=100)
    @Generator(klass=AbstractDiagram.class, generator=CustomAbstractDiagram.class)
    public void testParse () {
        
    }
    */
    
    @Test(expected = JsonMappingException.class)
    public void testImproperlyDefinedZone () throws JsonMappingException {
        // d describes a diagram with no contours but contaiing a zone.
        String          d  = "{\"AbstractDiagram\" : {\"Version\" : 0, \"Contours\" : [], "
                           + "\"Zones\" : [{\"in\" : [\"a\"]}], \"ShadedZones\" : [], \"Spiders\" : [] }}";
        ObjectMapper    m  = new ObjectMapper();
        AbstractDiagram ad = null;
        
        try {
            ad = m.readValue(d, AbstractDiagram.class);
        } catch (JsonMappingException jme) {
            throw jme;
        } catch (Exception e) {
        	// this is what's supposed to happen
        }
        // this line should not happen
        ad.getClass(); // keeps the compiler happy if we use ad for something

        fail();
    }
    
    @Test(expected = JsonMappingException.class)
    public void testImproperlyDefinedShadedZone () throws JsonMappingException {
        // d describes a diagram with contours, zones but a shaded zone that
        // doesn't exist.
        String          d  = "{\"AbstractDiagram\" : {\"Version\" : 0, \"Contours\" : [\"a\"], \"Zones\" : [{\"in\" : [\"\"]}], \"ShadedZones\" : [{\"in\" : [\"a\"]}], \"Spiders\" : [] }}";
        ObjectMapper    m  = new ObjectMapper();
        AbstractDiagram ad = null;
        
        try {
            ad = m.readValue(d, AbstractDiagram.class);
        } catch (JsonMappingException jme) {
            throw jme;
        } catch (Exception e) {
        	// this is what's supposed to happen
        }
        // this line should not happen
        ad.getClass(); // keeps the compiler happy if we use ad for something

        fail();
    }
    
    @Test(expected=JsonMappingException.class)
    public void testImproperlyDefinedSpider () throws JsonMappingException {
        // d describes a diagram with contours, zones but a shaded zone that
        // doesn't exist.
        String          d  = "{\"AbstractDiagram\" : {\"Version\" : 0, \"Contours\"    : [\"a\"], "
                           + "\"Zones\"       : [{\"in\" : [\"\"]}],"
                           + "\"ShadedZones\" : [],"
                           + "\"Spiders\"     : [{\"name\" : \"s1\", \"habitat\" : [{\"in\" : [\"b\"]}]}]"
                           + "}}";
        ObjectMapper    m  = new ObjectMapper();
        AbstractDiagram ad = null;
        
        try {
            ad = m.readValue(d, AbstractDiagram.class);
        } catch (JsonMappingException jme) {
            throw jme;
        } catch (Exception e) {
        	// this is what's supposed to happen
        }
        // this line should not happen
        ad.getClass(); // keeps the compiler happy if we use ad for something

        fail();
    }
    
    @Test
    public void testProperlyDefinedSpider () {
        // d describes a diagram with contours, zones but a spider that
    	// is not properly defined
        String          d  = "{\"AbstractDiagram\" : {\"Version\" : 0, \"Contours\"    : [\"a\"], "
                           + "\"Zones\"       : [{\"in\" : [\"\"]}],"
                           + "\"ShadedZones\" : [],"
                           + "\"Spiders\"     : [{\"name\" : \"s1\", \"habitat\" : [{\"in\" : [\"\"]}]}]"
                           + "}}";
        ObjectMapper    m  = new ObjectMapper();
        AbstractDiagram ad = null;
        
        try {
            ad = m.readValue(d, AbstractDiagram.class);
        } catch (JsonMappingException jme) {
            fail("Creation of diagram from JSON string failed, string = " + d);
        } catch (Exception e) {
            fail();
        }

        assertNotNull(ad);
    }
}