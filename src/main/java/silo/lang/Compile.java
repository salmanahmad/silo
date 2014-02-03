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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;

public class Compile {

    public static void writeBytecodeToDirectory(byte[] code, String targetDirectory) throws Exception {
        File test = new File(targetDirectory).getCanonicalFile();

        if(test.exists() && !test.isDirectory()) {
            throw new RuntimeException("Target '" + targetDirectory + "' is not a directory");
        }

        test.mkdirs();

        ClassReader reader = new ClassReader(code);
        String fileName = reader.getClassName() + ".class";

        File file = new File(targetDirectory, fileName);
        file.getParentFile().mkdirs();

        FileOutputStream output = new FileOutputStream(file);
        IOUtils.write(code, output);
    }

    public static void main(String[] args) throws Exception {
        // To not compile the standard library invoke Maven
        // with "-Dbootstrap.skip=true" on the commandline.
        // Example: "mvn test -Dbootstrap.skip=true"
        if(System.getProperty("bootstrap.skip") != null) {
            if(System.getProperty("bootstrap.skip").equals("true")) {
                return;
            }
        }

        Runtime runtime = new Runtime();
        String outputPath = args[0];

        for(int i = 1; i < args.length; i++) {
            String file = args[i];
            String source = FileUtils.readFileToString(new File(file));
            CompilationContext context = runtime.contextByCompiling(source);
            for(byte[] code : context.bytecode) {
                writeBytecodeToDirectory(code, outputPath);
            }
        }
    }
}