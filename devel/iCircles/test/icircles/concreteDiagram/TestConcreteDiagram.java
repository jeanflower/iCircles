package icircles.concreteDiagram;

import org.junit.*;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;

import icircles.abstractDescription.*;
import icircles.concreteDiagram.*;
import icircles.test.*;
import icircles.util.CannotDrawException;

public class TestConcreteDiagram {
	
	private final int diagramSize = 100;
	
	@Rule
	public ErrorCollector collector= new ErrorCollector();
	
	@Test
	public void testAllDiagrams() {
		
		for(TestDatum td : TestData.test_data) {
			AbstractDescription ad = AbstractDescription.makeForTesting(td.description);
			DiagramCreator      dc = new DiagramCreator(ad);
			
			// Don't simply report an assertion failure and exit, use the ErrorCollector
			// to record all errors.  We'll deal with them later.
			try {
				ConcreteDiagram cd = dc.createDiagram(diagramSize);
				collector.checkThat("checksum", td.expected_checksum, is(cd.checksum()));
			} catch (CannotDrawException cde) {
				// The expected result of a CannotDrawException is hardcoded as 0.0
				collector.checkThat("checksum", td.expected_checksum, is(0.0));
			}
		}
	}
	
	@After
	public void drawFailedDiagrams() {
		
	}

}
