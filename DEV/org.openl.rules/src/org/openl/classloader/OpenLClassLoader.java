package org.openl.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilerConfiguration;

import org.openl.util.IOUtils;

/**
 * ClassLoader that have bundle classLoaders. When loading any class, at first tries to find it in bundle classLoaders
 * if can`t tries to find it in his parent.
 */
public class OpenLClassLoader extends GroovyClassLoader {

    private final Set<ClassLoader> bundleClassLoaders = new LinkedHashSet<>();

    private final Map<String, byte[]> generatedClasses = new ConcurrentHashMap<>();

    private final Set<GroovyClassLoader> groovyClassLoaders = new HashSet<>();

    public OpenLClassLoader(ClassLoader parent) {
        this(new URL[0], parent);
    }

    private static CompilerConfiguration buildOpenLConfiguration() {
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.addCompilationCustomizers(OpenLGroovyCompilerCustomizer.getInstance());
        return configuration;
    }

    public OpenLClassLoader(URL[] urls, ClassLoader parent) {
        this(urls, parent, buildOpenLConfiguration(), true);
    }

    private OpenLClassLoader(URL[] urls,
                             ClassLoader parent,
                             CompilerConfiguration config,
                             boolean useConfigurationClasspath) {
        super(parent, config, useConfigurationClasspath);
        if (urls != null && urls.length > 0) {
            for (URL url : urls) {
                addURL(url);
            }
            setResourceLoader(new GroovyResourceLoader(getResourceLoader()));
        } else {
            // Performance improvement, but groovy classes are loaded only from project classpath
            setResourceLoader(filename -> null);
        }
    }

    private static ClassLoader applyGroovySupport(ClassLoader classLoader, URL[] urls) {
        if (classLoader instanceof GroovyClassLoader) {
            return classLoader;
        } else {
            GroovyClassLoader groovyClassLoader = new GroovyClassLoader(classLoader, buildOpenLConfiguration(), true);
            groovyClassLoader.setResourceLoader(new GroovyResourceLoader(groovyClassLoader.getResourceLoader()));
            if (urls != null) {
                for (URL url : urls) {
                    groovyClassLoader.addURL(url);
                }
            }
            return groovyClassLoader;
        }
    }

    public void addClassLoader(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "Bundle class loader cannot be null");

        if (classLoader == this) {
            throw new IllegalArgumentException("Bundle class loader cannot register himself");
        }

