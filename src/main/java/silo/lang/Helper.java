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

package silo.lang;

import java.util.*;
import java.io.*;
import java.net.URL;
import org.apache.commons.lang3.StringEscapeUtils;

public class Helper {

    public static int hashCode(Object object) {
        if(object == null) {
            return 0;
        } else {
            return object.hashCode();
        }
    }

    public static int hashCombine(int a, int b) {
        // Inspired from Clojure.
        a ^= b + 0x9e3779b9 + (a << 6) + (a >> 2);
        return a;
    }

    public static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public static String toQuotedString(Object o) {
        if(o == null) {
            return "null";
        }

        if(o instanceof String) {
            return "\"" + StringEscapeUtils.escapeJava(o.toString()) + "\"";
        }

        return o.toString();
    }

    public static String[] getResourceListing(Class klass, String path) {
        try {
          URL dirURL = klass.getResource(path);
          return new File(dirURL.toURI()).list();
        } catch(Exception e) {
          return null;
        }
    }

    public static String[] getResourceListing(String path) {
        return getResourceListing(Helper.class, path);
    }

    public static String readResource(Class klass, String path) {
        try {
            InputStream in = klass.getResourceAsStream(path);
            String content = new Scanner(in).useDelimiter("\\A").next();
            return content;
        } catch(Exception e) {
            return "";
        }
    }

    public static String readResource(String path) {
        return readResource(Helper.class, path);
    }
}