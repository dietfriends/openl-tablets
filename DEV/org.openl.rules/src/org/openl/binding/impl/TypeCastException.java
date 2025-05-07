/**
 * Created Jul 21, 2007
 */
package org.openl.binding.impl;

import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 */
public class TypeCastException extends SyntaxNodeException {

    private static final long serialVersionUID = 5570752529258476343L;

    private final IOpenClass from;
    private final IOpenClass to;

    public TypeCastException(ISyntaxNode node, IOpenClass from, IOpenClass to) {
        super(String.format("Cannot convert from '%s' to '%s'.", from.getName(), to.getName()), null, node);

        this.from = from;
        this.to = to;
    }

    public IOpenClass getFrom() {
        return from;
    }

    public IOpenClass getTo() {
        return to;
    }

}
