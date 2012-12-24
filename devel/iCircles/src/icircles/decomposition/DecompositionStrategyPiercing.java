package icircles.decomposition;

import icircles.abstractDescription.AbstractBasicRegion;
import icircles.abstractDescription.AbstractCurve;
import icircles.abstractDescription.AbstractDescription;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

public class DecompositionStrategyPiercing extends DecompositionStrategy {

    void getContoursToRemove(AbstractDescription ad, ArrayList<AbstractCurve> toRemove) {
        toRemove.clear();
        int bestNZ = Integer.MAX_VALUE;
        Iterator<AbstractCurve> acIt = ad.getContourIterator();
        while (acIt.hasNext()) {
            AbstractCurve ac = acIt.next();
            if (isPiercingCurve(ac, ad)) {
                int nz = numZonesInside(ac, ad);
                if (nz < bestNZ) {
                    toRemove.clear();
                    toRemove.add(ac);
                    bestNZ = nz;
                } else if (nz == bestNZ) {
                    toRemove.add(ac);
                }
            }
        }
        if (toRemove.size() == 0) {
            acIt = ad.getContourIterator();
            while (acIt.hasNext()) {
                AbstractCurve ac = acIt.next();
                int nz = numZonesInside(ac, ad);
                if (nz < bestNZ) {
                    toRemove.clear();
                    toRemove.add(ac);
                    bestNZ = nz;
                } else if (nz == bestNZ) {
                    toRemove.add(ac);
                }
            }
        }
    }

    private int numZonesInside(AbstractCurve ac,
            AbstractDescription ad) {
        int nz = 0;

        Iterator<AbstractBasicRegion> abrit = ad.getZoneIterator();
        while (abrit.hasNext()) {
            AbstractBasicRegion abr = abrit.next();
            if (abr.is_in(ac)) {
                nz++;
            }
        }
        return nz;
    }

    private boolean isPiercingCurve(AbstractCurve ac,
            AbstractDescription ad) {
        // every abstract basic region in ad which is in ac
        // must have a corresponding abr which is not in ac
        Iterator<AbstractBasicRegion> abrit = ad.getZoneIterator();
        ArrayList<AbstractBasicRegion> zonesInContour =
                new ArrayList<AbstractBasicRegion>();

        abrLoop:
        while (abrit.hasNext()) {
            AbstractBasicRegion abr = abrit.next();
            if (abr.is_in(ac)) {
                zonesInContour.add(abr);
                // look for a partner zone
                Iterator<AbstractBasicRegion> abrit2 = ad.getZoneIterator();
                while (abrit2.hasNext()) {
                    AbstractBasicRegion abr2 = abrit2.next();
                    if (abr.getStraddledContour(abr2) == ac) {
                        continue abrLoop;
                    }
                }
                // never found a partner zone
                return false;
            }
        }
        // check that the zones in C form a cluster - we need 2^n zones
        int power = powerOfTwo(zonesInContour.size());
        if (power < 0) {
            return false;
        }

        // find the smallest zone (one in fewest contours)
        int zoneSize = Integer.MAX_VALUE;
        AbstractBasicRegion smallestZone = null;
        abrit = zonesInContour.iterator();
        while (abrit.hasNext()) {
            AbstractBasicRegion abr = abrit.next();
            int numCs = abr.getNumContours();
            if (numCs < zoneSize) {
                zoneSize = numCs;
                smallestZone = abr;
            }
        }
        // every other zone in ac must be a superset of that zone
        abrit = zonesInContour.iterator();
        while (abrit.hasNext()) {
            AbstractBasicRegion abr = abrit.next();
            Iterator<AbstractCurve> acIt = smallestZone.getContourIterator();
            while (acIt.hasNext()) {
                AbstractCurve ac2 = acIt.next();
                if (!abr.is_in(ac2)) {
                    return false;
                }
            }
        }
        // We have 2^n zones which are all supersets of smallestZone.
        // Check that they use exactly n contours from smallestZone.
        TreeSet<AbstractCurve> addedContours = new TreeSet<AbstractCurve>();
        abrit = zonesInContour.iterator();
        while (abrit.hasNext()) {
            AbstractBasicRegion abr = abrit.next();
            Iterator<AbstractCurve> acIt = abr.getContourIterator();
            while (acIt.hasNext()) {
                AbstractCurve ac2 = acIt.next();
                if (!smallestZone.is_in(ac2)) {
                    addedContours.add(ac2);
                    if (addedContours.size() > power) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * For all non-zero positive values, find c where n=2^c.
     * Using maxima we can solve for c (you could do it manually too :) ) 
     * (%i1) n=2^c;
     *                                          c
     * (%o1)                               n = 2
     * (%i2) solve(%, c);
     *                                      log(n)
     * (%o2)                            [c = ------]
     *                                      log(2)
     * We calculate c and return it if n = 2^c.
     * 
     * Note: this implementation fixes several bugs in the previous code,
     * particularly an infinite while loop when n=0 was passed to the function.
     * The function is now total over Ints.
     * 
     * @param n
     * @return c where n=2^c, -1 otherwise.
     */
    private int powerOfTwo(int n) // return result where  n = 2^(result)
    {
        if(n <= 0)
            return -1;

        int c = (int) (Math.log(n)/Math.log(2));

        if(Math.pow(2,c) == n)
            return c;
        else
            return -1;
    }
}
