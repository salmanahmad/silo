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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Vector;

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.PersistentVector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;

public class Main {
    public static void main(String[] args) throws Exception {
        Runtime runtime = new Runtime();
        IPersistentVector vector = PersistentVector.create(args);
        runtime.spawn("silo.cli.main", vector).await();
        runtime.shutdown();
    }
}