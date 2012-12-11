package icircles.input;

import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;

import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import icircles.abstractDescription.AbstractCurve;
import icircles.abstractDescription.CustomAbstractCurveGen;

import org.jcheck.annotations.Configuration;
import org.jcheck.annotations.Generator;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.JsonParseException;
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
        AbstractDiagram ad;
        
        try {
            ad = m.readValue(d, AbstractDiagram.class);
        } catch (JsonMappingException jme) {
            throw jme;
        } catch (IOException e) {
            
        }
        
        fail();
    }
    
    @Test
    public void testEmptyDiagram () {
        String          d  = "{\"AbstractDiagram\" : {\"Version\" : 0, \"Contours\" : [], \"Zones\" : [], \"ShadedZones\" : [], \"Spiders\" : [] }}";
        ObjectMapper    m  = new ObjectMapper();
        AbstractDiagram ad = null;
        
        try {
            ad = m.readValue(d, AbstractDiagram.class);
        } catch (Exception e) {
            fail();
        }

        AbstractDiagram expected = new AbstractDiagram(0, new String[]{}, new Zone[]{}, new Zone[]{}, new Spider[]{});
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
        String          d  = "{\"AbstractDiagram\" : {\"Version\" : 0, \"Contours\" : [], \"Zones\" : [{\"in\" : [\"a\"]}], \"ShadedZones\" : [], \"Spiders\" : [] }}";
        ObjectMapper    m  = new ObjectMapper();
        AbstractDiagram ad;
        
        try {
            ad = m.readValue(d, AbstractDiagram.class);
        } catch (JsonMappingException jme) {
            throw jme;
        } catch (Exception e) {
        }

        fail();
    }
    
    @Test(expected = JsonMappingException.class)
    public void testImproperlyDefinedShadedZone () throws JsonMappingException {
        // d describes a diagram with contours, zones but a shaded zone that
        // doesn't exist.
        String          d  = "{\"AbstractDiagram\" : {\"Version\" : 0, \"Contours\" : [\"a\"], \"Zones\" : [{\"in\" : [\"\"]}], \"ShadedZones\" : [{\"in\" : [\"a\"]}], \"Spiders\" : [] }}";
        ObjectMapper    m  = new ObjectMapper();
        AbstractDiagram ad;
        
        try {
            ad = m.readValue(d, AbstractDiagram.class);
        } catch (JsonMappingException jme) {
            throw jme;
        } catch (Exception e) {
        }

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
        AbstractDiagram ad;
        
        try {
            ad = m.readValue(d, AbstractDiagram.class);
        } catch (JsonMappingException jme) {
            throw jme;
        } catch (Exception e) {
        }

        fail();
    }
    
    @Test
    public void testProperlyDefinedSpider () {
        // d describes a diagram with contours, zones but a shaded zone that
        // doesn't exist.
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