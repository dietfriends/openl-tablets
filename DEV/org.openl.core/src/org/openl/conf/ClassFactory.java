/*
 * Created on Jun 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.binding.MethodUtil;

/**
 * @author snshor
 */
public class ClassFactory extends AConfigurationElement {
    private static final Logger LOG = LoggerFactory.getLogger(ClassFactory.class);
    private static final Class<?>[] NO_PARAMS = {};
    private String className;
    private String extendsClassName;

    protected boolean singleton;

    Object cachedObject;

    public static Object newInstance(Class<?> cc, String uri) {
        try {
            return cc.getDeclaredConstructor().newInstance();
        } catch (Exception | LinkageError t) {
            throw new OpenLConfigurationException(String.format("Failed to instantiate class '%s'.", cc.getTypeName()),
                    uri,
                    t);
        }
    }

    public static Object newInstance(String classname, IConfigurableResourceContext cxt, String uri) {
        try {
            return cxt.getClassLoader().loadClass(classname).getDeclaredConstructor().newInstance();
        } catch (Exception | LinkageError t) {
            throw new OpenLConfigurationException(String.format("Failed to instantiate class '%s'.", classname),
                    uri,
                    t);
        }
    }

    public static Class<?> validateClassExistsAndPublic(String className, ClassLoader cl, String uri) {
        Class<?> c;
        try {
            c = cl.loadClass(className);
        } catch (Exception | LinkageError t) {
            throw new OpenLConfigurationException(String.format("Failed to load class '%s'.", className), uri, t);
        }

        if (!Modifier.isPublic(c.getModifiers())) {
            throw new OpenLConfigurationException(String.format("Class '%s' must be a public.", c.getTypeName()),
                    uri,
                    null);
        }

        return c;

    }

    public static Method validateHasMethod(Class<?> clazz, String methodName, Class<?>[] params, String uri) {
        Method m;
        try {
            m = clazz.getMethod(methodName, params);
        } catch (Exception | LinkageError t) {
            String methodString = MethodUtil.printMethod(methodName, params);
            throw new OpenLConfigurationException(String
                    .format("Method '%s' is not found in class '%s'.", methodString, clazz.getTypeName()), uri, t);
        }

        if (!Modifier.isPublic(m.getModifiers())) {
            throw new OpenLConfigurationException(String.format("Method '%s' is not a public.", methodName), uri, null);
        }
        return m;
    }

    public static void validateHaveNewInstance(Class<?> clazz, String uri) {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            throw new OpenLConfigurationException(String.format("Expected non abstract class '%s'.",
                    clazz.getTypeName()), uri, null);
        }

        try {
            Constructor<?> constr = clazz.getConstructor(NO_PARAMS);
            if (!Modifier.isPublic(constr.getModifiers())) {
                throw new OpenLConfigurationException(String.format("Default constructor is not public in class '%s'.",
                        clazz.getTypeName()), uri, null);
            }
        } catch (OpenLConfigurationException ex) {
            throw ex;
        } catch (Exception | LinkageError t) {
            LOG.debug("Error occurred: ", t);
            throw new OpenLConfigurationException(String.format("Default constructor is not found in class '%s'.",
                    clazz.getTypeName()), uri, null);
        }
    }

    public static void validateSuper(Class<?> clazz, Class<?> superClazz, String uri) {
        if (!superClazz.isAssignableFrom(clazz)) {
            String verb = superClazz.isInterface() ? "implement" : "extend";
            throw new OpenLConfigurationException(
                    String.format("Class '%s' does not %s '%s'.", clazz.getTypeName(), verb, superClazz.getTypeName()),
                    uri,
                    null);
        }

    }

    public String getClassName() {
        return className;
    }

    public String getExtendsClassName() {
        return extendsClassName;
    }

    public synchronized Object getResource(IConfigurableResourceContext cxt) {
        if (isSingleton()) {
            if (cachedObject == null) {
                cachedObject = getResourceInternal(cxt);
            }
            return cachedObject;
        }

        return getResourceInternal(cxt);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IConfigurationElement#getResource(org.openl.newconf.IConfigurationContext)
     */
    protected Object getResourceInternal(IConfigurableResourceContext cxt) {
        try {
            return cxt.getClassLoader().loadClass(className).getDeclaredConstructor().newInstance();
        } catch (Exception | LinkageError t) {
            throw new OpenLConfigurationException(String.format("Failed to instantiate class '%s'.", className),
                    getUri(),
                    t);
        }
    }

    public boolean isSingleton() {
        return singleton;
    }

    public void setClassName(String string) {
        className = string;
    }

    public void setExtendsClassName(String string) {
        extendsClassName = string;
    }

    public void setSingleton(boolean b) {
        singleton = b;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf.IConfigurationContext)
     */
    @Override
    public void validate(IConfigurableResourceContext cxt) {
        Class<?> c = validateClassExistsAndPublic(className, cxt.getClassLoader(), getUri());
        if (getExtendsClassName() != null) {
            Class<?> c2 = validateClassExistsAndPublic(getExtendsClassName(), cxt.getClassLoader(), getUri());
            validateSuper(c, c2, getUri());
        }
        validateHaveNewInstance(c, getUri());
    }

}
