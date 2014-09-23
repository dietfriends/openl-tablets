package org.openl.rules.tbasic.runtime.debug;

import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.tbasic.Algorithm;

public class TBasicAlgorithmTraceObject extends ATableTracerNode {
    /**
     * @param traceObject
     */
    public TBasicAlgorithmTraceObject(Algorithm traceObject, Object[] inputParams) {
        super("tbasic", traceObject, inputParams);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.base.INamedThing#getDisplayName(int)
     */
    public String getDisplayName(int mode) {
        Algorithm algorithm = (Algorithm) getTraceObject();
        return String.format("Algorithm %s", asString(algorithm, mode));
    }
}
