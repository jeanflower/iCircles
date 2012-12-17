package icircles.concreteDiagram;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import icircles.gui.CirclesSVGGenerator;
import icircles.input.AbstractDiagram;
import icircles.util.CannotDrawException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(value = Parameterized.class)
public class TestConcreteDiagram {
    private ConcreteDiagram       currentDiagram;
    
    public class DiagramCollector extends TestWatcher {
        @Override
        protected void failed(Throwable e, Description description) {
            // draw failed diagram unless it cannot be drawn
            if(null != currentDiagram) {
                CirclesSVGGenerator csg = new CirclesSVGGenerator(currentDiagram);
                
                try {
                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    Result output = new StreamResult(new File(currentDiagram.checksum() + ".svg"));
                    Source input = new DOMSource(csg.toSVG());

                    transformer.transform(input, output);
                } catch (Exception ex) {
                    // do nothing with IOExceptions or ConfigurationExceptions etc...
                }
            }
        }
    }
    
    private final int diagramSize = 100;
    private TestDatum datum;
    
    @Rule
    public ErrorCollector collector = new ErrorCollector();
    
    @Rule
    public DiagramCollector dcollector = new DiagramCollector();
    
    public TestConcreteDiagram (TestDatum datum) {
        this.datum = datum;
    }
    
    @Parameters
     public static Collection<TestDatum[]> data() {
         Vector<TestDatum[]> v = new Vector<TestDatum[]>();

         // To get a range, do this...there's too much plumbing in implementing
         // a range Iterator type.
         //for(int i = 176; i <= 176; i++) {
         //    TestDatum td = icircles.test.TestData.test_data[i];
         for(TestDatum td : TestData.test_data) {
             v.add(new TestDatum[]{ new TestDatum(td.toJSON(), td.expected_checksum)});
         }
       return v;
     }
     
    @Test
    public void testAllDiagrams() {
        ObjectMapper        m  = new ObjectMapper();
        m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        AbstractDiagram ad = null;
        try {
            ad = m.readValue(datum.description, AbstractDiagram.class);
        } catch (IOException e) { // JsonParseException | JsonMappingException
            e.printStackTrace();
            assertTrue(false);
        }
        DiagramCreator      dc = new DiagramCreator(ad.toAbstractDescription());
        
        // Don't simply report an assertion failure and exit, use the ErrorCollector
        // to record all errors.  We'll deal with them later.
        try {
            currentDiagram = dc.createDiagram(diagramSize);
            collector.checkThat("checksum", datum.expected_checksum, closeTo(currentDiagram.checksum(), 0.00001));
        } catch (CannotDrawException cde) {
            // The expected result of a CannotDrawException is hardcoded as 0.0
            collector.checkThat("checksum", datum.expected_checksum, is(0.0));
        }
    }
}
