package icircles.concreteDiagram;

import icircles.abstractDescription.AbstractBasicRegion;
import icircles.abstractDescription.AbstractCurve;
import icircles.abstractDescription.AbstractDescription;
import icircles.abstractDescription.AbstractSpider;
import icircles.decomposition.Decomposer;
import icircles.decomposition.DecompositionStep;
import icircles.decomposition.DecompositionStrategy;
import icircles.gui.CirclesPanel;
import icircles.recomposition.RecompData;
import icircles.recomposition.Recomposer;
import icircles.recomposition.RecompositionStep;
import icircles.recomposition.RecompositionStrategy;
import icircles.util.CannotDrawException;
import icircles.util.Colors;
import icircles.util.DEB;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * A class which will provide an angle between 0 and 2pi.  For example,
 * when fitting a single-piercing around part of an already-drawn circle,
 * we could get an angle from this iterator and try putting the center of
 * the piercing circle at that position.  To try more positions, add more
 * potential angles to this iterator.  The order in which positions are
 * attempted is determined by the order in which this iterator generates 
 * possible angles.
 */
class AngleIterator {

    private int[] ints = {0, 8, 4, 12, 2, 6, 10, 14, 1, 3, 5, 7, 9, 11, 13, 15};
    private int index = -1;

    public AngleIterator() {
    }

    public boolean hasNext() {
        return index < ints.length - 1;
    }

    public double nextAngle() {
        index++;
        int mod_index = (index % ints.length);
        double angle = Math.PI * 2 * ints[mod_index] / (1.0 * ints.length);
        return angle;
    }
}

/**
 * An interface to provide a rough idea of sizes for zones (AbstractBasicRegions)
 * and contours (AbstractCurves), beginning with an AbstractDescription and some
 * recomposition steps (these determine the order in which we will (re)construct 
 * the concrete diagram).  The get methods should return big numbers for zones 
 * and contours which ought to be drawn large to accommodate other diagram features.  
 *
 */
interface GuideSizeStrategy{
    public double getGuideSize(AbstractCurve ac);
    public Set<AbstractBasicRegion> getScoredZones();
    public double getGuideSize(AbstractBasicRegion abr);
}

class JeansGuideSizeStrategy implements GuideSizeStrategy{
    // output
    private HashMap<AbstractCurve, Double> guideSizes;
    private HashMap<AbstractBasicRegion, Double> zoneScores;
    private HashMap<AbstractCurve, Double> containedZoneScores;	

    public JeansGuideSizeStrategy( AbstractDescription ad, 
                                   ArrayList<RecompositionStep> recompSteps ){
        guideSizes = new HashMap<AbstractCurve, Double>();
        zoneScores = new HashMap<AbstractBasicRegion, Double>();
        containedZoneScores = new HashMap<AbstractCurve, Double>();
        
        if (recompSteps.size() == 0) {
            return ; // an empty diagram has no guide sizes
        }

        RecompositionStep last_step = recompSteps.get(recompSteps.size() - 1);
        AbstractDescription last_diag = last_step.to();

        // Each zone = AbstractBasicRegion will have a score.
        // Large score for zones that will need to be big.
        // Store them in a map.
        double totalZoneScore = 0.0;
        {
            Iterator<AbstractBasicRegion> zIt = last_diag.getZoneIterator();
            while (zIt.hasNext()) {
                AbstractBasicRegion abr = zIt.next();
                double score = scoreZone(abr, last_diag);
                totalZoneScore += score;
                zoneScores.put(abr, score);
            }
        }

        // Each contour will contain a set of zones so inherits
        // an accumulated score from that set of zone scores.
        Iterator<AbstractCurve> cIt = last_diag.getContourIterator();
        while (cIt.hasNext()) {
            AbstractCurve ac = cIt.next();
            double cScore = 0;
            Iterator<AbstractBasicRegion> zIt = last_diag.getZoneIterator();
            while (zIt.hasNext()) {
                AbstractBasicRegion abr = zIt.next();
                if (abr.is_in(ac)) {
                    cScore += zoneScores.get(abr);
                }
            }
            containedZoneScores.put(ac, cScore);
            
            // The guide size is a *magic* formula made up
            // of the contained zone scores and the total of all
            // zone scores.
            double guide_size = Math.exp(0.75 * Math.log(cScore / totalZoneScore)) * 200;
            guideSizes.put(ac, guide_size);
        }
    }
    public double getGuideSize(AbstractCurve ac){
        return guideSizes.get(ac);
    }
    public Set<AbstractBasicRegion> getScoredZones(){
        return zoneScores.keySet();
    }
    public double getGuideSize(AbstractBasicRegion abr) {
        return zoneScores.get(abr);
    }

    /**
     * Apply a heuristic to gauge how big a zone should be in a drawn 
     * diagram.  For the moment, a simple heuristic says that each zone 
     * contributes the same value to the heuristics.  In the future, we
     * might decide to weight some zones so that they provide larger
     * values to the heuristics.
     * @param abr
     * @return
     */
    private static double scoreZone(AbstractBasicRegion abr, AbstractDescription ad) {
        return 1.0;
    }
}

/**
 * Determine the order in which we build a diagram 
 * (i.e. choose in which order circles will be placed).
 * Optimisations include choosing a good order in which to add circles
 * or choosing to draw lots of circles at once.  
 * The BuildStep is the head of a linked list.
 */
interface BuildStepMaker{
    public BuildStep make();
}
/**
 * The simplest BuildStepMaker takes a sequence of recomposition steps
 * and re-expresses those as a sequence of BUildSteops which each adds
 * one contour.
 */
class SimpleBuildStepMaker implements BuildStepMaker{
    private ArrayList<RecompositionStep> recompSteps;
    
    public SimpleBuildStepMaker( ArrayList<RecompositionStep> recompSteps ){
        this.recompSteps = recompSteps;
    }
    public BuildStep make(){
        BuildStep buildStepsHead = null;
        BuildStep buildStepsTail = null;
        for (RecompositionStep rs : recompSteps) {
            // we need to add the new curves with regard to their placement
            // relative to the existing ones in the map
            Iterator<RecompData> it = rs.getRecompIterator();
            while (it.hasNext()) {
                RecompData rd = it.next();
                BuildStep newOne = new BuildStep(rd);
                if (buildStepsHead == null) {
                    buildStepsHead = newOne;
                    buildStepsTail = newOne;
                } else {
                    buildStepsTail.next = newOne;
                    buildStepsTail = newOne;
                }
            }
        }
        return buildStepsHead;
    }
    
}
/**
 * A better BuildStepMaker re-orders and combines the steps which add
 * contours so that we can add groups of similar contours together to
 * be more able to buidl diagrams with symmetry.
 */

class JeansBuildStepMaker implements BuildStepMaker{
    private ArrayList<RecompositionStep> recompSteps;
    private GuideSizeStrategy guideSizes;
    
