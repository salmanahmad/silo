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

package silo.util;

import silo.lang.*;

import java.util.*;
import java.io.*;
import java.net.URL;
import org.apache.commons.lang3.StringEscapeUtils;


public class Helper {

    // TODO: Move this to a standard library that is accessible by the runtime.
    // Additionally, even implement the chainExpressions as a macro. Keep in mind,
    // that if I do implement it as a macro, the algorithm would be slightly different
    // because I need to reverse the entire tree and not just case case down an
    // element.
    public static Node cascadeNode(Node element, Object object) {

        if(object instanceof Node) {
            Node node = (Node)object;

            if(node.getChildren().get(1) instanceof Node) {
                Node child = (Node)node.getChildren().get(1);

                if(child.getLabel().equals(new Symbol("|")) ||
                   child.getLabel().equals(new Symbol(".")) ||
                   child.getLabel().equals(new Symbol("::"))) {
                       return new Node(
                           node.getLabel(),
                           node.getChildren().get(0),
                           Helper.cascadeNode(element, child)
                       );
                 }
            }

            return new Node(
                node.getLabel(),
                node.getChildren().get(0),
                new Node(
                    element.getLabel(),
                    node.getChildren().get(1),
                    element.getChildren().get(0)
                )
            );
        } else {
            return new Node(element.getLabel(), object, element.getChildren().get(0));
        }
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
