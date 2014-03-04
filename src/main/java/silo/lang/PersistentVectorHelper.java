/*
 *
 *  Copyright 2014 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */

package silo.lang;

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.PersistentVector;

public class PersistentVectorHelper {

    public static IPersistentVector create() {
        return PersistentVector.emptyVector();
    }

    public static IPersistentVector create(Object... items) {
        return PersistentVector.create(items);
    }

    public static IPersistentVector create(Iterable items) {
        return PersistentVector.create(items);
    }

    public static Object get(IPersistentVector vector, int index) {
        return vector.nth(index);
    }

    public static Object get(IPersistentVector vector, int index, Object notFound) {
        return vector.nth(index, notFound);
    }

    public static IPersistentVector set(IPersistentVector vector, int index, Object value) {
        return vector.assocN(index, value);
    }

    public static IPersistentVector push(IPersistentVector vector, Object value) {
        return vector.cons(value);
    }
}
