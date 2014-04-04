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

import com.github.krukow.clj_lang.IEditableCollection;
import com.github.krukow.clj_lang.ITransientCollection;

import com.github.krukow.clj_lang.RT;

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

    public static IPersistentVector subVector(IPersistentVector vector, int start, int end) {
        return RT.subvec(vector, start, end);
    }

    public static IPersistentVector removeFirst(IPersistentVector vector) {
        return subVector(vector, 1, length(vector));
    }

    public static IPersistentVector push(IPersistentVector vector, Object value) {
        return vector.cons(value);
    }

    public static IPersistentVector concat(IPersistentVector a, IPersistentVector b) {
        IPersistentVector head = a;
        IPersistentVector tail = b;

        ITransientCollection buffer = ((IEditableCollection)head).asTransient();
        for(int i = 0; i < length(tail); i++) {
            buffer.conj(get(tail, i));
        }

        return (IPersistentVector)buffer.persistent();
    }

    public static IPersistentVector pop(IPersistentVector vector) {
        return subVector(vector, 0, length(vector) - 1);
    }

    public static int length(IPersistentVector vector) {
        return vector.length();
    }

    public static Object last(IPersistentVector vector) {
        return get(vector, length(vector) - 1);
    }
}
