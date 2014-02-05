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

public class PersistentVector {

    public final com.github.krukow.clj_lang.IPersistentVector vector;

    public PersistentVector(com.github.krukow.clj_lang.IPersistentVector vector) {
        this.vector = vector;
    }

    public PersistentVector(Object... items) {
        this.vector = com.github.krukow.clj_lang.PersistentVector.create(items);
    }

    public PersistentVector(Iterable items) {
        this.vector = com.github.krukow.clj_lang.PersistentVector.create(items);
    }
}
