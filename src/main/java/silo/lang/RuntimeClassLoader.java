/*
 *
 *  Copyright 2012 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */

package silo.lang;

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

import java.net.URLClassLoader;
import java.net.URL;

public class RuntimeClassLoader extends URLClassLoader {

    HashMap<String, Class> lookAsideCache = new HashMap<String, Class>();

    public RuntimeClassLoader() {
        super(new URL[] {});
    }

    public RuntimeClassLoader(ClassLoader parent) {
        super(new URL[] {}, parent);
    }

    public Class loadClass(byte[] b) {
        Class klass = null;

        try {
            // TODO - This seems really slow. Investigate more.
            klass = defineClass(null, b, 0, b.length);
            lookAsideCache.put(klass.getName(), klass);
        } catch(Exception e) {
            // TODO - Remove this comment
            //e.printStackTrace();
            throw new RuntimeException("Could not load class.");
        }

        return klass;
    }

    public Class<?> findClass(String name) throws ClassNotFoundException {
        // TODO: Should I override this to try to compile a name if it is not found when doing the forward declaration? It will not work for mututally recursive functions but it could make this nicer. Or atleast have a look-aside cache.
        return super.findClass(name);
    }

    public Class resolveType(String name) {
        if(lookAsideCache.containsKey(name)) {
            return lookAsideCache.get(name);
        } else {
            try {
                Class klass = this.loadClass(name);
                lookAsideCache.put(name, klass);
                return klass;
            } catch(ClassNotFoundException e) {
                lookAsideCache.put(name, null);
                return null;
            }
        }
    }

    public Package getPackage(String name) {
        return super.getPackage(name);
    }

    public Package[] getPackages() {
        return super.getPackages();
    }

    public Set getPackagesSet() {
        Package[] list = getPackages();
        Set packages = new HashSet();

        for(Package p : list) {
            packages.add(p.getName());
        }

        return packages;
    }
}

