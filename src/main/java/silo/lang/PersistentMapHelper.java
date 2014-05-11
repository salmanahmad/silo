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

import java.util.Map;

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.IPersistentMap;
import com.github.krukow.clj_lang.PersistentHashMap;
import com.github.krukow.clj_lang.RT;

public class PersistentMapHelper {

    public static IPersistentMap create() {
        return PersistentHashMap.emptyMap();
    }

    public static IPersistentMap create(Object... items) {
        return PersistentHashMap.create(items);
    }

    public static IPersistentMap create(Map items) {
        return PersistentHashMap.create(items);
    }

    public static int size(IPersistentMap map) {
        return PersistentVectorHelper.length(keys(map));
    }

    public static IPersistentVector keys(IPersistentMap map) {
        Iterable i = (Iterable)RT.keys(map);

        if(i == null) {
            return PersistentVectorHelper.create();
        } else {
            return PersistentVectorHelper.create(i);
        }
    }

    public static IPersistentMap merge(IPersistentMap map1, IPersistentMap map2) {
        IPersistentMap output = map1;

        IPersistentVector keys = keys(map2);
        for(int i = 0; i < PersistentVectorHelper.length(keys); i++) {
            Object key = PersistentVectorHelper.get(keys, i);
            output = set(output, key, get(map2, key));
        }

        return output;
    }

    public static boolean contains(IPersistentMap map, Object key) {
        Object o = new Object();
        return get(map, key, o) != o;
    }

    public static Object get(IPersistentMap map, Object key) {
        return map.valAt(key);
    }

    public static Object get(IPersistentMap map, Object key, Object notFound) {
        return map.valAt(key, notFound);
    }

    public static IPersistentMap set(IPersistentMap map, Object key, Object value) {
        return map.assoc(key, value);
    }

    public static IPersistentMap remove(IPersistentMap map, Object key) {
        return map.without(key);
    }
}
