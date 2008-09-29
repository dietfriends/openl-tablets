/*
 * Created on Jul 29, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Stack;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.main.SourceCodeURLTool;
import org.openl.syntax.ISyntaxNode;

/**
 * @author snshor
 *
 */
public class OpenLRuntimeException extends RuntimeException
{



	/**
	 * 
	 */
	private static final long serialVersionUID = -8422089115244904493L;
	Throwable cause;
	IBoundNode node;

	Stack openlCallStack = new Stack();

	public OpenLRuntimeException(Throwable cause, IBoundNode node)
	{
		this.cause = cause;
		this.node = node;
	}

	public void pushMethodNode(IBoundNode node)
	{
		openlCallStack.push(node);
	}

	/**
	 * @return
	 */
	public Throwable getCause()
	{
		return cause;
	}

	/**
	 * @return
	 */
	public IBoundNode getNode()
	{
		return node;
	}

	/**
	 * @return
	 */
	public Stack getOpenlCallStack()
	{
		return openlCallStack;
	}

	/**
	 *
	 */

	public void printStackTrace(PrintWriter stream)
	{

		Throwable rootCause = this;

		if (getCause() != null)
			rootCause = ExceptionUtils.getRootCause(this);

		stream.println(
			rootCause.getClass().getName() + ": " + rootCause.getMessage());

		ISyntaxNode syntaxNode = getNode().getSyntaxNode();

		SourceCodeURLTool. printCodeAndError(
			syntaxNode.getSourceLocation(),
			syntaxNode.getModule(),
			stream);

		SourceCodeURLTool.printSourceLocation(
			syntaxNode.getSourceLocation(),
			syntaxNode.getModule(),
			stream);

		Stack nodes = getOpenlCallStack();

		for (int i = 0; i < nodes.size(); i++)
		{
			IBoundNode node = (IBoundNode) nodes.elementAt(i);
			SourceCodeURLTool.printSourceLocation(
				node.getSyntaxNode().getSourceLocation(),
				node.getSyntaxNode().getModule(),
				stream);

		}

		rootCause.printStackTrace(stream);
	}

	/**
	 *
	 */

	public void printStackTrace()
	{
		printStackTrace(System.err);
	}

	/**
	 *
	 */

	public void printStackTrace(PrintStream s)
	{
		synchronized(s)
		{
			printStackTrace(new PrintWriter(s, true));
		}
	}

}
