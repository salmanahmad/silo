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


import org.junit.*;
import java.util.*;

import silo.lang.*;
import silo.lang.compiler.*;
import silo.lang.compiler.grammar.*;

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.IPersistentMap;

public class CollectionsTest {

    @Test
    public void testConcat() {
        IPersistentVector a = PersistentVectorHelper.create("a", "b");
        IPersistentVector b = PersistentVectorHelper.create("c", "d");
        IPersistentVector c = PersistentVectorHelper.concat(a, b);

        Assert.assertEquals(a, PersistentVectorHelper.create("a", "b"));
        Assert.assertEquals(b, PersistentVectorHelper.create("c", "d"));
        Assert.assertEquals(c, PersistentVectorHelper.create("a", "b", "c", "d"));
    }

    @Test
    public void testMerge() {
        IPersistentMap a = PersistentMapHelper.create("a", "b");
        IPersistentMap b = PersistentMapHelper.create("c", "d");
        IPersistentMap c = PersistentMapHelper.merge(a, b);

        Assert.assertEquals(c, PersistentMapHelper.create("a", "b", "c", "d"));
    }
}
