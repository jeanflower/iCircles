package icircles.abstractDescription;

/*
 *
 * @author Aidan Delaney <aidan@phoric.eu>
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

import java.util.TreeSet;

import java.lang.reflect.Constructor;

import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for {@link AbstractDescription}.
 */
public class TestAbstractDescription {

    @Test
    public void testADSystemTests()
    {

        CurveLabel a = CurveLabel.get("a");
        CurveLabel a2 = CurveLabel.get("a");

        /*
         * Debug.level = 2;
         * System.out.println("contour labels equal? "+a.debug()+","+a2.debug());
         * System.out.println("contour labels equal? "+(a==a2));
         *
         * replaced by
         */
        assertEquals(a, a2);


        AbstractCurve ca1 = new AbstractCurve(a);
        AbstractCurve ca2 = new AbstractCurve(a);

        /*
         * System.out.println("contours equal? "+a.debug()+","+a2.debug());
         * System.out.println("contours equal? "+(a==a2));
         *
         * replaced by
         */
        // should not be equal as their internal curve id differs
        assertThat(ca1, is(not(ca2)));

        TreeSet<AbstractCurve> ts = new TreeSet<AbstractCurve>();
        AbstractBasicRegion z0 = AbstractBasicRegion.get(ts);

        /*
         * System.out.println("outside zone "+z0.debug());
         *
         * replaced by
         */
        // break private scope of AbstractBasicRegion constructor for this test
        try {
            Constructor<AbstractBasicRegion> constructor = AbstractBasicRegion.class.getDeclaredConstructor(new Class[0]);
            constructor.setAccessible(true);
            AbstractBasicRegion abc = constructor.newInstance(ts);
            assertEquals(abc, z0);
        } catch (Exception e) {
            // do nothing
        }

        ts.add(ca1);
        AbstractBasicRegion za = AbstractBasicRegion.get(ts);

        AbstractBasicRegion za2;
        {
            TreeSet<AbstractCurve> ts2 = new TreeSet<AbstractCurve>();
            ts2.add(ca2);
            za2 = AbstractBasicRegion.get(ts2);
            /*
             * System.out.println("za==za2 ?" + (za == za2));
             *
             * replaced by
             */
            assertThat(za, is(not(za2)));
        }

        CurveLabel b = CurveLabel.get("b");
        AbstractCurve cb = new AbstractCurve(b);
        ts.add(cb);
        AbstractBasicRegion zab = AbstractBasicRegion.get(ts);
        //System.out.println("zone in ab "+zab.debug());

        ts.remove(ca1);
        /*AbstractBasicRegion zb = */AbstractBasicRegion.get(ts);
        //System.out.println("zone in b "+zb.debug());

        ts.add(ca1);
        AbstractBasicRegion zab2 = AbstractBasicRegion.get(ts);
        //System.out.println("zone2 in ab "+zab2.debug());

        /*
         * System.out.println("zab==zab2 ?" + (zab == zab2));
         *
         * replaced by
         */
        assertEquals(zab, zab2);
    }
    /*
    // The following pre-existing system tests have not been reformulated in
    // JUnit as I cannot see where any assertions are made.
    // TODO: reformulate these tests in JUnit

    ts.clear();
    TreeSet<AbstractBasicRegion> tsz = new TreeSet<AbstractBasicRegion>();

    debug_abstract_description(ts, tsz);

    ts.add(ca1);
    debug_abstract_description(ts, tsz);

    ts.add(ca1);
    debug_abstract_description(ts, tsz);

    ts.add(ca2);
    debug_abstract_description(ts, tsz);

    ts.add(cb);
    debug_abstract_description(ts, tsz);

    tsz.add(z0);
    debug_abstract_description(ts, tsz);

    tsz.add(za);
    debug_abstract_description(ts, tsz);

    tsz.add(zab);
    debug_abstract_description(ts, tsz);

    tsz.add(zb);
    debug_abstract_description(ts, tsz);

    //ContourLabel c = ContourLabel.get("c");
    //ContourLabel d = ContourLabel.get("d");
    //ContourLabel e = ContourLabel.get("e");

    System.out.println("\"\" is " + makeForTesting("").debug());
    System.out.println("\"a\" is " + makeForTesting("a").debug());
    System.out.println("\"a a\" is " + makeForTesting("a a").debug());
    System.out.println("\"a ab\" is " + makeForTesting("a ab").debug());

    }
    private static void debug_abstract_description(
                           TreeSet<AbstractCurve> ts, TreeSet<AbstractBasicRegion> tsz)
    {
    AbstractDescription ad = new AbstractDescription(ts, tsz);
    System.out.println("ad is " + ad.debug());
    }

    Set<AbstractBasicRegion> m_zones;
    Set<AbstractSpider> m_spiders;
    Set<AbstractBasicRegion> m_shaded_zones;



     */
}
