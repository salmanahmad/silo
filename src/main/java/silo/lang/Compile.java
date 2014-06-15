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
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;

import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.JarEntry;
import java.util.jar.Attributes;


public class Compile {

    public static void writeBytecodeToDirectory(List<byte[]> code, String targetDirectory) throws Exception {
        File test = new File(targetDirectory).getCanonicalFile();

        if(test.exists() && !test.isDirectory()) {
            throw new RuntimeException("Target '" + targetDirectory + "' is not a directory");
        }

        test.mkdirs();

        for(byte[] b : code) {
            ClassReader reader = new ClassReader(b);
            String fileName = reader.getClassName() + ".class";

            File file = new File(targetDirectory, fileName);
            file.getParentFile().mkdirs();

            FileOutputStream output = new FileOutputStream(file);
            IOUtils.write(b, output);
        }
    }

    public static void writeBytecodeToJarFile(List<byte[]> code, String targetFile) throws Exception {
        File test = new File(targetFile);
        if(test.getParent() != null) {
            test.getParentFile().mkdirs();
        }

        FileOutputStream stream = new FileOutputStream(targetFile);

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name("Silo-Main"), (new ClassReader(code.get(code.size() - 1))).getClassName().replace("/", "."));

        JarOutputStream target = new JarOutputStream(stream, manifest);

        for(byte[] b : code) {
            ClassReader reader = new ClassReader(b);
            JarEntry entry = new JarEntry(reader.getClassName() + ".class");
            target.putNextEntry(entry);
            target.write(b, 0, b.length);
            target.closeEntry();
        }

        target.close();
    }

    public static void main(String[] args) throws Exception {
        // To not compile the standard library invoke Maven
        // with "-DskipSilo=true" on the commandline.
        // Example: "mvn test -DskipSilo=true"
        if(System.getProperty("skipSilo") != null) {
            if(System.getProperty("skipSilo").equals("true")) {
                return;
            }
        }

        Runtime runtime = new Runtime();
        String outputPath = args[0];

        for(int i = 1; i < args.length; i++) {
            String file = args[i];
            String source = FileUtils.readFileToString(new File(file));
            CompilationContext context = runtime.contextByCompiling(FilenameUtils.getName(file), source);
            writeBytecodeToDirectory(context.bytecode, outputPath);
        }
    }
}