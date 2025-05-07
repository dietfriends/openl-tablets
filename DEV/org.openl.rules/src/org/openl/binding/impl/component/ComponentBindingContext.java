package org.openl.binding.impl.component;

import java.util.HashMap;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.binding.exception.DuplicatedTypeException;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * Binding context for different Openl components.<br>
 * Handles {@link ComponentOpenClass} for which binding is performed.<br>
 * And a map of internal types that are found during binding.<br>
 * <p>
 * Was created by extracting functionality from {@link ModuleBindingContext} of 20192 revision.
 *
 * @author DLiauchuk
 */
public class ComponentBindingContext extends BindingContextDelegator {

    private final ComponentOpenClass componentOpenClass;

    private Map<String, IOpenClass> internalTypes;

    public ComponentBindingContext(IBindingContext delegate, ComponentOpenClass componentOpenClass) {
        super(delegate);
        this.componentOpenClass = componentOpenClass;
    }

    public ComponentOpenClass getComponentOpenClass() {
        return componentOpenClass;
    }

    @Override
    public IOpenClass addType(IOpenClass type) throws DuplicatedTypeException {
        final String typeName = type.getName();
        if (internalTypes == null) {
            internalTypes = new HashMap<>();
        }
        if (internalTypes.containsKey(typeName)) {
            IOpenClass openClass = internalTypes.get(typeName);
            if (openClass == type) {
                return type;
            }
            if (openClass.getPackageName().equals(type.getPackageName())) {
                throw new DuplicatedTypeException(null, typeName);
            }
        }

        internalTypes.put(typeName, type);
        return type;
    }

    @Override
    public ILocalVar addVar(String namespace, String name, IOpenClass type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IMethodCaller findMethodCaller(String namespace,
                                          String methodName,
                                          IOpenClass[] parTypes) throws AmbiguousMethodException {

        IMethodCaller imc = null;
        if (ISyntaxConstants.THIS_NAMESPACE.equals(namespace)) {
            imc = MethodSearch.findMethod(methodName, parTypes, this, componentOpenClass, true);
        }

        return imc != null ? imc : super.findMethodCaller(namespace, methodName, parTypes);
    }

    @Override
    public IOpenClass findType(String typeName) throws AmbiguousTypeException {
        if (internalTypes != null) {
            IOpenClass ioc = internalTypes.get(typeName);
            if (ioc != null) {
                return ioc;
            }
        }

        IOpenClass type = componentOpenClass.findType(typeName);
        if (type != null) {
            return type;
        }

        return super.findType(typeName);
    }

    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch) throws AmbiguousFieldException {
        IOpenField res = null;
        if (namespace.equals(ISyntaxConstants.THIS_NAMESPACE)) {
            res = componentOpenClass.getField(name, strictMatch);
        }

        return res != null ? res : super.findVar(namespace, name, strictMatch);
    }

    protected IOpenClass findOpenClass(IOpenClass openClass) {
        if (openClass == null) {
            return null;
        }
        IOpenClass componentOpenClass = openClass;
        int dim = 0;
        while (componentOpenClass.isArray()) {
            componentOpenClass = componentOpenClass.getComponentClass();
            dim++;
        }
        if (isComponentSpecificOpenClass(componentOpenClass)) {
            IOpenClass thisContextOpenClass = this.findType(
                    componentOpenClass.getName());
            if (thisContextOpenClass != null) {
                return dim > 0 ? thisContextOpenClass.getArrayType(dim) : thisContextOpenClass;
            }
        }
        return openClass;
    }

    protected boolean isComponentSpecificOpenClass(IOpenClass componentOpenClass) {
        return false;
    }
}
