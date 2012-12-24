package icircles.concreteDiagram;

import icircles.recomposition.RecompData;

import java.util.ArrayList;

public class BuildStep {

    public ArrayList<RecompData> recomp_data;
    public BuildStep next = null;

    BuildStep(RecompData rd) {
        recomp_data = new ArrayList<RecompData>();
        recomp_data.add(rd);
    }
}
