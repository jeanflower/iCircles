package icircles.recomposition;

import icircles.abstractDescription.AbstractBasicRegion;

import java.util.Comparator;

public class ABRComparator implements Comparator<AbstractBasicRegion> {

    public int compare(AbstractBasicRegion o1, AbstractBasicRegion o2) {
        return o1.compareTo(o2);
    }
}