    public JeansBuildStepMaker( ArrayList<RecompositionStep> recompSteps, 
                                GuideSizeStrategy guideSizes ){
        this.recompSteps = recompSteps;
        this.guideSizes = guideSizes;
    }
    public BuildStep make(){
        // Each RecompositionStep can yield a BuildStep.
        // The simplest BuildStepMaker just does a 1-1 conversion
        SimpleBuildStepMaker simpleConverter = new SimpleBuildStepMaker(recompSteps);
        BuildStep buildStepsHead = simpleConverter.make();

        // Now we apply some more intelligence to the BuildStep sequence.
        // Reorder where useful and combine multiple steps into one
        // to help us create diagrams with symmetry.

        BuildStep bs = buildStepsHead;
        while (bs != null) {
            // Keep this BuildStep in the list but consider
            // merging future BuildSteps into this one.
            // Expect that the BuildStep has one RecompositionStep
            // at the moment (i.e. one definition of how we want to add a 
            // circle).
            DEB.assertCondition(bs.recomp_data.size() == 1, "not ready for multistep");
            //
            if (bs.recomp_data.get(0).split_zones.size() == 1) {
                // This BuildStep is about adding a circle into a zone,
                // where the new circle is disjoint from all we have added
                // so far (i.e. it's "nested").  
                // Find out to which zone we will add this circle.
                RecompData rd = bs.recomp_data.get(0);
                AbstractBasicRegion abr = rd.split_zones.get(0);
                // look ahead - are there other similar nested additions?
                // Similar means that we will be adding a contour into the
                // same zone, and that the guide size of the 
                BuildStep beforefuturebs = bs;
                while (beforefuturebs != null && beforefuturebs.next != null) {
                    RecompData rd2 = beforefuturebs.next.recomp_data.get(0);
                    if (rd2.split_zones.size() == 1) {
                        AbstractBasicRegion abr2 = rd2.split_zones.get(0);
                        if (abr.isLabelEquivalent(abr2)) {
                            DEB.out(2, "found matching abrs " + abr.debug() + ", " + abr2.debug());
                            // check scores match
                            double abrScore = guideSizes.getGuideSize(rd.added_curve);
                            double abrScore2 = guideSizes.getGuideSize(rd2.added_curve);
                            DEB.assertCondition(abrScore > 0 && abrScore2 > 0, "zones must have score");
                            DEB.out(2, "matched nestings " + abr.debug() + " and " + abr2.debug()
                                    + "\n with scores " + abrScore + " and " + abrScore2);
                            if (abrScore == abrScore2) {
                                // unhook futurebs and insert into list after bs
                                BuildStep to_move = beforefuturebs.next;
                                beforefuturebs.next = to_move.next;

                                bs.recomp_data.add(to_move.recomp_data.get(0));
                            }
                        }
                    }
                    beforefuturebs = beforefuturebs.next;
                }// loop through futurebs's to see if we insert another
            }// check - are we adding a nested contour?
            else if (bs.recomp_data.get(0).split_zones.size() == 2) {// we are adding a 1-piercing
                RecompData rd = bs.recomp_data.get(0);
                AbstractBasicRegion abr1 = rd.split_zones.get(0);
                AbstractBasicRegion abr2 = rd.split_zones.get(1);
                // look ahead - are there other similar 1-piercings?
                BuildStep beforefuturebs = bs;
                while (beforefuturebs != null && beforefuturebs.next != null) {
                    RecompData rd2 = beforefuturebs.next.recomp_data.get(0);
                    if (rd2.split_zones.size() == 2) {
                        AbstractBasicRegion abr3 = rd2.split_zones.get(0);
                        AbstractBasicRegion abr4 = rd2.split_zones.get(1);
                        if ((abr1.isLabelEquivalent(abr3) && abr2.isLabelEquivalent(abr4))
                                || (abr1.isLabelEquivalent(abr4) && abr2.isLabelEquivalent(abr3))) {

                            DEB.out(2, "found matching abrs " + abr1.debug() + ", " + abr2.debug());
                            // check scores match
                            double abrScore = guideSizes.getGuideSize(rd.added_curve);
                            double abrScore2 = guideSizes.getGuideSize(rd2.added_curve);
                            DEB.assertCondition(abrScore > 0 && abrScore2 > 0, "zones must have score");
                            DEB.out(2, "matched piercings " + abr1.debug() + " and " + abr2.debug()
                                    + "\n with scores " + abrScore + " and " + abrScore2);
                            if (abrScore == abrScore2) {
                                // unhook futurebs and insert into list after bs
                                BuildStep to_move = beforefuturebs.next;
                                beforefuturebs.next = to_move.next;

                                bs.recomp_data.add(to_move.recomp_data.get(0));
                                continue;
                            }
                        }
                    }
                    beforefuturebs = beforefuturebs.next;
                }// loop through futurebs's to see if we insert another
            }

            bs = bs.next;
        }// bsloop
        return buildStepsHead;
    }
}

/**
 * The primary task of a DiagramCreator is to take an AbstractDescription
 * and produce a ConcreteDiagram.  The constructors take in the AbstractDescription
 * and potentially some more information to influence some of the choices
 * which are made (the heuristics) when we define the geometry of the ConcreteDiagram.
 * The main work takes place in DiagramCreator::createDiagram.
 * The diagrams are drawn iteratively - we successively add groups of contours.
 * We can this of the diagram as being built up in stages.  In the simplest case
 * we add only one new contour at each stage, but to assist with symmetry, we
 * sometimes add multiple contours in one stage.
 * Not all diagrams can be drawn, because we try to ensure that contours can
 * be visually distinguished and we might not find enough space to place them far 
 * enough apart.  Except in very extreme circumstances, this could be considered
 * a bug - a different choice at an earlier stage might have made it easier to
 * fit a later contour into the diagram.  
 * The task of taking an AbstractDescription and splitting the creation task into
 * stages is called Decomposition/Recomposition.  The heuristics used to make
 * choices there can be controlled using Strategy objects, passed to the 
 * DiagramCreator constructor.  Other heuristics are, for the moment, hidden
 * inside the createDiagram method (and the methods it calls).
 */
public class DiagramCreator {
	// TODO : does this class really need to be concerned with fonts?
    public static final Font font = new Font("Helvetica", Font.BOLD,  16);

    // Specification about the task - what are we trying to draw,
    // and how was it decomposed into a sequence of abstract diagrams?
    AbstractDescription abstractDiagram;
    final static int smallestRadius = 10;
    ArrayList<DecompositionStep> decompSteps;
    ArrayList<RecompositionStep> recompSteps;
    GuideSizeStrategy guideSizes;
    BuildStepMaker buildStepMaker;
    
    // Hold the results so far - for each AbstractCurve we will build
    // a CircleContour.
    HashMap<AbstractCurve, CircleContour> abstractToConcreteContourMap;
    ArrayList<CircleContour> drawnCircles;

    // Indices for debugging data collection
    int debugImageNumber = 0;
    int debugSize = 50;
    
    private void init(){
        Decomposer d = new Decomposer();
        decompSteps.addAll(d.decompose(abstractDiagram));
        Recomposer r = new Recomposer();
        recompSteps.addAll(r.recompose(decompSteps));
        abstractToConcreteContourMap = new HashMap<AbstractCurve, CircleContour>();
        drawnCircles = new ArrayList<CircleContour>();
        guideSizes = new JeansGuideSizeStrategy(abstractDiagram, recompSteps);
        //buildStepMaker = new SimpleBuildStepMaker(recompSteps);
        buildStepMaker = new JeansBuildStepMaker(recompSteps, guideSizes);
    }

    /** In this constructor, we take the abstract description,
    * and do some analysis of it.   We build a decomposition
    * and recomposition, using the default strategies.
    */
    public DiagramCreator(AbstractDescription ad) {
        abstractDiagram = ad;
        decompSteps = new ArrayList<DecompositionStep>();
        recompSteps = new ArrayList<RecompositionStep>();
        init();
    }

    /** In this constructor, we take the abstract description,
    * and do some analysis of it.   We build a decomposition
    * and recomposition using the specified strategies.
    */
    public DiagramCreator(AbstractDescription ad,
            DecompositionStrategy decomp_strategy,
            RecompositionStrategy recomp_strategy) {
        abstractDiagram = ad;
        decompSteps = new ArrayList<DecompositionStep>();
        recompSteps = new ArrayList<RecompositionStep>();
        init();
    }

    /** Do the bulk of the work to create a diagram. 
     * 
     * @param size
     * @return drawn diagram
     * @throws CannotDrawException
     */
    public ConcreteDiagram createDiagram(int size) throws CannotDrawException {
    	// Each diagram creation task triggers a fresh set
    	// of debugging information.
        debugSize = size;
        debugImageNumber = 0;        
    	
    	// Some heuristics determine roughly how big we expect contours
    	// to be (e.g. contours containing a lot of zones might be bigger).
    	// After this call, we can query the guideSizes map for each 
    	// abstractContour to get a number.
        makeGuideSizes();

        ConcreteDiagram result = null; // scoped outside the try-catch block
        try 
        {
        boolean ok = createCircles(); // draws at default size
        if (!ok) {
            drawnCircles = null;
            DEB.showFilmStrip();
            return null;
        }
        
        // createCircles returned OK
        CircleContour.fitCirclesToSize(drawnCircles, size); // scales to requested size

        // Now that we have the contours drawn, convert drawn circles 
        // into a ConcreteDiagram with shaded zones and spiders
        ArrayList<ConcreteZone> shadedZones = new ArrayList<ConcreteZone>();
        ArrayList<ConcreteZone> unshadedZones = new ArrayList<ConcreteZone>();
        createZones(shadedZones, unshadedZones);

        ArrayList<ConcreteSpider> spiders = createSpiders();

        // Put the contours, zones and spiders together to form a 
        // ConcreteDiagram.
        result = new ConcreteDiagram(new Rectangle2D.Double(0, 0, size, size),
                drawnCircles, shadedZones, unshadedZones, spiders);
        result.setFont(font);
        DEB.showFilmStrip();
        }
        catch(CannotDrawException x)
        {
        DEB.showFilmStrip();
        throw x;
        }
        return result;
    }
    
    private boolean foot_is_on_leg(ConcreteSpiderFoot foot, ConcreteSpiderLeg leg, double tol)
    {
        double sf_x = foot.getX() - leg.from.getX();
        double sf_y = foot.getY() - leg.from.getY();

        double se_x = leg.to.getX() - leg.from.getX();
        double se_y = leg.to.getY() - leg.from.getY();

        double se_length = Math.sqrt(se_x * se_x + se_y * se_y);

        double unit_leg_x = se_x / se_length;
        double unit_leg_y = se_y / se_length;

        double sf_dot_unit_leg = sf_x * unit_leg_x + sf_y * unit_leg_y;

        double sf_proj_leg_x = sf_dot_unit_leg * unit_leg_x;
        double sf_proj_leg_y = sf_dot_unit_leg * unit_leg_y;

        double sf_perp_leg_x = sf_x - sf_proj_leg_x;
        double sf_perp_leg_y = sf_y - sf_proj_leg_y;

        double sf_perp_leg_len = Math.sqrt(sf_perp_leg_x * sf_perp_leg_x + sf_perp_leg_y * sf_perp_leg_y);

        double sf_prop_leg = sf_proj_leg_x / se_x;
        
        if(Math.abs(se_x) < 0.001 && Math.abs(se_y) > 0.001)
        {
            sf_prop_leg = sf_proj_leg_y / se_y;
        }
        if (DEB.level >= 3) {
            System.out.println("sf_perp_leg_len = "+sf_perp_leg_len +", sf_prop_leg = "+sf_prop_leg);
        }
        
        boolean foot_on_leg = sf_perp_leg_len < tol
                && sf_prop_leg > 0
                && sf_prop_leg < 1;
        return foot_on_leg;
    }

