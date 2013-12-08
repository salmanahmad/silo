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
        this.children.addAll(node.children);
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
                           Node.cascadeNode(element, child)
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
