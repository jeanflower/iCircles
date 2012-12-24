package icircles.abstractDualGraph;

import icircles.abstractDescription.AbstractBasicRegion;

import java.util.ArrayList;

public class AbstractDualNode {

    public AbstractBasicRegion abr;
    ArrayList<AbstractDualEdge> incidentEdges;

    AbstractDualNode(AbstractBasicRegion abr) {
        incidentEdges = new ArrayList<AbstractDualEdge>();
        this.abr = abr;
    }

    int degree() {
        return incidentEdges.size();
    }

    void removeEdge(AbstractDualEdge e) {
        incidentEdges.remove(e);
    }
}
