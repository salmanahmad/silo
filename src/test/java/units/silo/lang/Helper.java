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

import silo.lang.*;

import java.util.*;
import java.io.*;
import java.net.URL;
import org.apache.commons.lang3.StringEscapeUtils;

// TODO: Move this class to the lang pakcage and rename it as Util.

public class Helper {

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
