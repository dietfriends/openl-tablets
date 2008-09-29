/*
 * Created on May 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.impl;

import org.openl.types.IMethodCaller;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class MethodCaller implements IMethodCaller
{
	IOpenMethod method;
	public MethodCaller(IOpenMethod method)
	{
		this.method = method;
	}

  /* (non-Javadoc)
   * @see org.openl.types.IMethodCaller#invoke(java.lang.Object[])
   */
  public Object invoke(Object target, Object[] params, IRuntimeEnv env)
  {
    return method.invoke(target, params, env);
  }

  /**
   * @return
   */
  public IOpenMethod getMethod()
  {
    return method;
  }

}
