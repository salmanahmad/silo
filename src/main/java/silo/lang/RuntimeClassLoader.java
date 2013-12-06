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
}

