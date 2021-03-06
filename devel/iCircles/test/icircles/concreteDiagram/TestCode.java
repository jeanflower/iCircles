package icircles.concreteDiagram;

import icircles.gui.CirclesPanel;
import icircles.input.AbstractDiagram;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.fasterxml.jackson.databind.ObjectMapper;

import icircles.concreteDiagram.ConcreteDiagram;
import icircles.concreteDiagram.DiagramCreator;

import icircles.util.CannotDrawException;
import icircles.util.DEB;

import icircles.abstractDescription.AbstractCurve;
import icircles.abstractDescription.AbstractDescription;

import com.fasterxml.jackson.core.JsonParser;

/*
 * TODO Convert this class to use the JSON input format and junit test framework.
 */
public class TestCode {

    public static void main(String args[]) {
        DEB.level = TestData.TEST_DEBUG_LEVEL;
        if (TestData.TASK == TestData.RUN_ALL_TESTS) {
            ArrayList<Integer> failures = runAllTests();
            if (!TestData.GENERATE_ALL_TEST_DATA) {
                System.out.println("******************");
                if (failures.isEmpty()) {
                    System.out.println("**** all pass ****");
                } else {
                    System.out.println("**** failures ****" + failures);
                }
                // a failure means something behaves different from baseline
                System.out.println("******************");
            }
        } else if (TestData.TASK == TestData.RUN_TEST_LIST) {
            runTestList();
        } else if (TestData.TASK == TestData.VIEW_TEST_LIST) {
            viewTestList();
        } else if (TestData.TASK == TestData.VIEW_ALL_TESTS) {
            viewAllTests();
        }
    }

    public static ArrayList<Integer> runAllTests() {
        int num_tests = TestData.test_data.length;
        int[] testlist = new int[num_tests];
        for (int i = 0; i < num_tests; i++) {
            testlist[i] = i;
        }

        return do_testlist(testlist,
                true, // run
                false // view
                );
    }

    public static ArrayList<Integer> viewAllTests() {
    	/*
    	 * this block draws all the test data (many diagrams!)
    	 */
    	int[] testlist = {};
		int num_tests = 0;
    	if(TestData.TEST_EULER_THREE)
    	{
    		num_tests = 39;
        } 
    	else
        {
    		num_tests = TestData.test_data.length;
        }
        testlist = new int[num_tests];
        for (int i = 0; i < num_tests; i++) 
            testlist[i] = i;

        return do_testlist(testlist,
                false, // run
                true // view
                );
    }

    public static ArrayList<Integer> runTestList() {
        return do_testlist(TestData.test_list,
                true, // run
                false/*, // view
                false*/ // compare
                );
    }

    public static ArrayList<Integer> viewTestList() {
        return do_testlist(TestData.test_list,
                false, // run
                true // view
                );
    }

    public static void compareTestList() {
        do_testlist(TestData.test_list,
                false, // run
                false // view
                );
    }
    private static double CHECKSUM_TOL = 0.001;
    private static double CHECKSUM_FOR_UNDRAWABLE = 99;
    private static double CHECKSUM_FOR_NaN = 111;

