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

import silo.util.Helper;

import java.util.Vector;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

// TODO: I need to figure out how to include meta-data like line number
// and character positions in the Node class.

public class Node {
    Object label;
    Vector children;

    public Node(Object label, Vector<Object> children) {
        this.label = label;
        this.children = children;
    }
    
    public Node(Object label, Object... children) {
        this.label = label;
        this.children = new Vector(Arrays.asList(children));
    }

    public boolean equals(Object o) {
        if(Node.class.equals(o.getClass())) {
            Node node = (Node)o;
            return Helper.equals(this.label, node.label) && Helper.equals(this.children, node.children);
        } else {
            return super.equals(o);
        }
    }

    public void addChild(Object child) {
        this.children.add(child);
    }

    public void addChildren(Vector children) {
        this.children.addAll(children);
    }

    public Vector getChildren() {
        return new Vector(children);
    }

    public String toString() {
        String s = Helper.toQuotedString(this.label);
        s += "(";

        Vector<String> children = new Vector<String>();
        for(Object child : this.children) {
            children.add(Helper.toQuotedString(child));
        }

        s += StringUtils.join(children, ",");

        s += ")";
        return s;
    }
}
