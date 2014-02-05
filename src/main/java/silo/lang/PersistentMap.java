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

import java.util.Map;
import com.github.krukow.clj_lang.IPersistentMap;
import com.github.krukow.clj_lang.PersistentHashMap;

public class PersistentMap {

    public final IPersistentMap map;

    public PersistentMap(IPersistentMap map) {
        this.map = map;
    }

    public PersistentMap(Object... items) {
        this.map = PersistentHashMap.create(items);
    }

    public PersistentMap(Map items) {
        this.map = PersistentHashMap.create(items);
    }

    public Object get(Object key) {
        return map.valAt(key);
    }
}
