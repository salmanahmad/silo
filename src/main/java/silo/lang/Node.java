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

// TODO: Should I rename "Node" to "Tree"?
// TODO: Change "children" to a PersistentDataStructure

public class Node {
    // TODO: Rename label to head?
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
        if(o instanceof Node) {
            Node node = (Node)o;
            return Helper.equals(this.label, node.label) && Helper.equals(this.children, node.children);
        } else {
            return super.equals(o);
        }
    }

    public int hashCode() {
        int hash = 1;
        return Helper.hashCombine(Helper.hashCode(this.label.hashCode()), this.children.hashCode());
    }

    public Object getLabel() {
        return label;
    }

    public void addChild(Object child) {
        this.children.add(child);
    }

    public void addChildren(Vector children) {
        this.children.addAll(children);
    }

    public void addChildren(Node node) {
        this.children.addAll(node.getChildren());
    }

    public Vector getChildren() {
        // TODO: Optimize this. Use persistent data structures.
        return new Vector(children);
    }

    // TODO: Should the following methods also be static so that it makes jav interop seamless?
    public Node getChildNode(Symbol name) {
        for(Object child : children) {
            if(child instanceof Node) {
                Node node = (Node)child;
                if(node.getLabel().equals(name)) {
                    return node;
                }
            }
        }

        return null;
    }

    public Object getFirstChild() {
        return children.get(0);
    }

    public Object getSecondChild() {
        return children.get(1);
    }

    public Object getLastChild() {
        return children.lastElement();
    }

    public static Node chainNodes(Object label, Object... elements) {
        if(elements == null || elements.length == 0) {
            return new Node(label);
        }

        Node node = new Node(label, elements[elements.length - 1], null);

        for(int i = elements.length - 2; i >= 0; i--) {
            Object element = elements[i];
            node = new Node(label, elements, node);
        }

        return node;
    }

    public static Node flattenTree(Node node) {
        Node buffer = new Node(null);
        for(Object o : node.children) {
            if(o instanceof Node) {
                buffer.addChildren(flattenTree((Node)o));
            } else {
                buffer.addChild(o);
            }
        }

        return buffer;
    }

    // TODO - Update this so it takes an anonymous function instead...
    public static Node flattenTree(Node node, Object... labels) {
        return null;
    }

    public static Node splitChain(Node node, Object chainLabel) {
        if(node.getLabel().equals(chainLabel)) {
            if(node.getSecondChild() instanceof Node) {
                Node split = splitChain((Node)node.getSecondChild(), chainLabel);

                Node chain = new Node(null);
                chain.addChild(node.getFirstChild());
                chain.addChildren((Node)split.getFirstChild());

                return new Node(null,
                    chain,
                    split.getSecondChild()
                );
            } else {
                return new Node(null,
                    new Node(null, node.getFirstChild(), node.getSecondChild()),
                    null
                );
            }
        } else {
            return new Node(null,
                new Node(null),
                node
            );
        }
    }

    public String toString() {
        String s = this.label == null ? "" : Helper.toQuotedString(this.label);
        s += "(";

        Vector<String> children = new Vector<String>();
        for(Object child : this.children) {
            children.add(Helper.toQuotedString(child));
        }

        s += StringUtils.join(children, ", ");

        s += ")";
        return s;
    }

    public String toPrettyString() {
        return this.toPrettyString(0);
    }

    // TODO: I should prettyPrint the label if it is a node to make the pretty printing a bit easier to understand.
    private String toPrettyString(int indent) {
        StringBuffer buffer = new StringBuffer();

        String spacing = "";
        String tab = "  ";
        for(int i = 0; i < indent; i++) {
            spacing += tab;
        }

        buffer.append(spacing);
        buffer.append(this.label == null ? "" : Helper.toQuotedString(this.label));
        buffer.append("(");

        if(this.children.size() == 0) {
            buffer.append(")");
        } else {
            for(Object child : this.children) {
                buffer.append("\n");

                if(child instanceof Node) {
                    Node node = (Node)child;
                    buffer.append(node.toPrettyString(indent + 1));
                } else {
                    buffer.append(spacing);
                    buffer.append(tab);
                    buffer.append(Helper.toQuotedString(child));
                }
            }

            buffer.append("\n");
            buffer.append(spacing);
            buffer.append(")");
        }

        return buffer.toString();
    }
}
