package icircles.input;

import java.util.Set;
import java.util.HashSet;

import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;


public class TestSpider {

    @Test
    public void testVerify () {
        Set <String> cs = new HashSet<String>();
        cs.add("");
        Zone         z  = new Zone(cs);
        
        Set <String> c2 = new HashSet<String>();
        Set <Zone>   zs = new HashSet<Zone>();
        zs.add(new Zone(c2));
        Spider       s  = new Spider("s1", zs);

        assertTrue(s.verify(zs));
    }
}
