package org.openl.rules;

import junit.framework.Assert;

import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * Helper class for building OpenClassJavaWrapper and getting XlsModuleSyntaxNode from it. To get everything you need 
 * for your tests just extend this class. 
 *  
 * 
 * @author DLiauchuk
 *
 */
public abstract class BaseOpenlBuilderHelper {
    
    private XlsModuleSyntaxNode xsn;    
    private OpenClassJavaWrapper wrapper;
    
    public BaseOpenlBuilderHelper(String _src) {
        build(_src);        
    }
    
    protected void buildXlsModuleSyntaxNode(String fileToBuildWrapper) {        
        buildJavaWrapper(fileToBuildWrapper);
        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClassWithErrors().getMetaInfo();
        xsn = xmi.getXlsModuleNode();        
    }
    
    protected OpenClassJavaWrapper buildJavaWrapper(String fileToBuildWrapper) {
        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        wrapper = OpenClassJavaWrapper.createWrapper("org.openl.xls", ucxt, fileToBuildWrapper);
        return wrapper;
    }
    
    @Deprecated
    protected TableSyntaxNode findTable(String tableName, TableSyntaxNode[] tsns) {
        TableSyntaxNode result = null;
        for (TableSyntaxNode tsn : tsns) {
            if (tableName.equals(tsn.getDisplayName())) {
                result = tsn;   
            }
        }
        return result;
    }
    
    protected TableSyntaxNode findTable(String tableName) {
        TableSyntaxNode result = null;
        for (TableSyntaxNode tsn : getTableSyntaxNodes()) {
            if (tableName.equals(tsn.getDisplayName())) {
                result = tsn;   
            }
        }
        return result;
    }

    protected TableSyntaxNode[] getTableSyntaxNodes() {  
        TableSyntaxNode[] tsns = xsn.getXlsTableSyntaxNodes();
        return tsns;
    }
    
    protected XlsModuleSyntaxNode getModuleSuntaxNode() {
        return xsn;
    }
    
    protected OpenClassJavaWrapper getJavaWrapper() {
        return wrapper;
    }
    
    private void build(String fileToBuildWrapper) {
        buildXlsModuleSyntaxNode(fileToBuildWrapper);        
    }
    
    protected Object invokeMethod(String methodName) {
        IOpenClass __class = getJavaWrapper().getOpenClassWithErrors(); 
        
        IOpenMethod testMethod = __class.getMatchingMethod(methodName, new IOpenClass[] {});
        
        Assert.assertNotNull(String.format("Method with name %s exists", methodName), testMethod);
        
        Object[] __params = new Object[0];
        org.openl.vm.IRuntimeEnv environment = new org.openl.vm.SimpleVM().getRuntimeEnv();
        Object __myInstance = __class.newInstance(environment);
        
        Object result = testMethod.invoke(__myInstance, __params, environment);        
        
        return result;
    }
}
