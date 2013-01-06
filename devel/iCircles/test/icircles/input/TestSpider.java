package icircles.input;

import java.util.Set;
import java.util.HashSet;

import org.junit.*;
import static org.junit.Assert.*;


public class TestSpider {

    @Test
    public void testVerify () {
        Set <String> cs = new HashSet<String>();
        cs.add("");
        Zone         z  = new Zone(cs.toArray(new String[]{}));
        
        Set <String> c2 = new HashSet<String>();
        Set <Zone>   zs = new HashSet<Zone>();
        zs.add(new Zone(c2.toArray(new String[]{})));
        Spider       s  = new Spider("s1", zs.toArray(new Zone[]{}));

        assertTrue(s.verify(zs));
    }
}