        if (classLoader instanceof OpenLClassLoader && ((OpenLClassLoader) classLoader).containsClassLoader(this)) {
            throw new IllegalArgumentException("Bundle class loader cannot register class loader containing himself");
        }
        ClassLoader classLoader1 = applyGroovySupport(classLoader, new URL[0]);
        if (classLoader1 instanceof GroovyClassLoader && classLoader1 != classLoader) {
            groovyClassLoaders.add((GroovyClassLoader) classLoader1);
        }
        bundleClassLoaders.add(classLoader1);
    }

    public void addGeneratedClass(String name, byte[] byteCode) {
        if (generatedClasses.putIfAbsent(name, byteCode) != null) {
            throw new OpenLGeneratedClassAlreadyDefinedException(
                    String.format("Byte code for class '%s' is already defined.", name));
        }

    }

    public boolean containsClassLoader(ClassLoader classLoader) {
        if (bundleClassLoaders.contains(classLoader)) {
            return true;
        }

        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            if (bundleClassLoader instanceof OpenLClassLoader && ((OpenLClassLoader) bundleClassLoader)
                    .containsClassLoader(classLoader)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Set<ClassLoader> c = Collections.newSetFromMap(new IdentityHashMap<>());
        c.add(this);
        return loadClass(name, c);
    }

    protected Class<?> loadClass(String name, Set<ClassLoader> c) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            byte[] byteCode = generatedClasses.get(name);
            if (byteCode != null) {
                try {
                    var clazz = defineClass(name, byteCode, 0, byteCode.length);
                    var packageName = clazz.getPackageName();
                    if (getDefinedPackage(packageName) == null) {
                        definePackage(packageName, null, null, null, null, null, null, null);
                    }
                    return clazz;
                } catch (Exception e1) {
                    throw e;
                }
            }
            Class<?> clazz = findClassInBundles(name, c);
            if (clazz != null) {
                return clazz;
            }
            throw e;
        }
    }

    private Class<?> findClassInBundles(String name, Set<ClassLoader> c) {
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            if (c.contains(bundleClassLoader)) {
                continue;
            }
            c.add(bundleClassLoader);
            try {
                // if current class loader contains appropriate class - it will
                // be returned as a result
                //
                Class<?> clazz;
                if (bundleClassLoader instanceof OpenLClassLoader && bundleClassLoader.getParent() == this) {
                    OpenLClassLoader sbc = (OpenLClassLoader) bundleClassLoader;
                    clazz = sbc.findLoadedClass(name);
                    if (clazz == null) {
                        clazz = sbc.findClassInBundles(name, c);
                    }
                } else {
                    if (bundleClassLoader instanceof OpenLClassLoader) {
                        clazz = ((OpenLClassLoader) bundleClassLoader).loadClass(name, c);
                    } else {
                        clazz = bundleClassLoader.loadClass(name);
                    }
                }
                if (clazz != null) {
                    return clazz;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }

        return null;
    }

    private URL findResourceInBundles(String name, Set<ClassLoader> c) {
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            if (c.contains(bundleClassLoader)) {
                continue;
            }
            c.add(bundleClassLoader);
            URL url;
            if (bundleClassLoader instanceof OpenLClassLoader && bundleClassLoader.getParent() == this) {
                OpenLClassLoader sbcl = (OpenLClassLoader) bundleClassLoader;
                url = sbcl.findResourceInBundles(name, c);
            } else if (bundleClassLoader instanceof OpenLClassLoader) {
                OpenLClassLoader sbcl = (OpenLClassLoader) bundleClassLoader;
                url = sbcl.getResource(name, c);
            } else {
                url = bundleClassLoader.getResource(name);
            }
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private InputStream findResourceAsStreamInBundles(String name, Set<ClassLoader> c) {
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            if (c.contains(bundleClassLoader)) {
                continue;
            }
            c.add(bundleClassLoader);
            InputStream inputStream;
            if (bundleClassLoader instanceof OpenLClassLoader && bundleClassLoader.getParent() == this) {
                OpenLClassLoader sbcl = (OpenLClassLoader) bundleClassLoader;
                inputStream = sbcl.findResourceAsStreamInBundles(name, c);
            } else if (bundleClassLoader instanceof OpenLClassLoader) {
                OpenLClassLoader sbcl = (OpenLClassLoader) bundleClassLoader;
                inputStream = sbcl.getResourceAsStream(name, c);
            } else {
                inputStream = bundleClassLoader.getResourceAsStream(name);
            }
            if (inputStream != null) {
                return inputStream;
            }
        }
        return null;
    }

    private void findResourcesInBundles(String name,
                                        Set<ClassLoader> c,
                                        List<Enumeration<URL>> queue) throws IOException {
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            if (c.contains(bundleClassLoader)) {
                continue;
            }
            c.add(bundleClassLoader);
            Enumeration<URL> resources = null;
            if (bundleClassLoader instanceof OpenLClassLoader && bundleClassLoader.getParent() == this) {
                OpenLClassLoader sbcl = (OpenLClassLoader) bundleClassLoader;
                sbcl.findResourcesInBundles(name, c, queue);
            } else if (bundleClassLoader instanceof OpenLClassLoader) {
                OpenLClassLoader sbcl = (OpenLClassLoader) bundleClassLoader;
                resources = sbcl.getResources(name, c);
            } else {
                resources = bundleClassLoader.getResources(name);
            }
            if (resources != null) {
                queue.add(resources);
            }
        }
    }

    private Enumeration<URL> getResources(String name, Set<ClassLoader> c) throws IOException {
        LinkedList<Enumeration<URL>> resources = new LinkedList<>();
        resources.add(super.getResources(name));
        findResourcesInBundles(name, c, resources);
        return new CompoundEnumeration(resources);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Set<ClassLoader> c = Collections.newSetFromMap(new IdentityHashMap<>());
        c.add(this);
        return getResources(name, c);
    }

    private URL getResource(String name, Set<ClassLoader> c) {
        URL url = super.getResource(name);
        if (url != null) {
            return url;
        }
        return findResourceInBundles(name, c);
    }

    @Override
    public URL getResource(String name) {
        Set<ClassLoader> c = Collections.newSetFromMap(new IdentityHashMap<>());
        c.add(this);
        return getResource(name, c);
    }

    private InputStream getResourceAsStream(String name, Set<ClassLoader> c) {
        InputStream inputStream = super.getResourceAsStream(name);
        if (inputStream != null) {
            return inputStream;
        }
        return findResourceAsStreamInBundles(name, c);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        Set<ClassLoader> c = Collections.newSetFromMap(new IdentityHashMap<>());
        c.add(this);
        return getResourceAsStream(name, c);
    }

    private void addURLToList(List<URL> urls, URL url) {
        for (URL existingURL : urls) {
            if (existingURL.sameFile(url)) {
                return;
            }
        }
        urls.add(url);
    }

    private void addURLsFromBundles(Set<ClassLoader> c, List<URL> urls) {
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            if (c.contains(bundleClassLoader)) {
                continue;
            }
            c.add(bundleClassLoader);
            if (bundleClassLoader instanceof OpenLClassLoader && bundleClassLoader.getParent() == this) {
                OpenLClassLoader sbcl = (OpenLClassLoader) bundleClassLoader;
                sbcl.addURLsFromBundles(c, urls);
            } else if (bundleClassLoader instanceof OpenLClassLoader) {
                OpenLClassLoader sbcl = (OpenLClassLoader) bundleClassLoader;
                for (URL url : sbcl.getURLs(c)) {
                    addURLToList(urls, url);
                }
            } else if (bundleClassLoader instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader) bundleClassLoader).getURLs()) {
                    addURLToList(urls, url);
                }
            }
        }
    }

    private URL[] getURLs(Set<ClassLoader> c) {
        List<URL> urls = new ArrayList<>();
        for (URL url : super.getURLs()) {
            addURLToList(urls, url);
        }
        addURLsFromBundles(c, urls);
        return urls.toArray(new URL[0]);
    }

    @Override
    public URL[] getURLs() {
        Set<ClassLoader> c = Collections.newSetFromMap(new IdentityHashMap<>());
        c.add(this);
        return getURLs(c);
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            groovyClassLoaders.forEach(IOUtils::closeQuietly);
        }
    }

    /*
     * A utility class that will enumerate over an enumerations queue.
     */
    private static final class CompoundEnumeration implements Enumeration<URL> {
        private final Queue<Enumeration<URL>> queue;
        private Enumeration<URL> cursor;

        public CompoundEnumeration(Queue<Enumeration<URL>> queue) {
            this.queue = queue;
        }

        private boolean next() {
            if (cursor != null && cursor.hasMoreElements()) {
                return true;
            }
            while (!queue.isEmpty()) {
                cursor = queue.poll();
                if (cursor != null && cursor.hasMoreElements()) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasMoreElements() {
            return next();
        }

        public URL nextElement() {
            if (!next()) {
                throw new NoSuchElementException();
            }
            return cursor.nextElement();
        }
    }
}
