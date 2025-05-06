package org.openl.rules.method;

import java.util.Map;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.vm.CacheMode;
import org.openl.rules.vm.ResultNotFoundException;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IModuleInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.ExecutableMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

public abstract class ExecutableRulesMethod extends ExecutableMethod implements ITablePropertiesMethod, IModuleInfo {

    private ITableProperties properties;
    // FIXME: it should be AMethodBasedNode but currently it will be
    // ATableBoundNode due to TestSuiteMethod instance of
    // ExecutableRulesMethod(but test table is firstly data table)
    private ATableBoundNode boundNode;
    private boolean hasAliasTypeParams;
    private IOpenCast[] aliasDatatypeCasts;

    private String moduleName;

    @Override
    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public ExecutableRulesMethod(IOpenMethodHeader header, ATableBoundNode boundNode) {
        super(header);
        this.boundNode = boundNode;
        hasAliasTypeParams = false;
        if (header != null) {
            int i = 0;
            ICastFactory castFactory = new CastFactory();
            for (IOpenClass param : header.getSignature().getParameterTypes()) {
                if (param instanceof DomainOpenClass) {
                    hasAliasTypeParams = true;
                    if (aliasDatatypeCasts == null) {
                        aliasDatatypeCasts = new IOpenCast[header.getSignature().getNumberOfParameters()];
                    }
                    if (param.getInstanceClass() != null) {
                        aliasDatatypeCasts[i] = castFactory
                                .getCast(JavaOpenClass.getOpenClass(param.getInstanceClass()), param);
                    }
                }
                i++;
            }
        }
    }

    private Boolean cacheble = null;

    protected boolean isMethodCacheable() {
        if (cacheble == null) {
            if (getMethodProperties() == null) {
                cacheble = Boolean.FALSE;
            } else {
                Boolean cacheable = getMethodProperties().getCacheable();
                cacheble = Boolean.TRUE.equals(cacheable);
            }
        }
        return cacheble;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return Tracer.invoke(invoke2, target, params, env, this);
    }

    private final Invokable invoke2 = (Invokable) this::invoke2;

    private Object invoke2(Object target, Object[] params, IRuntimeEnv env) {
        if (hasAliasTypeParams) {
            for (int i = 0; i < getSignature().getNumberOfParameters(); i++) {
                if (aliasDatatypeCasts[i] != null) {
                    aliasDatatypeCasts[i].convert(params[i]); // Validate alias
                    // datatypes
                }
            }
        }
        if (env instanceof SimpleRulesRuntimeEnv) {
            SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = (SimpleRulesRuntimeEnv) env;
            Object result;
            boolean isSimilarStep = false;
            if (simpleRulesRuntimeEnv.isMethodArgumentsCacheEnable() && isMethodCacheable()) {
                try {
                    result = simpleRulesRuntimeEnv.getArgumentCachingStorage().findInCache(this, params);
                } catch (ResultNotFoundException e) {
                    result = innerInvoke(target, params, env);
                    if (CacheMode.READ_WRITE.equals(simpleRulesRuntimeEnv.getCacheMode())) {
                        simpleRulesRuntimeEnv.getArgumentCachingStorage().putToCache(this, params, result);
                    }
                }
            } else {
                result = innerInvoke(target, params, env);
            }
            return result;
        } else {
            return innerInvoke(target, params, env);
        }
    }

    protected abstract Object innerInvoke(Object target, Object[] params, IRuntimeEnv env);

    public void setBoundNode(ATableBoundNode node) {
        this.boundNode = node;
    }

    public void clearForExecutionMode() {
        setBoundNode(null);
        ITableProperties methodProperties = getMethodProperties();
        if (methodProperties != null) {
            methodProperties.setModulePropertiesTableSyntaxNode(null);
            methodProperties.setCategoryPropertiesTableSyntaxNode(null);
            methodProperties.setPropertiesSection(null);
        }
    }

    public ATableBoundNode getBoundNode() {
        return boundNode;
    }

    @Override
    public Map<String, Object> getProperties() {
        if (getMethodProperties() != null) {
            return getMethodProperties().getAllProperties();
        }
        return null;

    }

    @Override
    public ITableProperties getMethodProperties() {
        return properties;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }

    protected void initProperties(ITableProperties tableProperties) {
        this.properties = tableProperties;
    }

    /**
     * Overridden to get access to {@link TableSyntaxNode} from current implementation.
     */
    @Override
    public TableSyntaxNode getSyntaxNode() {
        if (boundNode != null) {
            return boundNode.getTableSyntaxNode();
        }

        return null;
    }

    public boolean isAlias() {
        return false;
    }
}
