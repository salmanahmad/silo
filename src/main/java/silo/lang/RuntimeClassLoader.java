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

import java.net.URLClassLoader;
import java.net.URL;

public class RuntimeClassLoader extends URLClassLoader {

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
        } catch(Exception e) {
            // TODO - Remove this comment
            //e.printStackTrace();
            throw new RuntimeException("Could not load class.");
        }

        return klass;
    }
}