    /**
     * Given a set of drawnCircles and a list of AbstractSpiders, choose 
     * spider feet and legs to make up ConcreteSpiders in the ConcreteDiagram.
     * @return
     * @throws CannotDrawException
     */
    private ArrayList<ConcreteSpider> createSpiders() throws CannotDrawException {
    	
    	// For each zone = AbstractBasicRegion, count how many spider feet
    	// fall in that zone.
        HashMap<AbstractBasicRegion, Integer> footCount = 
        		new HashMap<AbstractBasicRegion, Integer>();
        Iterator<AbstractSpider> it = abstractDiagram.getSpiderIterator();
        while (it.hasNext()) {
            AbstractSpider as = it.next();
            for (AbstractBasicRegion abr : as.get_feet()) {
                Integer oldCount = footCount.get(abr);
                Integer newCount = null;
                if (oldCount != null) {
                    newCount = new Integer(oldCount.intValue() + 1);
                } else {
                    newCount = new Integer(1);
                }
                footCount.put(abr, newCount);
            }
        }
        // Build some feet.  This is achieved by treating each foot as 
        // a very small contour - we reuse the code that puts nested
        // contours inside zones.
        Rectangle2D.Double box = CircleContour.makeBigOuterBox(drawnCircles);
        RecompositionStep last_step = null;
        if (recompSteps != null && recompSteps.size() > 0) {
            last_step = recompSteps.get(recompSteps.size() - 1);
        }
        AbstractDescription last_diag = null;
        if (last_step != null) {
            last_diag = last_step.to();
        }
        // Map from each AbstractBasicRegion to the set of ConcreteSpiderFoot
        // objects that we build inside that zone.
        HashMap<AbstractBasicRegion, ArrayList<ConcreteSpiderFoot>> drawnFeet =
                new HashMap<AbstractBasicRegion, ArrayList<ConcreteSpiderFoot>>();
        for (AbstractBasicRegion abr : footCount.keySet()) {
        	// The list of feet.
            ArrayList<ConcreteSpiderFoot> footList = new ArrayList<ConcreteSpiderFoot>();
            drawnFeet.put(abr, footList);
            Integer num_required = footCount.get(abr);
            
            // Build tiny contours.
            ArrayList<AbstractCurve> acs = new ArrayList<AbstractCurve>();
            for (int i = 0; i < num_required.intValue(); i++) {
                acs.add(new AbstractCurve(null));
            }
            // Work out which zone they need to go into.
            AbstractBasicRegion zone_in_last_diag = last_diag.getLabelEquivalentZone(abr);
            if (zone_in_last_diag == null) {
                throw new CannotDrawException("problem with spider habitat");
            }
            // Use the function placeContour as if we were placing
            // nested contours inside this zone.  That will give us
            // circle centers to use for spider feet positions.
            ArrayList<CircleContour> cs = placeContours(box, smallestRadius, 3,
                    zone_in_last_diag, last_diag, acs, 3);
            for (CircleContour cc : cs) {
                footList.add(new ConcreteSpiderFoot(cc.cx, cc.cy));
            }
        }

        // Now we have chosen positions for all spider feet.  We haven't
        // grouped together feet to allocate them to individual spiders
        // nor decided which feet should be joined by spider legs.
        
        // TODO collect good choices of feet into spiders
        // for now, we just pick feet which are in the right zones.
        ArrayList<ConcreteSpider> result = new ArrayList<ConcreteSpider>();
        it = abstractDiagram.getSpiderIterator();
        ArrayList<ConcreteSpider> spiders = new ArrayList<ConcreteSpider>();
        HashMap<ConcreteSpiderFoot, AbstractBasicRegion> feet_and_zones =
                new HashMap<ConcreteSpiderFoot, AbstractBasicRegion>();
        while (it.hasNext()) {
            AbstractSpider as = it.next();
            ConcreteSpider cs = new ConcreteSpider(as);
            // For this abstract spider, choose some feet.
            for (AbstractBasicRegion abr : as.get_feet()) {
                ArrayList<ConcreteSpiderFoot> footList = drawnFeet.get(abr);
                // footList is the list of available feet in that zone.
                // It should have some feet in it, otherwise there is
                // some coding error.
                if (footList == null || footList.size() == 0) {
                    throw new CannotDrawException("spider foot problem");
                }

                // Simplistic - just use the first available 
                // foot in the list.
                ConcreteSpiderFoot foot = footList.get(0);
                footList.remove(0);
                foot.setSpider(cs);
                cs.feet.add(foot);

                // get the corresponding abr from the last_diag
                feet_and_zones.put(foot, last_diag.getLabelEquivalentZone(abr));
            }
            // Now we have the set of feet for this spider.  
            // Simplistic : Arrange the legs by choosing a foot as 
            // "most central" - meaning shortest leg length sum -
            // and draw legs from the central body to the outer feet
            //
            // TODO - not all spiders require a central body 
            // - e.g. four feet in a row
            // so we should construct a minimum-leg-span tree rather than 
            // choosing a central foot as the spider "body"
            ConcreteSpiderFoot centralFoot = null;
            double best_dist_sum = Double.MAX_VALUE;
            for (ConcreteSpiderFoot centreCandidate : cs.feet) {
                double distSum = 0;
                for (ConcreteSpiderFoot other : cs.feet) {
                    if (other == centreCandidate) {
                        continue;
                    }
                    distSum += Math.sqrt((centreCandidate.getX() - other.getX()) * (centreCandidate.getX() - other.getX())
                            + (centreCandidate.getY() - other.getY()) * (centreCandidate.getY() - other.getY()));
                }
                if (distSum < best_dist_sum) {
                    best_dist_sum = distSum;
                    centralFoot = centreCandidate;
                }
            }
            for (ConcreteSpiderFoot other : cs.feet) {
                if (other == centralFoot) {
                    continue;
                }
                ConcreteSpiderLeg leg = new ConcreteSpiderLeg();
                leg.from = centralFoot;
                leg.to = other;
                cs.legs.add(leg);
            }

            spiders.add(cs);
            result.add(cs);
        }

        // We want to avoid spiders that overlap - especially 
        // those with a leg passing through the foot of another spider.
        // For now, just nudge the spider foot off the offending leg.
        // Check that the new foot is still in its relevant abstract 
        // basic region.

        // First - detect where feet meet a non-adjacent leg
        double tol = 6;
        
        boolean check_feet_placements = true;
        while(check_feet_placements){
        check_feet_placements = false;
        for (ConcreteSpider cs : spiders) {
            if(check_feet_placements)//(start again)
                break;
            for (ConcreteSpiderFoot foot : cs.feet) {
                // check whether foot is too close to another leg
                if(check_feet_placements)//(start again)
                    break;
                for (ConcreteSpider cs2 : spiders) {
                    if(check_feet_placements)//(start again)
                        break;
                    for (ConcreteSpiderLeg leg : cs2.legs) {
                        if (leg.from == foot || leg.to == foot) {
                            // this leg is bound to be close to foot -it's attached!
                            continue;
                        }
                        // is foot on leg?
                        if (DEB.level >= 3) {
                            System.out.println("check spider "+cs+" foot ("+foot.getX()+","+foot.getY()+") against "+
                                                    " spider "+cs2+
                             " leg ("+leg.from.getX()+","+leg.from.getY()+")->("+leg.to.getX()+","+leg.to.getY()+")");
                        }
                        boolean foot_on_leg = foot_is_on_leg(foot, leg, tol);
                        if (foot_on_leg) {
                            // nudge the foot, but check it's still in its zone afterwards
                            double old_x = foot.getX();
                            double old_y = foot.getY();
                            AbstractBasicRegion abr = feet_and_zones.get(foot);

                            ConcreteZone cz = makeConcreteZone(abr);
                            Area a = new Area(cz.getShape(box));
                            
                            double new_y = old_y + 5 * tol; // how far to nudge it?
//                          double new_x = old_x + 2 * tol;
                            double new_x = old_x;
                            CircleContour test = new CircleContour(new_x, new_y, tol, null);
                            if (circleInArea(test, a)) {
                                foot.setX(new_x);
                                foot.setY(new_y);
                                if(foot_is_on_leg(foot, leg, tol)){
                                    foot.setX(old_x);
                                    foot.setY(old_y);
                                }else{
                                    check_feet_placements = true; // if we moved one, start all over again!
                                    break;                                  
                                }
                            }
                            new_x = old_x - 5 * tol;
                            new_y = old_y - 5 * tol;
                            test = new CircleContour(new_x, new_y, tol, null);
                            if (circleInArea(test, a)) {
                                foot.setX(new_x);
                                foot.setY(new_y);
                                if(foot_is_on_leg(foot, leg, tol)){
                                    foot.setX(old_x);
                                    foot.setY(old_y);
                                }else{
                                    check_feet_placements = true; // if we moved one, start all over again!
                                    break;                                  
                                }
                            }
                            new_x = old_x + 5 * tol;
                            new_y = old_y - 5 * tol;
                            test = new CircleContour(new_x, new_y, tol, null);
                            if (circleInArea(test, a)) {
                                foot.setX(new_x);
                                foot.setY(new_y);
                                if(foot_is_on_leg(foot, leg, tol)){
                                    foot.setX(old_x);
                                    foot.setY(old_y);
                                }else{
                                    check_feet_placements = true; // if we moved one, start all over again!
                                    break;                                  
                                }
                            }
                            new_x = old_x - 5 * tol;
                            new_y = old_y + 5 * tol;
                            test = new CircleContour(new_x, new_y, tol, null);
                            if (circleInArea(test, a)) {
                                foot.setX(new_x);
                                foot.setY(new_y);
                                if(foot_is_on_leg(foot, leg, tol)){
                                    foot.setX(old_x);
                                    foot.setY(old_y);
                                }else{
                                    check_feet_placements = true; // if we moved one, start all over again!
                                    break;                                  
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        return result;
    }
    /**
     * Apply heuristics to abstract diagram contours to decide
     * how large they might be (a "guide size" for the contour).
     * Populates the map guideSizes.
     */
    private void makeGuideSizes() {
    	
    }

    /**
     * Given drawnCircles create the ConcreteZones we will need
     * in the drawn ConcreteDiagram. Call this only after createCircles
     * has successfully done its job.
     * @param shadedZones
     * @param unshadedZones
     */
    private void createZones(ArrayList<ConcreteZone> shadedZones,
            ArrayList<ConcreteZone> unshadedZones) {
        AbstractDescription final_diagram = null;
        if (decompSteps.size() == 0) {
            final_diagram = abstractDiagram;
        } else {
            final_diagram = recompSteps.get(recompSteps.size() - 1).to();
        }
        // which zones in final_diagram were shaded in initial_diagram?
        // which zones in final_diagram were not in initial_diagram, or specified shaded in initial_diagram?

        if (DEB.level > 2) {
            Iterator<AbstractBasicRegion> it = abstractDiagram.getZoneIterator();
            while (it.hasNext()) {
                System.out.println("initial zone " + it.next().debug());
            }
            it = final_diagram.getZoneIterator();
            while (it.hasNext()) {
                System.out.println("final zone " + it.next().debug());
            }
        }

        Iterator<AbstractBasicRegion> it = final_diagram.getZoneIterator();
        while (it.hasNext()) {
            AbstractBasicRegion z = it.next();
            AbstractBasicRegion matched_z = abstractDiagram.getLabelEquivalentZone(z);
            if (matched_z == null || abstractDiagram.hasShadedZone(matched_z)) {
                if (DEB.level > 2) {
                    System.out.println("extra zone " + z.debug());
                }
                ConcreteZone cz = makeConcreteZone(z);
                shadedZones.add(cz);
            } else {
                ConcreteZone cz = makeConcreteZone(z);
                unshadedZones.add(cz);
            }
        }
    }

    /**
     * Given drawnCircles and an AbstractBasicRegion (zone), create a
     * ConcreteZone (which has a shape as well as knowing which contours
     * it belongs to / doesn't belong to).  Call this only after createCircles
     * has successfully done its job.
     * @param z
     * @return
     */
    private ConcreteZone makeConcreteZone(AbstractBasicRegion z) {
        ArrayList<CircleContour> includingCircles = new ArrayList<CircleContour>();
        ArrayList<CircleContour> excludingCircles = new ArrayList<CircleContour>(drawnCircles);
        Iterator<AbstractCurve> acIt = z.getContourIterator();
        while (acIt.hasNext()) {
            AbstractCurve ac = acIt.next();
            CircleContour containingCC = abstractToConcreteContourMap.get(ac);
            excludingCircles.remove(containingCC);
            includingCircles.add(containingCC);
        }
        ConcreteZone cz = new ConcreteZone(z, includingCircles, excludingCircles);
        return cz;
    }

    /**
	 * Given the abstract diagram description, the decomposition
	 * / recomposition and some guide sizes for contours
	 * based on heuristics, make choices of drawn circles for the 
	 * contours in the abstract diagram.  
	 * @return whether circle creation completed OK
	 * @throws CannotDrawException
	 */
    private boolean createCircles() throws CannotDrawException {
    	// Make a linked list of BuildSteps from the recomposition
    	// sequence.  Each BuildStep can corresponds to some RecompositionSteps.
        BuildStep buildStepsHead = buildStepMaker.make();
 
        // Iterate through the sequence of BuildSteps, incrementally
        // building up the drawn diagram by choosing the circle placement
        // for the circles in each BuildStep.
        BuildStep thisBuildStep = buildStepsHead;
        stepLoop:
        while (thisBuildStep != null) {
            DEB.out(2, "new build step");
            
            // We have built some circles so far.  Make an outerBox 
            // from those we have already drawn.  This helps if we have
            // to add circles outside existing ones.
            Rectangle2D.Double outerBox = CircleContour.makeBigOuterBox(drawnCircles);

            // A BuildStep corresponds to one or more RecompositionSteps.
            if (thisBuildStep.recomp_data.size() > 1) {
            	// We have chosen to draw more than one circle at once.
                if (thisBuildStep.recomp_data.get(0).split_zones.size() == 1) {
                    // We have chosen to draw more than one nested 
                	// circle in a zone all at once.
                	
                	// Which zone are we adding to?
                    RecompData rd = thisBuildStep.recomp_data.get(0);
                    AbstractBasicRegion zone = rd.split_zones.get(0);

                    RecompositionStep last_step = recompSteps.get(recompSteps.size() - 1);
                    AbstractDescription last_diag = last_step.to();

                    // Find out our guide size.  Assume the guide size for all
                    // our RecompDatas are equal - so just find out 1st.
                    AbstractCurve ac = rd.added_curve;
                    double suggested_rad = guideSizes.getGuideSize(ac);

                    // Build a set of AbstractCurves for all the circles we
                    // seek to insert.
                    ArrayList<AbstractCurve> acs = new ArrayList<AbstractCurve>();
                    for (RecompData rd2 : thisBuildStep.recomp_data) {
                        ac = rd2.added_curve;
                        acs.add(ac);
                    }

                    // Call placeContours to decide how to arrange the circles 
                    // in our zone.
                    ArrayList<CircleContour> cs = placeContours(outerBox, 
                    		smallestRadius, suggested_rad,
                            zone, last_diag, acs, debugImageNumber);

                    if (cs != null && cs.size() > 0) {
                        DEB.assertCondition(cs.size() == thisBuildStep.recomp_data.size(), "not enough circles for rds");
                        for (int i = 0; i < cs.size(); i++) {
                            CircleContour c = cs.get(i);
                            ac = thisBuildStep.recomp_data.get(i).added_curve;
                            DEB.assertCondition(
                                    c.ac.getLabel() == ac.getLabel(), "mismatched labels");
                            abstractToConcreteContourMap.put(ac, c);
                            addCircle(c);
                        }
                        thisBuildStep = thisBuildStep.next;
                        continue stepLoop;
                    }
                } else if (thisBuildStep.recomp_data.get(0).split_zones.size() == 2) {
                	// We are seeking to place multiple 1-piercings around
                	// between a pair of adjacent "split zones".

                    // Look at the 1st 1-piercing (assume others similar)
                    RecompData rd0 = thisBuildStep.recomp_data.get(0);
                    AbstractBasicRegion abr0 = rd0.split_zones.get(0);
                    AbstractBasicRegion abr1 = rd0.split_zones.get(1);
                    AbstractCurve piercingCurve = rd0.added_curve;

                    AbstractCurve pierced_ac = abr0.getStraddledContour(abr1);
                    CircleContour pierced_cc = abstractToConcreteContourMap.get(pierced_ac);
                    ConcreteZone cz0 = makeConcreteZone(abr0);
                    ConcreteZone cz1 = makeConcreteZone(abr1);

                    // Build a combined area for the pair of split zones 
                    Area a = new Area(cz0.getShape(outerBox));
                    a.add(cz1.getShape(outerBox));

                    double suggested_rad = guideSizes.getGuideSize(piercingCurve);

                    DEB.show(4, a, "a for 1-piercings " + debugImageNumber);

                    // We have made a piercing which is centred on the circumference of circle c.
                    // but if the contents of rd.addedCurve are not equally balanced between
                    // things inside c and things outside, we may end up squashing lots
                    // into half of rd.addedCurve, leaving the other half looking empty.
                    // See if we can nudge c outwards or inwards to accommodate
                    // its contents.

                    // iterate through zoneScores, looking for zones inside c,
                    // then ask whether they are inside or outside cc.  If we
                    // get a big score outside, then try to move c outwards.

                    //  HashMap<AbstractBasicRegion, Double> zoneScores;
                    double score_in_c = 0.0;
                    double score_out_of_c = 0.0;

                    double center_of_circle_lies_on_rad = pierced_cc.radius;

                    Set<AbstractBasicRegion> allZones = guideSizes.getScoredZones();
                    for (AbstractBasicRegion abr : allZones) {
                        DEB.out(1, "compare " + abr.debug() + " against " + piercingCurve.debug());
                        if (!abr.is_in(piercingCurve)) {
                            continue;
                        }
                        DEB.out(1, "OK " + abr.debug() + " is in " + piercingCurve.debug() + ", so compare against " + pierced_ac.debug());
                        if (abr.is_in(pierced_ac)) {
                            score_in_c += guideSizes.getGuideSize(abr);
                        } else {
                            score_out_of_c += guideSizes.getGuideSize(abr);
                        }
                    }
                    DEB.out(3, "scores for " + piercingCurve + " are inside=" + score_in_c + " and outside=" + score_out_of_c);

                    if (score_out_of_c > score_in_c) {
                        double nudge = suggested_rad * 0.3;
                        center_of_circle_lies_on_rad += nudge;
                    } else if (score_out_of_c < score_in_c) {
                        double nudge = Math.min(suggested_rad * 0.3, (pierced_cc.radius * 2 - suggested_rad) * 0.5);
                        center_of_circle_lies_on_rad -= nudge;
                    }

                    double guide_rad = guideSizes.getGuideSize(thisBuildStep.recomp_data.get(0).added_curve);
                    int sampleSize = (int) (Math.PI / Math.asin(guide_rad / pierced_cc.radius));
                    if (sampleSize >= thisBuildStep.recomp_data.size()) {
                        int num_ok = 0;
                        for (int i = 0; i < sampleSize; i++) {
                            double angle = i * Math.PI * 2.0 / sampleSize;
                            double x = pierced_cc.cx + Math.cos(angle) * center_of_circle_lies_on_rad;
                            double y = pierced_cc.cy + Math.sin(angle) * center_of_circle_lies_on_rad;
                            if (a.contains(x, y)) {
                                CircleContour sample = new CircleContour(x, y, guide_rad,
                                        thisBuildStep.recomp_data.get(0).added_curve);
                                if (circleInArea(sample, a)) {
                                    num_ok++;
                                }
                            }
                        }
                        if (num_ok >= thisBuildStep.recomp_data.size()) {
                            if (num_ok == sampleSize) {
                                // all OK.
                                for (int i = 0; i < thisBuildStep.recomp_data.size(); i++) {
                                    double angle = 0.0 + i * Math.PI * 2.0 / thisBuildStep.recomp_data.size();
                                    double x = pierced_cc.cx + Math.cos(angle) * center_of_circle_lies_on_rad;
                                    double y = pierced_cc.cy + Math.sin(angle) * center_of_circle_lies_on_rad;
                                    if (a.contains(x, y)) {
                                        AbstractCurve added_curve = thisBuildStep.recomp_data.get(i).added_curve;
                                        CircleContour c = new CircleContour(x, y, guide_rad, added_curve);
                                        abr0 = thisBuildStep.recomp_data.get(i).split_zones.get(0);
                                        abr1 = thisBuildStep.recomp_data.get(i).split_zones.get(1);

                                        abstractToConcreteContourMap.put(added_curve, c);
                                        addCircle(c);
                                    }
                                }
                                thisBuildStep = thisBuildStep.next;
                                continue stepLoop;
                            } else if (num_ok > sampleSize) {  // BUG?  Doesn't make sense
                                num_ok = 0;
                                for (int i = 0; i < sampleSize; i++) {
                                    double angle = 0.0 + i * Math.PI * 2.0 / sampleSize;
                                    double x = pierced_cc.cx + Math.cos(angle) * center_of_circle_lies_on_rad;
                                    double y = pierced_cc.cy + Math.sin(angle) * center_of_circle_lies_on_rad;
                                    if (a.contains(x, y)) {
                                        AbstractCurve added_curve = thisBuildStep.recomp_data.get(i).added_curve;
                                        CircleContour c = new CircleContour(x, y, guide_rad, added_curve);
                                        if (circleInArea(c, a)) {
                                            abr0 = thisBuildStep.recomp_data.get(num_ok).split_zones.get(0);
                                            abr1 = thisBuildStep.recomp_data.get(num_ok).split_zones.get(1);
                                            abstractToConcreteContourMap.put(added_curve, c);
                                            addCircle(c);
                                            num_ok++;
                                            if (num_ok == thisBuildStep.recomp_data.size()) {
                                                break;
                                            }
                                        }
                                    }
                                }
                                thisBuildStep = thisBuildStep.next;
                                continue stepLoop;
                            }
                        }
                    }
                }
            }

            for (RecompData rd : thisBuildStep.recomp_data) {
                AbstractCurve ac = rd.added_curve;
                double suggested_rad = guideSizes.getGuideSize(ac);
                if (rd.split_zones.size() == 1) {
                    // add a nested contour---------------------------------------------------
                    // add a nested contour---------------------------------------------------
                    // add a nested contour---------------------------------------------------

                    // look ahead - are we going to add a piercing to this?
                    // if so, push it to one side to make space
                    boolean will_pierce = false;
                    BuildStep future_bs = buildStepsHead.next;
                    while (future_bs != null) {
                        if (future_bs.recomp_data.get(0).split_zones.size() == 2) {
                            AbstractBasicRegion abr0 = future_bs.recomp_data.get(0).split_zones.get(0);
                            AbstractBasicRegion abr1 = future_bs.recomp_data.get(0).split_zones.get(1);
                            AbstractCurve ac_future = abr0.getStraddledContour(abr1);
                            if (ac_future == ac) {
                                will_pierce = true;
                                break;
                            }
                        }
                        future_bs = future_bs.next;
                    }

                    if (DEB.level > 3) {
                        System.out.println("make a nested contour");
                    }
                    // make a circle inside containingCircles, outside excludingCirles.

                    AbstractBasicRegion zone = rd.split_zones.get(0);

                    RecompositionStep last_step = recompSteps.get(recompSteps.size() - 1);
                    AbstractDescription last_diag = last_step.to();

                    // put contour into a zone
                    CircleContour c = findCircleContour(outerBox, smallestRadius, suggested_rad,
                            zone, last_diag, ac, debugImageNumber);

                    if (c == null) {
                        throw new CannotDrawException("cannot place nested contour");
                    }

                    if (will_pierce && rd.split_zones.get(0).getNumContours() > 0) {
                        // nudge to the left
                        c.cx -= c.radius * 0.5;

                        ConcreteZone cz = makeConcreteZone(rd.split_zones.get(0));
                        Area a = new Area(cz.getShape(outerBox));
                        if (!circleInArea(c, a)) {
                            c.cx += c.radius * 0.25;
                            c.radius *= 0.75;
                        }
                    }
                    abstractToConcreteContourMap.put(ac, c);
                    addCircle(c);
                } else if (rd.split_zones.size() == 2) {
                    // add a single piercing---------------------------------------------------
                    // add a single piercing---------------------------------------------------
                    // add a single piercing---------------------------------------------------

                    if (DEB.level > 3) {
                        System.out.println("make a single-piercing contour");
                    }
                    AbstractBasicRegion abr0 = rd.split_zones.get(0);
                    AbstractBasicRegion abr1 = rd.split_zones.get(1);
                    AbstractCurve c = abr0.getStraddledContour(abr1);
                    CircleContour cc = abstractToConcreteContourMap.get(c);
                    ConcreteZone cz0 = makeConcreteZone(abr0);
                    ConcreteZone cz1 = makeConcreteZone(abr1);
                    Area a = new Area(cz0.getShape(outerBox));

                    DEB.show(4, a, "for single piercing first half " + debugImageNumber);
                    DEB.show(4, new Area(cz1.getShape(outerBox)), "for single piercing second half " + debugImageNumber);
                    a.add(cz1.getShape(outerBox));

                    DEB.show(4, a, "for single piercing " + debugImageNumber);

                    // We have made a piercing which is centred on the circumference of circle c.
                    // but if the contents of rd.addedCurve are not equally balanced between
                    // things inside c and things outside, we may end up squashing lots
                    // into half of rd.addedCurve, leaving the other half looking empty.
                    // See if we can nudge c outwards or inwards to accommodate
                    // its contents.

                    // iterate through zoneScores, looking for zones inside c,
                    // then ask whether they are inside or outside cc.  If we
                    // get a big score outside, then try to move c outwards.

                    //  HashMap<AbstractBasicRegion, Double> zoneScores;
                    double score_in_c = 0.0;
                    double score_out_of_c = 0.0;

                    double center_of_circle_lies_on_rad = cc.radius;
                    double smallest_allowed_rad = smallestRadius;

                    Set<AbstractBasicRegion> allZones = guideSizes.getScoredZones();
                    for (AbstractBasicRegion abr : allZones) {
                        DEB.out(1, "compare " + abr.debug() + " against " + c.debug());
                        if (!abr.is_in(rd.added_curve)) {
                            continue;
                        }
                        DEB.out(1, "OK " + abr.debug() + " is in " + c.debug() + ", so compare against " + cc.debug());
                        if (abr.is_in(c)) {
                            score_in_c += guideSizes.getGuideSize(abr);
                        } else {
                            score_out_of_c += guideSizes.getGuideSize(abr);
                        }
                    }
                    DEB.out(3, "scores for " + c + " are inside=" + score_in_c + " and outside=" + score_out_of_c);

                    if (score_out_of_c > score_in_c) {
                        double nudge = suggested_rad * 0.3;
                        smallest_allowed_rad += nudge;
                        center_of_circle_lies_on_rad += nudge;
                    } else if (score_out_of_c < score_in_c) {
                        double nudge = Math.min(suggested_rad * 0.3, (cc.radius * 2 - suggested_rad) * 0.5);
                        smallest_allowed_rad += nudge;
                        center_of_circle_lies_on_rad -= nudge;
                    }

                    // now place circles around cc, checking whether they fit into a
                    CircleContour solution = null;
                    for (AngleIterator ai = new AngleIterator(); ai.hasNext();) {
                        double angle = ai.nextAngle();
                        double x = cc.cx + Math.cos(angle) * center_of_circle_lies_on_rad;
                        double y = cc.cy + Math.sin(angle) * center_of_circle_lies_on_rad;
                        if (a.contains(x, y)) {
                            // how big a circle can we make?
                            double start_rad;
                            if (solution != null) {
                                start_rad = solution.radius + smallestRadius;
                            } else {
                                start_rad = smallestRadius;
                            }
                            CircleContour attempt = growCircleContour(a, rd.added_curve,
                                    x, y, suggested_rad,
                                    start_rad,
                                    smallest_allowed_rad);
                            if (attempt != null) {
                                solution = attempt;
                                if (solution.radius == guideSizes.getGuideSize(ac)) {
                                    break; // no need to try any more
                                }
                            }

                        }//check that the centre is ok
                    }// loop for different centre placement
                    if (solution == null) // no single piercing found which was OK
                    {
                        throw new CannotDrawException("1-peircing no fit");
                    } else {
                        DEB.out(2, "added a single piercing labelled " + solution.ac.getLabel());
                        abstractToConcreteContourMap.put(rd.added_curve, solution);
                        addCircle(solution);
                    }
                } else {
                    //double piercing
                    AbstractBasicRegion abr0 = rd.split_zones.get(0);
                    AbstractBasicRegion abr1 = rd.split_zones.get(1);
                    AbstractBasicRegion abr2 = rd.split_zones.get(2);
                    AbstractBasicRegion abr3 = rd.split_zones.get(3);
                    AbstractCurve c1 = abr0.getStraddledContour(abr1);
                    AbstractCurve c2 = abr0.getStraddledContour(abr2);
                    CircleContour cc1 = abstractToConcreteContourMap.get(c1);
                    CircleContour cc2 = abstractToConcreteContourMap.get(c2);

                    double[][] intn_coords = intersctCircles(cc1.cx, cc1.cy, cc1.radius,
                            cc2.cx, cc2.cy, cc2.radius);
                    if (intn_coords == null) {
                        System.out.println("double piercing on non-intersecting circles");
                        return false;
                    }

                    ConcreteZone cz0 = makeConcreteZone(abr0);
                    ConcreteZone cz1 = makeConcreteZone(abr1);
                    ConcreteZone cz2 = makeConcreteZone(abr2);
                    ConcreteZone cz3 = makeConcreteZone(abr3);
                    Area a = new Area(cz0.getShape(outerBox));
                    a.add(cz1.getShape(outerBox));
                    a.add(cz2.getShape(outerBox));
                    a.add(cz3.getShape(outerBox));

                    DEB.show(4, a, "for double piercing " + debugImageNumber);

                    double cx, cy;
                    if (a.contains(intn_coords[0][0], intn_coords[0][1])) {
                        if (DEB.level > 2) {
                            System.out.println("intn at (" + intn_coords[0][0] + "," + intn_coords[0][1] + ")");
                        }
                        cx = intn_coords[0][0];
                        cy = intn_coords[0][1];
                    } else if (a.contains(intn_coords[1][0], intn_coords[1][1])) {
                        if (DEB.level > 2) {
                            System.out.println("intn at (" + intn_coords[1][0] + "," + intn_coords[1][1] + ")");
                        }
                        cx = intn_coords[1][0];
                        cy = intn_coords[1][1];
                    } else {
                        if (DEB.level > 2) {
                            System.out.println("no suitable intn for double piercing");
                        }
                        throw new CannotDrawException("2peircing + disjoint");
                    }

                    CircleContour solution = growCircleContour(a, rd.added_curve, cx, cy,
                            suggested_rad, smallestRadius, smallestRadius);
                    if (solution == null) // no double piercing found which was OK
                    {
                        throw new CannotDrawException("2peircing no fit");
                    } else {
                        DEB.out(2, "added a double piercing labelled " + solution.ac.getLabel());
                        abstractToConcreteContourMap.put(rd.added_curve, solution);
                        addCircle(solution);
                    }
                }// if/else/else about piercing type
            }// next RecompData in the BuildStep
            thisBuildStep = thisBuildStep.next;
        }// go to next BuildStep

        return true;
    }

    /**
     * Once we have chosen a CircleContour to put in the diagram,
     * call this function to perform the necessary steps.
     * (generate debug, give it a colour, store it as a drawnCircle,...)
     * @param c
     */
    void addCircle(CircleContour c) {
        if (DEB.level > 2) {
            System.out.println("adding " + c.debug());
        }
        assignCircleColour(c);
        drawnCircles.add(c);

        addDebugView(3, debugImageNumber, debugSize);
        debugImageNumber++;
    }

    /**
     * Assign a colour to a CircleContour (determined by its label).
     * @param cc
     */
    private void assignCircleColour(CircleContour cc) {
        String s = cc.ac.getLabel().getLabel();
        if (s == null || s.length() < 1) {
            return;
        }
        char c = s.charAt(0);
        int n = Character.getNumericValue(c) - Character.getNumericValue('a');
        while (n < Colors.COLORS.length) {
            n += Colors.COLORS.length;
        }
        int col_index = n % Colors.COLORS.length;
        cc.setColor(Colors.COLORS[col_index]);
    }

    /**
     * Determine a largish radius for a circle centered at given cx, cy
     * which fits inside area a.  Return a CircleContour with this
     * centre, radius and labelled according to the AbstractCurve.
     * @param a
     * @param ac
     * @param centreX
     * @param centreY
     * @param suggestedRadius
     * @param startRadius
     * @param smallestRadius
     * @return
     */
    private CircleContour growCircleContour(Area a, AbstractCurve ac,
            double centreX, 
            double centreY,
            double suggestedRadius, 
            double startRadius,
            double smallestRadius) {
        CircleContour attempt = new CircleContour(centreX, centreY, suggestedRadius, ac);
        if (circleInArea(attempt, a)) {
            return new CircleContour(centreX, centreY, suggestedRadius, ac);
        }

        boolean ok = true;
        double good_rad = -1.0;
        double rad = startRadius;
        while (ok) {
            attempt = new CircleContour(centreX, centreY, rad, ac);
            if (circleInArea(attempt, a)) {
                good_rad = rad;
                rad *= 1.5;
            } else {
                break;
            }
        }// loop for increasing radii
        if (good_rad < 0.0) {
            return null;
        }
        CircleContour sol = new CircleContour(centreX, centreY, good_rad, ac);
        return sol;
    }

    /**
     * A wrapper function around placeContours which has an interface
     * for placing just one contour.
     * @param outerBox
     * @param smallest_rad
     * @param guide_rad
     * @param zone
     * @param last_diag
     * @param ac
     * @param debug_index
     * @return
     * @throws CannotDrawException
     */
    private CircleContour findCircleContour(Rectangle2D.Double outerBox,
            int smallest_rad,
            double guide_rad,
            AbstractBasicRegion zone,
            AbstractDescription last_diag,
            AbstractCurve ac,
            int debug_index) throws CannotDrawException {
        ArrayList<AbstractCurve> acs = new ArrayList<AbstractCurve>();
        acs.add(ac);
        ArrayList<CircleContour> result = placeContours(outerBox,
                smallest_rad, guide_rad, zone, last_diag, acs,
                debug_index);
        if (result == null || result.size() == 0) {
            return null;
        } else {
            return result.get(0);
        }
    }

    /**
     * Scan an array of PotentialCentres and find out whether
     * all in a given sub-array are flagged as "ok".
     * @param lowi
     * @param highi
     * @param lowj
     * @param highj
     * @param ok_array
     * @param Ni
     * @param Nj
     * @return
     */
    private boolean all_ok_in(int lowi, int highi, int lowj, int highj,
            PotentialCentre[][] ok_array, int Ni, int Nj) {
        boolean all_ok = true;
        for (int i = lowi; all_ok && i < highi + 1; i++) {
            for (int j = lowj; all_ok && j < highj + 1; j++) {
                if (i >= Ni || j >= Nj || !ok_array[i][j].ok) {
                    all_ok = false;
                }
            }
        }
        return all_ok;
    }

    private ArrayList<CircleContour> placeContours(
    		Rectangle2D.Double outerBox,
            int smallestRadius,
            double guideRadius,
            AbstractBasicRegion zone,
            AbstractDescription lastDiagram,
            ArrayList<AbstractCurve> abstractCurves,
            int debugIndex) throws CannotDrawException {
        ArrayList<CircleContour> result = new ArrayList<CircleContour>();

        // special case : handle the drawing if it's the first contour(s)
        boolean is_first_contour = !abstractToConcreteContourMap.keySet().iterator().hasNext();
        if (is_first_contour) {
            int label_index = 0;
            for (AbstractCurve ac : abstractCurves) {
                result.add(new CircleContour(
                        outerBox.getCenterX() - 0.5 * (guideRadius * 3 * abstractCurves.size()) + 1.5 * guideRadius
                        + guideRadius * 3 * label_index,
                        outerBox.getCenterY(),
                        guideRadius, ac));
                label_index++;
            }
            DEB.out(2, "added first contours into diagram, labelled " + abstractCurves.get(0).getLabel());
            return result;
        }
        
        // general case : it's (they're) not our first contour
        if (zone.getNumContours() == 0) {
            // adding contour(s) outside everything else
            double minx = Double.MAX_VALUE;
            double maxx = Double.MIN_VALUE;
            double miny = Double.MAX_VALUE;
            double maxy = Double.MIN_VALUE;

            for (CircleContour c : drawnCircles) {
                if (c.getMinX() < minx) {
                    minx = c.getMinX();
                }
                if (c.getMaxX() > maxx) {
                    maxx = c.getMaxX();
                }
                if (c.getMinY() < miny) {
                    miny = c.getMinY();
                }
                if (c.getMaxY() > maxy) {
                    maxy = c.getMaxY();
                }
            }
            if (abstractCurves.size() == 1) {
                if (maxx - minx < maxy - miny) {// R
                    result.add(new CircleContour(
                            maxx + guideRadius * 1.5,
                            (miny + maxy) * 0.5,
                            guideRadius, abstractCurves.get(0)));
                } else {// B
                    result.add(new CircleContour(
                            (minx + maxx) * 0.5,
                            maxy + guideRadius * 1.5,
                            guideRadius, abstractCurves.get(0)));
                }
            } else if (abstractCurves.size() == 2) {
                if (maxx - minx < maxy - miny) {// R
                    result.add(new CircleContour(
                            maxx + guideRadius * 1.5,
                            (miny + maxy) * 0.5,
                            guideRadius, abstractCurves.get(0)));
                    result.add(new CircleContour(
                            minx - guideRadius * 1.5,
                            (miny + maxy) * 0.5,
                            guideRadius, abstractCurves.get(1)));
                } else {// T
                    result.add(new CircleContour(
                            (minx + maxx) * 0.5,
                            maxy + guideRadius * 1.5,
                            guideRadius, abstractCurves.get(0)));
                    result.add(new CircleContour(
                            (minx + maxx) * 0.5,
                            miny - guideRadius * 1.5,
                            guideRadius, abstractCurves.get(1)));
                }
            } else {
                if (maxx - minx < maxy - miny) {// R
                    double lowy = (miny + maxy) * 0.5 - 0.5 * abstractCurves.size() * guideRadius * 3 + guideRadius * 1.5;
                    for (int i = 0; i < abstractCurves.size(); i++) {
                        result.add(new CircleContour(
                                maxx + guideRadius * 1.5,
                                lowy + i * 3 * guideRadius,
                                guideRadius, abstractCurves.get(i)));
                    }
                } else {
                    double lowx = (minx + maxx) * 0.5 - 0.5 * abstractCurves.size() * guideRadius * 3 + guideRadius * 1.5;
                    for (int i = 0; i < abstractCurves.size(); i++) {
                        result.add(new CircleContour(
                                lowx + i * 3 * guideRadius,
                                maxy + guideRadius * 1.5,
                                guideRadius, abstractCurves.get(i)));
                    }
                }
            }
            return result;
        }

        ConcreteZone cz = makeConcreteZone(zone);
        Area a = new Area(cz.getShape(outerBox));
        if (a.isEmpty()) {
            throw new CannotDrawException("cannot put a nested contour into an empty region");
        }

        DEB.show(4, a, "area for " + debugIndex);

        // special case : one contour inside another with no other interference between
        // look at the final diagram - find the corresponding zone
        DEB.out(2, "");
        if (zone.getNumContours() > 0 && abstractCurves.size() == 1) {
            //System.out.println("look for "+zone.debug()+" in "+last_diag.debug());
            // not the outside zone - locate the zone in the last diag
            AbstractBasicRegion zoneInLast = null;
            Iterator<AbstractBasicRegion> abrIt = lastDiagram.getZoneIterator();
            while (abrIt.hasNext() && zoneInLast == null) {
                AbstractBasicRegion abrInLast = abrIt.next();
                if (abrInLast.isLabelEquivalent(zone)) {
                    zoneInLast = abrInLast;
                }
            }
            DEB.assertCondition(zoneInLast != null, "failed to locate zone in final diagram");

            // how many neighbouring abrs?
            abrIt = lastDiagram.getZoneIterator();
            ArrayList<AbstractCurve> nbring_curves = new ArrayList<AbstractCurve>();
            while (abrIt.hasNext()) {
                AbstractBasicRegion abrInLast = abrIt.next();
                AbstractCurve ac = zoneInLast.getStraddledContour(abrInLast);
                if (ac != null) {
                    if (ac.getLabel() != abstractCurves.get(0).getLabel()) {
                        nbring_curves.add(ac);
                    }
                }
            }
            if (nbring_curves.size() == 1) {
                //  we should use concentric circles

                AbstractCurve acOutside = nbring_curves.get(0);
                // use the centre of the relevant contour
                DEB.assertCondition(acOutside != null, "did not find containing contour");
                CircleContour ccOutside = abstractToConcreteContourMap.get(acOutside);
                DEB.assertCondition(ccOutside != null, "did not find containing circle");
                if (ccOutside != null) {
                    DEB.out(2, "putting contour " + abstractCurves.get(0) + " inside " + acOutside.getLabel());
                    double rad = Math.min(guideRadius, ccOutside.radius - smallestRadius);
                    if (rad > 0.99 * smallestRadius) {
                        // build a co-centric contour
                        CircleContour attempt = new CircleContour(
                                ccOutside.cx, ccOutside.cy, rad, abstractCurves.get(0));
                        if (circleInArea(attempt, a)) {
                            if (rad > 2 * smallestRadius) // shrink the co-centric contour a bit
                            {
                                attempt = new CircleContour(
                                        ccOutside.cx, ccOutside.cy, rad - smallestRadius, abstractCurves.get(0));
                            }
                            result.add(attempt);
                            return result;
                        }
                    }
                } else {
                    System.out.println("warning : did not find expected containing circle...");
                }
            } else if (nbring_curves.size() == 2) {
                //  we should put a circle along the line between two existing centres
                AbstractCurve ac1 = nbring_curves.get(0);
                AbstractCurve ac2 = nbring_curves.get(1);

                CircleContour cc1 = abstractToConcreteContourMap.get(ac1);
                CircleContour cc2 = abstractToConcreteContourMap.get(ac2);

                if (cc1 != null && cc2 != null) {
                    boolean in1 = zone.is_in(ac1);
                    boolean in2 = zone.is_in(ac2);

                    double step_c1_c2_x = cc2.cx - cc1.cx;
                    double step_c1_c2_y = cc2.cy - cc1.cy;

                    double step_c1_c2_len = Math.sqrt(step_c1_c2_x * step_c1_c2_x
                            + step_c1_c2_y * step_c1_c2_y);
                    double unit_c1_c2_x = 1.0;
                    double unit_c1_c2_y = 0.0;
                    if (step_c1_c2_len != 0.0) {
                        unit_c1_c2_x = step_c1_c2_x / step_c1_c2_len;
                        unit_c1_c2_y = step_c1_c2_y / step_c1_c2_len;
                    }

                    double p1x = cc1.cx + unit_c1_c2_x * cc1.radius * (in2 ? 1.0 : -1.0);
                    double p2x = cc2.cx + unit_c1_c2_x * cc2.radius * (in1 ? -1.0 : +1.0);
                    double cx = (p1x + p2x) * 0.5;
                    double max_radx = (p2x - p1x) * 0.5;
                    double p1y = cc1.cy + unit_c1_c2_y * cc1.radius * (in2 ? 1.0 : -1.0);
                    double p2y = cc2.cy + unit_c1_c2_y * cc2.radius * (in1 ? -1.0 : +1.0);
                    double cy = (p1y + p2y) * 0.5;
                    double max_rady = (p2y - p1y) * 0.5;
                    double max_rad = Math.sqrt(max_radx * max_radx + max_rady * max_rady);

                    // build a contour
                    CircleContour attempt = new CircleContour(
                            cx, cy, max_rad - smallestRadius, abstractCurves.get(0));
                    //DEB.show(3, attempt.getBigInterior());
                    if (circleInArea(attempt, a)) {
                        if (max_rad > 3 * smallestRadius) // shrink the co-centric contour a bit
                        {
                            attempt = new CircleContour(
                                    cx, cy, max_rad - 2 * smallestRadius, abstractCurves.get(0));
                        } else if (max_rad > 2 * smallestRadius) // shrink the co-centric contour a bit
                        {
                            attempt = new CircleContour(
                                    cx, cy, max_rad - smallestRadius, abstractCurves.get(0));
                        }
                        result.add(attempt);
                        return result;
                    }
                }
            }
        }

        // special case - inserting a nested contour into a part of a Venn2


        Rectangle bounds = a.getBounds();
        /*
         * // try from the middle of the bounds. double cx =
         * bounds.getCenterX(); double cy = bounds.getCenterX();
         * if(a.contains(cx, cy)) { if(labels.size() == 1) { // go for a circle
         * of the suggested size CircleContour attempt = new CircleContour(cx,
         * cy, guide_rad, labels.get(0)); if(containedIn(attempt, a)) {
         * result.add(attempt); return result; } } else { Rectangle box = new
         * Rectangle(cx - guide_rad/2) } }
         */
        if (abstractCurves.get(0) == null) {
            DEB.out(2, "putting unlabelled contour inside a zone - grid-style");
        } else {
            DEB.out(2, "putting contour " + abstractCurves.get(0).getLabel() + " inside a zone - grid-style");
        }

        // Use a grid approach to search for a space for the contour(s)
        int ni = (int) (bounds.getWidth() / smallestRadius) + 1;
        int nj = (int) (bounds.getHeight() / smallestRadius) + 1;
        PotentialCentre contained[][] = new PotentialCentre[ni][nj];
        double basex = bounds.getMinX();
        double basey = bounds.getMinY();
        if (DEB.level > 3) {
            System.out.println("--------");
        }
        for (int i = 0; i < ni; i++) {
            double cx = basex + i * smallestRadius;

            for (int j = 0; j < nj; j++) {
                double cy = basey + j * smallestRadius;
                //System.out.println("check for ("+cx+","+cy+") in region");
                contained[i][j] = new PotentialCentre(cx, cy, a.contains(cx, cy));
                if (DEB.level > 3) {
                    if (contained[i][j].ok) {
                        System.out.print("o");
                    } else {
                        System.out.print("x");
                    }
                }
            }
            if (DEB.level > 3) {
                System.out.println("");
            }
        }
        if (DEB.level > 3) {
            System.out.println("--------");
        }
        // look in contained[] for a large square

        int corneri = -1, cornerj = -1, size = -1;
        boolean isTall = true; // or isWide
        for (int i = 0; i < ni; i++) {
            for (int j = 0; j < nj; j++) {
                // biggest possible square?
                int max_sq = Math.min(ni - i, nj - j);
                for (int sq = size + 1; sq < max_sq + 1; sq++) {
                    // scan a square from i, j
                    DEB.out(2, "look for a box from (" + i + "," + j + ") size " + sq);

                    if (all_ok_in(i, i + (sq * abstractCurves.size()) + 1, j, j + sq + 1, contained, ni, nj)) {
                        DEB.out(2, "found a wide box, corner at (" + i + "," + j + "), size " + sq);
                        corneri = i;
                        cornerj = j;
                        size = sq;
                        isTall = false;
                    } else if (abstractCurves.size() > 1
                            && all_ok_in(i, i + sq + 1, j, j + (sq * abstractCurves.size()) + 1, contained, ni, nj)) {
                        DEB.out(2, "found a tall box, corner at (" + i + "," + j + "), size " + sq);
                        corneri = i;
                        cornerj = j;
                        size = sq;
                        isTall = true;
                    } else {
                        break; // neither wide nor tall worked - move onto next (x, y)
                    }
                }// loop for increasing sizes
            }// loop for j corner
        }// loop for i corner
        //System.out.println("best square is at corner ("+corneri+","+cornerj+"), of size "+size);
        if (size > 0) {
            PotentialCentre pc = contained[corneri][cornerj];
            double radius = size * smallestRadius * 0.5;
            double actualRad = radius;
            if (actualRad > 2 * smallestRadius) {
                actualRad -= smallestRadius;
            } else if (actualRad > smallestRadius) {
                actualRad = smallestRadius;
            }

            // have size, cx, cy
            DEB.out(2, "corner at " + pc.x + "," + pc.y + ", size " + size);

            ArrayList<CircleContour> centredCircles = new ArrayList<CircleContour>();

            double bx = bounds.getCenterX();
            double by = bounds.getCenterY();
            if (isTall) {
                by -= radius * (abstractCurves.size() - 1);
            } else {
                bx -= radius * (abstractCurves.size() - 1);
            }
            for (int labelIndex = 0;
                    centredCircles != null && labelIndex < abstractCurves.size();
                    labelIndex++) {
                AbstractCurve ac = abstractCurves.get(labelIndex);
                double x = bx;
                double y = by;
                if (isTall) {
                    y += 2 * radius * labelIndex;
                } else {
                    x += 2 * radius * labelIndex;
                }

                CircleContour attempt = new CircleContour(x, y,
                        Math.min(guideRadius, actualRad), ac);
                //DEB.show(3, attempt.getBigInterior());
                if (circleInArea(attempt, a)) {
                    centredCircles.add(attempt);
                } else {
                    centredCircles = null;
                    //Debug.show(a);
                }
            }
            if (centredCircles != null) {
                result.addAll(centredCircles);
                return result;
            }

            for (int labelIndex = 0; labelIndex < abstractCurves.size(); labelIndex++) {
                AbstractCurve ac = abstractCurves.get(labelIndex);
                double x = pc.x + radius;
                double y = pc.y + radius;
                if (isTall) {
                    y += 2 * radius * labelIndex;
                } else {
                    x += 2 * radius * labelIndex;
                }

                CircleContour attempt = new CircleContour(x, y,
                        Math.min(guideRadius, actualRad + smallestRadius), ac);
                if (circleInArea(attempt, a)) {
                    result.add(attempt);
                } else {
                    result.add(new CircleContour(x, y, actualRad, ac));
                }
            }
            return result;
        } else {
            throw new CannotDrawException("cannot fit nested contour into region");
        }
    }

    /**
     * Find two points where two circles meet
     * (null if they don't, equal points if they just touch)
     * @param c1x
     * @param c1y
     * @param rad1
     * @param c2x
     * @param c2y
     * @param rad2
     * @return
     */
    private double[][] intersctCircles(double c1x, double c1y, double rad1,
            double c2x, double c2y, double rad2) {

        double ret[][] = new double[2][2];
        double dx = c1x - c2x;
        double dy = c1y - c2y;
        double d2 = dx * dx + dy * dy;
        double d = Math.sqrt(d2);

        if (d > rad1 + rad2 || d < Math.abs(rad1 - rad2)) {
            return null; // no solution
        }

        double a = (rad1 * rad1 - rad2 * rad2 + d2) / (2 * d);
        double h = Math.sqrt(rad1 * rad1 - a * a);
        double x2 = c1x + a * (c2x - c1x) / d;
        double y2 = c1y + a * (c2y - c1y) / d;


        double paX = x2 + h * (c2y - c1y) / d;
        double paY = y2 - h * (c2x - c1x) / d;
        double pbX = x2 - h * (c2y - c1y) / d;
        double pbY = y2 + h * (c2x - c1x) / d;

        ret[0][0] = paX;
        ret[0][1] = paY;
        ret[1][0] = pbX;
        ret[1][1] = pbY;

        return ret;
    }

    /**
     * Is this circle in this area, including some slop for a gap.
     * Slop is smallestRadius.
     * @param c
     * @param a
     * @return
     */
    private boolean circleInArea(CircleContour c, Area a) {
        Area test = new Area(c.getFatInterior(smallestRadius));
        test.subtract(a);
        return test.isEmpty();
    }

    /**
     * Optionally, depending on DEB.level, add a CirclesPanel displaying the
     * current circles in drawnCircles to DEB's "filmstrip".
     * @param deb_level
     * @param debug_frame_index
     * @param size
     */
    private void addDebugView(int deb_level, // only show if deb_level >= global debug level
            int debug_frame_index,
            int size) {
        if (deb_level > DEB.level) {
            return;
        }

        // build a ConcreteDiagram for the current collection of circles
        ArrayList<ConcreteZone> shadedZones = new ArrayList<ConcreteZone>();
        ArrayList<ConcreteZone> unshadedZones = new ArrayList<ConcreteZone>();
        ArrayList<ConcreteSpider> spiders = new ArrayList<ConcreteSpider>();

        ArrayList<CircleContour> circles_copy = new ArrayList<CircleContour>();
        for (CircleContour c : drawnCircles) {
            circles_copy.add(new CircleContour(c));
        }
        CircleContour.fitCirclesToSize(circles_copy, size);
        ConcreteDiagram cd = new ConcreteDiagram(new Rectangle2D.Double(0, 0, size, size),
                circles_copy, shadedZones, unshadedZones, spiders);
        CirclesPanel cp = new CirclesPanel("debug frame " + debug_frame_index, "no failure",
                cd, true);
            
        DEB.addFilmStripShot(cp);
    }
}
/**
 * When considering a set of possible positions for a circle's centre,
 * we can build PotentialCentre objects to log considered positions and
 * whether they were OK (reklative to other circles) or not.
 * @author 
 *
 */
class PotentialCentre {

    double x;
    double y;
    boolean ok;

    PotentialCentre(double x, double y, boolean ok) {
        this.x = x;
        this.y = y;
        this.ok = ok;
    }
}
