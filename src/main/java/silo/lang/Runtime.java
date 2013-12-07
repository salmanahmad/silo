/*
 *
 *  Copyright 2013 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */

package silo.lang;

import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class Runtime {
    RuntimeClassLoader loader;

    public Runtime() {
        this.loader = new RuntimeClassLoader();
    }

    public Runtime(RuntimeClassLoader loader) {
        this.loader = loader;
    }

    public RuntimeClassLoader loader() {
        return loader;
    }

}