    private static ArrayList<Integer> do_testlist(int[] testlist,
            boolean do_run,
            boolean do_view) {
        ArrayList<Integer> result = new ArrayList<Integer>();

        JPanel gridPanel = new JPanel();
        gridPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        //c.fill = GridBagConstraints.NONE;
        int num_failures = 0;
        for (int i = 0; i < testlist.length; i++) {
            int test_num = testlist[i];
            if (test_num < 0 || test_num >= TestData.test_data.length) {
                System.out.println("invalid test number : must be between 0 and "
                        + (TestData.test_data.length - 1));
                return result;
            } else {
                if (do_run) {
                	int size = 0;
                	if(TestData.GENERATE_ALL_TEST_DATA)
                		size = TestData.TEST_PANEL_SIZE;
                	else
                		size = TestData.VIEW_PANEL_SIZE;
                	
                    if (!run_test(test_num, false, false,size))
                        {
                        result.add(new Integer(test_num));
                        num_failures++;
                        if (TestData.DO_VIEW_FAILURES) {
                            run_test(test_num, true, true,
                                    TestData.FAIL_VIEW_PANEL_SIZE);
                        }
                    }
                }
                if (do_view) {
                    JPanel jp = get_view_of_test(test_num, "",
                            TestData.VIEW_PANEL_SIZE);
                    c.gridx = i % TestData.GRID_WIDTH;
                    c.gridy = (int) (i / TestData.GRID_WIDTH);
                    c.insets = new Insets(2, 2, 2, 2);

                    gridPanel.add(jp, c);
                }
            }
        }
        if (do_view) {
            JFrame jf = new JFrame("circle tests");

            JScrollPane scrollpane = new JScrollPane(gridPanel);
            jf.getContentPane().add(scrollpane, BorderLayout.CENTER);
            scrollpane.getViewport().add(gridPanel);
            jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            jf.pack();
            jf.setVisible(true);
        }

        if (do_run) {
            System.out.println("Got " + num_failures + " failures from " + testlist.length + " test cases");
        }
        return result;
    }

//	private static boolean sleep(int time) 
//		{
//		try 
//			{
//			Thread.sleep(time);
//			} 
//		catch(Exception e) 
//			{
//			System.out.println("Exception occurred in Thread.sleep() "+e);
//			e.printStackTrace();
//			return false;
//			}
//		return true;
//		}
    private static boolean within_tol(double expected, double found) {
        if (isNaN(expected) && isNaN(found)) {
            return true;
        } else if (isNaN(expected)) {
            return false;
        } else if (isNaN(found)) {
            return false;
        } else {
            return Math.abs(expected - found) < CHECKSUM_TOL;
        }
    }

    private static boolean isNaN(double x) {
        return (x == CHECKSUM_FOR_NaN) || (!(x > 1.0) && !(x < 2.0));
    }

    private static String messageFrom(double checksum) {
        if (isNaN(checksum)) {
            return "crash";
        } else if (checksum == CHECKSUM_FOR_UNDRAWABLE) {
            return "undrawable";
        } else {
            return "" + checksum;
        }
    }

    private static void describe_result(String outcome,
            int test_num, double expected_checksum, double found_checksum) {
        System.out.println("test " + outcome + " : test " + test_num + " expected "
                + messageFrom(expected_checksum) + " and got "
                + messageFrom(found_checksum));
    }

    private static boolean run_test(int test_num, // returns false if test fails
            boolean generate_fresh_test_data,
            boolean view_failure,
            int size) {
        // clear the static id counter for abstract curves
        if (!view_failure) {
            AbstractCurve.reset_id_counter();
            //AbstractBasicRegion.clearLibrary();
            //CurveLabel.clearLibrary();
        }
        String desc = TestData.test_data[test_num].description;
                
        if (DEB.level > 0) {
            System.out.println("test desc:" + desc);
        }
        ConcreteDiagram cd = null;
        try {
            cd = getDiagram(test_num, 100); // fixed size for checksumming
            cd.setFont(TestData.font);
        } catch (CannotDrawException x) {
            // suppress
        }

        double checksum_found = cd == null ? 0.0 : cd.checksum();

        double baseline = TestData.test_data[test_num].expected_checksum;

        if (TestData.GENERATE_ALL_TEST_DATA) {
            printFreshTestData(test_num, checksum_found);
            return within_tol(checksum_found, baseline);
        } else if (!within_tol(checksum_found, baseline)) {
            // test failure
            if (generate_fresh_test_data) {
                printFreshTestData(test_num, checksum_found);
            } else {
                describe_result("fails", test_num, baseline, checksum_found);
            }
            if (view_failure) {
                JPanel jp = get_view_of_test(test_num, "", size);
                JFrame jf = new JFrame("failing test " + test_num);
                jf.getContentPane().add(jp);
                jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                jf.pack();
                jf.setVisible(true);
            }
            return false;
        } else {
            // test passes
            describe_result("passes", test_num, baseline, checksum_found);
            return true;
        }
    }

