/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.syntax.*;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.error.ISyntaxNodeError;

/**
 * The <code>IBoundCode</code> interface is designed to provide a common
 * protocol for objects what describes bound code.
 * 
 * @author snshor
 * 
 */
public interface IBoundCode {
    
    /**
     * Gets errors what was found during binding process.
     * 
     * @return syntax errors
     */
    ISyntaxNodeError[] getErrors();

    /**
     * Gets link to parsed code that was used in binding process.
     * 
     * @return source code
     */
    IParsedCode getParsedCode();

    /**
     * Gets link to top node of bound code objects hierarchy. Bound code
     * represented as a tree of bound code objects (nodes).
     * 
     * @return top node
     */
    IBoundNode getTopNode();
}
