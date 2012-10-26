package icircles.concreteDiagram;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.svg.SVGDocument;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.*;

import junit.framework.AssertionFailedError;

import org.junit.*;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;

import icircles.abstractDescription.*;
import icircles.concreteDiagram.*;
import icircles.gui.CirclesSVGGenerator;
import icircles.test.*;
import icircles.util.CannotDrawException;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

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
		 //for(int i = 0; i <= 200; i++)
		 for(TestDatum td : TestData.test_data) {
			 v.add(new TestDatum[]{td});
		 }
	   return v;
	 }
	 
	@Test
	public void testAllDiagrams() {
		AbstractDescription ad = AbstractDescription.makeForTesting(datum.description);
		DiagramCreator      dc = new DiagramCreator(ad);
		
		// Don't simply report an assertion failure and exit, use the ErrorCollector
		// to record all errors.  We'll deal with them later.
		try {
			currentDiagram = dc.createDiagram(diagramSize);
			collector.checkThat("checksum", datum.expected_checksum, is(currentDiagram.checksum()));
		} catch (CannotDrawException cde) {
			// The expected result of a CannotDrawException is hardcoded as 0.0
			collector.checkThat("checksum", datum.expected_checksum, is(0.0));
		}
	}
}