    private static JPanel get_view_of_test(int test_num, String for_title,
            int size) {
        String desc = TestData.test_data[test_num].description;        
        System.out.println("drawing test " + test_num + " description " + desc);

        ConcreteDiagram cd = null;
        String failureMessage = null;
        try 
        {
            cd = getDiagram(test_num, size);
            cd.setFont(TestData.font);
        } catch (CannotDrawException x) {
            failureMessage = x.message;
        }
        String description = "" + test_num + "." + desc;        
        if (description.length() > 36) {
            description = "" + test_num + ".description..";
        }
       
        CirclesPanel cp;
        if(cd != null)
        	{
        	cp = new CirclesPanel(description, failureMessage, cd,
                true);// do use colours
        	}
        else
	    	{
	    	cp = new CirclesPanel(description, failureMessage, size);
	    	}
        	
        cp.setScaleFactor(TestData.scale);
        return cp;
    }
//	static Rectangle getBoundingBox(ConstructedConcreteDiagram ccd)
//	{
//	int minX = Integer.MAX_VALUE;
//	int maxX = Integer.MIN_VALUE;
//	int minY = Integer.MAX_VALUE;
//	int maxY = Integer.MIN_VALUE;
//	
//	for(ConcreteContour cc : ccd.getConcreteContours()){
//		RegularPolygon rp = cc.getCircle();
//		int lowX = rp.getCentreX() - rp.getRadius();
//		int higX = rp.getCentreX() + rp.getRadius();
//		int lowY = rp.getCentreY() - rp.getRadius();
//		int higY = rp.getCentreY() + rp.getRadius();
//		if(lowX < minX) minX = lowX;
//		if(higX > maxX) maxX = higX;
//		if(lowY < minY) minY = lowY;
//		if(higY > maxY) maxY = higY;
//	}
//	return new Rectangle(minX, minY, maxX - minX, maxY - minY);
//	}
//	static ConstructedConcreteDiagram 
//			transform(ConstructedConcreteDiagram ccd, 
//						double xstep, double ystep, double scale,
//						String desc)
//	{
//		ArrayList<ConcreteContour> newConts = new ArrayList<ConcreteContour>();
//		for(ConcreteContour cc : ccd.getConcreteContours()){
//			RegularPolygon rp = cc.getCircle();
//			RegularPolygon newrp = new RegularPolygon(
//					(int)((rp.getCentreX()+xstep)*scale),
//					(int)((rp.getCentreY()+ystep)*scale),
//					(int)(rp.getRadius()*scale),50);
//			ConcreteContour newcc = 
//				new ConcreteContour(cc.getAbstractContour(), newrp);
//			newConts.add(newcc);
//		}
//		ConstructedConcreteDiagram result = 
//			new ConstructedConcreteDiagram(desc, newConts);
//		return result;
//	}

    private static ConcreteDiagram getDiagram(int test_num,
            int size) throws CannotDrawException {

    	String json_desc = TestData.test_data[test_num].toJSON();
        ObjectMapper        m  = new ObjectMapper();
        m.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        AbstractDiagram adiag = null;
        try {
            adiag = m.readValue(json_desc, AbstractDiagram.class);
        } catch (IOException e) { // JsonParseException | JsonMappingException
            e.printStackTrace();
            throw new CannotDrawException("cannot understand string");
        }
        
    	AbstractCurve.reset_id_counter();
        
        AbstractDescription ad = adiag.toAbstractDescription();
        DiagramCreator dc = new DiagramCreator(ad);
        ConcreteDiagram cd = dc.createDiagram(size);
        return cd;
    }
    
    private static void printFreshTestData(int test_num, double checksum_found) {
        //int decomp_strategy = TestData.test_data[test_num].decomp_strategy;
        //int recomp_strategy = TestData.test_data[test_num].recomp_strategy;
        String desc = TestData.test_data[test_num].description;

        System.out.println("/*" + test_num
                + "*/new TestDatum( \""
                + desc + "\", "
                //+ "DecompositionStrategy."
                //+ DecompositionStrategy.text_for(decomp_strategy) + ", "
                //+ "RecompositionStrategy."
                //+ RecompositionStrategy.text_for(recomp_strategy) + ", "
                + (isNaN(checksum_found) ? 111 : checksum_found) + "),");

    }
}
