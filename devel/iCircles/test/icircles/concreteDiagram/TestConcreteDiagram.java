package icircles.concreteDiagram;

import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.jcheck.annotations.Configuration;
import org.jcheck.annotations.Generator;

import icircles.abstractDescription.*;
import icircles.concreteDiagram.*;
import icircles.test.*;
import icircles.util.CannotDrawException;

public class TestConcreteDiagram {
	
	private final int diagramSize = 100;
	
	@Test
	public void testAllDiagrams() {
		
		for(TestDatum td : TestData.test_data) {
			AbstractDescription ad = AbstractDescription.makeForTesting(td.description);
			DiagramCreator      dc = new DiagramCreator(ad);
			
			try {
				ConcreteDiagram cd = dc.createDiagram(diagramSize);
				assertEquals(td.expected_checksum, cd.checksum(), 0.0);
			} catch (CannotDrawException cde) {
				// The expected result of a CannotDrawException is hardcoded as 0.0
				assertEquals(td.expected_checksum, 0.0, 0.0);
			}
		}
		
	}
	
	

}
