package org.openl.types.impl;

import java.util.Collection;
import java.util.Collections;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.types.DomainOpenClassAggregateInfo;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.StaticOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * {@link IOpenClass} implementation, that adds restriction for instances of this class by {@link IDomain}
 */
public class DomainOpenClass implements IOpenClass, BelongsToModuleOpenClass {
    private volatile StaticOpenClass staticOpenClass;

    private IDomain<?> domain;

    private IAggregateInfo aggregateInfo;
    private final IOpenClass baseClass;
    private final String name;
    private IMetaInfo metaInfo;
    private final ModuleOpenClass module;

    public DomainOpenClass(String name,
                           IOpenClass baseClass,
                           IDomain<?> domain,
                           ModuleOpenClass module,
                           IMetaInfo metaInfo) {
        assert name != null;
        this.baseClass = baseClass;
        this.name = name;
        this.metaInfo = metaInfo;
        this.domain = domain;
        this.module = module;
    }

    @Override
    public String getExternalRefName() {
        if (module == null) {
            throw new IllegalStateException("moduleName is not defined");
        }
        return "`" + module.getModuleName() + "`." + getName();
    }

    @Override
    public ModuleOpenClass getModule() {
        return module;
    }

    @Override
    public IDomain<?> getDomain() {
        return domain;
    }

    public void setDomain(IDomain<?> domain) {
        this.domain = domain;
    }

    /**
     * Overriden to add the possibility to return special aggregate info for DomainOpenClass
     *
     * @author DLiauchuk
     */
    @Override
    public IAggregateInfo getAggregateInfo() {
        if (aggregateInfo == null) {
            aggregateInfo = DomainOpenClassAggregateInfo.DOMAIN_AGGREGATE;
        }
        return aggregateInfo;
    }

    @Override
    public String getDisplayName(int mode) {
        return getName();
    }

    public IOpenClass getBaseClass() {
        return baseClass;
    }

    @Override
    public IOpenField getField(String fname) {
        return baseClass.getField(fname);
    }

    @Override
    public IOpenField getField(String fname, boolean strictMatch) throws AmbiguousFieldException {
        return baseClass.getField(fname, strictMatch);
    }

    @Override
    public IOpenField getIndexField() {
        return baseClass.getIndexField();
    }

    @Override
    public Class<?> getInstanceClass() {
        return baseClass.getInstanceClass();
    }

    @Override
    public IOpenMethod getConstructor(IOpenClass[] params) throws AmbiguousMethodException {
        return null;
    }

    @Override
    public IMetaInfo getMetaInfo() {
        return metaInfo;
    }

    @Override
    public IOpenMethod getMethod(String mname, IOpenClass[] classes) {
        return baseClass.getMethod(mname, classes);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getJavaName() {
        return baseClass.getJavaName();
    }

    @Override
    public String getPackageName() {
        return baseClass.getPackageName();
    }

    @Override
    public IOpenField getVar(String vname, boolean strictMatch) throws AmbiguousFieldException {
        return baseClass.getVar(vname, strictMatch);
    }

    @Override
    public boolean isAbstract() {
        return baseClass.isAbstract();
    }

    @SuppressWarnings("unchecked")
    public static boolean isFromValuesIncludedToValues(DomainOpenClass from, DomainOpenClass to, IOpenCast openCast) {
        IDomain<Object> fromDomain = (IDomain<Object>) from.getDomain();
        IDomain<Object> toDomain = (IDomain<Object>) to.getDomain();
        try {
            for (Object value : fromDomain) {
                if (!toDomain.selectObject(openCast != null ? openCast.convert(value) : value)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        if (baseClass.isAssignableFrom(ioc)) {
            if (ioc instanceof DomainOpenClass) {
                DomainOpenClass domainOpenClass = (DomainOpenClass) ioc;
                if (domainOpenClass.baseClass == baseClass) {
                    return isFromValuesIncludedToValues(domainOpenClass, this, null);
                }
            }
        }
        return false;
    }

    @Override
    public boolean isAssignableFrom(IType type) {
        return baseClass.isAssignableFrom(type);
    }

    @Override
    public boolean isInstance(Object instance) {
        return baseClass.isInstance(instance);
    }

    @Override
    public boolean isSimple() {
        return baseClass.isSimple();
    }

    @Override
    public boolean isArray() {
        return baseClass.isArray();
    }

    @Override
    public IOpenClass getComponentClass() {
        return getAggregateInfo().getComponentType(this);
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        return baseClass.newInstance(env);
    }

    @Override
    public Object nullObject() {
        return baseClass.nullObject();
    }

    @Override
    public void setMetaInfo(IMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    @Override
    public Collection<IOpenClass> superClasses() {
        return baseClass.superClasses();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void addType(IOpenClass type) {
    }

    @Override
    public IOpenClass findType(String typeName) {
        // Default implementation.
        return null;
    }

    @Override
    public Collection<IOpenClass> getTypes() {
        // Default implementation
        return Collections.emptyList();
    }

    @Override
    public Collection<IOpenField> getFields() {
        return baseClass.getFields();
    }

    @Override
    public Collection<IOpenField> getDeclaredFields() {
        return baseClass.getDeclaredFields();
    }

    @Override
    public Collection<IOpenMethod> getMethods() {
        return baseClass.getMethods();
    }

    @Override
    public Collection<IOpenMethod> getDeclaredMethods() {
        return baseClass.getMethods();
    }

    @Override
    public Iterable<IOpenMethod> methods(String name) {
        return baseClass.methods(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DomainOpenClass that = (DomainOpenClass) o;

        if (name.equals(that.name)) {
            return domain.equals(that.domain);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public Iterable<IOpenMethod> constructors() {
        return Collections.emptyList();
    }

    @Override
    public IOpenClass getArrayType(int dim) {
        return AOpenClass.getArrayType(this, dim);
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public IOpenField getStaticField(String fname) {
        return null;
    }

    @Override
    public IOpenField getStaticField(String name, boolean strictMatch) {
        return null;
    }

    @Override
    public Collection<IOpenField> getStaticFields() {
        return null;
    }

    @Override
    public IOpenClass toStaticClass() {
        if (staticOpenClass == null) {
            synchronized (this) {
                if (staticOpenClass == null) {
                    staticOpenClass = new StaticDomainOpenClass(this);
                }
            }
        }
        return staticOpenClass;
    }

    @Override
    public boolean isStatic() {
        return false;
    }
}
