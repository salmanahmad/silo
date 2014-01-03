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

package silo.lang.compiler;

import silo.lang.Runtime;
import silo.lang.*;
import silo.lang.expressions.*;

import java.util.Vector;
import java.util.HashMap;
import java.lang.reflect.Array;

import org.objectweb.asm.commons.GeneratorAdapter;

// TODO: Consider moving Compiler and Parser out of the 'compiler' package and simply into the 'lang' package instead.

public class Compiler {

    public static HashMap<String, Class> primitives = new HashMap<String, Class>();
    static {
        primitives.put("silo.core.boolean", Boolean.TYPE);
        primitives.put("silo.core.char", Character.TYPE);
        primitives.put("silo.core.byte", Byte.TYPE);
        primitives.put("silo.core.short", Short.TYPE);
        primitives.put("silo.core.int", Integer.TYPE);
        primitives.put("silo.core.long", Long.TYPE);
        primitives.put("silo.core.float", Float.TYPE);
        primitives.put("silo.core.double", Double.TYPE);
        primitives.put("silo.core.void", Void.TYPE);
    }

    public static Vector<Class> compile(CompilationContext context, Node node) {
        Object expanded = expandMacros(node, context);
        Expression expression = buildExpression(expanded);
        expression.emit(context);

        return context.classes;

        /*
        node = expandMacros(node);

        Vector<Symbol> declarations = extractDeclarations(node);
        checkForDuplicates(declarations);

        Vector<Node> containsUncompiledMacros(node)

        Object expressionTree = transformToExpressions(node);

        emit(expressionTree);
        */
    }

    public static Object expandMacros(Object node, CompilationContext context) {
        Object previous = null;

        while(true) {
            previous = node;
            node = expandMacrosOnce(node, context);

            if(previous.equals(node)) {
                break;
            }
        }

        return node;
    }

    private static void checkForDuplicates(Vector<Node> declarations) {
        
    }

    public static Object expandMacrosOnce(Object o, CompilationContext context) {
        // TODO: Namespaces and packages with macros. How are those resolved or imported?
        // TODO: I should make a special case and NOT attempt to macroExpand special forms - that said, I should expand their children...right?

        if(o instanceof Node) {
            Node node = (Node)o;
            Object label = node.getLabel();
            Vector children = node.getChildren();

            Class klass = null;

            try {
                klass = resolveType(label, context);
            } catch(Exception e) {
                klass = null;
            }

            if(isMacro(klass)) {
                o = context.runtime.eval(klass, children.toArray());
            } else {
                node = new Node(expandMacrosOnce(label, context));

                for(Object child : children) {
                    node.addChild(expandMacrosOnce(child, context));
                }

                o = node;
            }
        }

        return o;
    }

    public static boolean isMacro(Class klass) {
        if(klass == null) {
            return false;
        } else {
            Function.Definition definition = (Function.Definition)klass.getAnnotation(Function.Definition.class);
            if(definition == null) {
                return false;
            } else {
                return definition.macro();
            }
        }
    }

    private static boolean containsUncompiledMacros(Object value) {
        return false;
    }

    public static Vector<Node> extractDeclarations(Node node) {
        return null;
    }

    public static Expression buildExpression(Object value) {
        if(value instanceof Node) {
            Node node = (Node)value;
            Object label = node.getLabel();

            // TODO: Create an "ExpressionBuilder" interface that all the Expressions have
            // as a nested class. Then, just create a list of "ExpressionBuilder" as "Special-Forms"
            // and literate over them instead of having this huge if-else statement.

            // TODO: If you do not create an ExpressionBuilder atleast re-organize this switch table...

            if(node.getLabel() == null) {
                return Block.build(node);
            } else if(label.equals(new Symbol("do"))) {
                return Block.build(node);
            } else if(label.equals(new Symbol("array"))) {
                return LiteralArrayType.build(node);
            } else if(label.equals(new Symbol("arraynew"))) {
                return LiteralArray.build(node);
            } else if(label.equals(new Symbol("function"))) {
                return FunctionExpression.build(node);
            } else if(label.equals(new Symbol("declare"))) {
                return Declare.build(node);
            } else if(label.equals(new Symbol("loop"))) {
                return Loop.build(node);
            } else if(label.equals(new Symbol("break"))) {
                return Break.build(node);
            } else if(label.equals(new Symbol("branch"))) {
                return Branch.build(node);
            } else if(label.equals(new Symbol("return"))) {
                return Return.build(node);
            } else if(label.equals(new Symbol("invokevirtual"))) {
                // TODO: Add macro called "dispatch" to wrap this...
                return InvokeVirtual.build(node);
            } else if(label.equals(new Symbol("#"))) {
                return InvokeVirtual.build(node);
            } else if(label.equals(new Symbol("."))) {
                return Access.build(node);
            } else if(label.equals(new Symbol("="))) {
                return Assign.build(node);
            } else if(LogicalOperation.accepts(node.getLabel())) {
                return LogicalOperation.build(node);
            } else if(MathOperation.accepts(node.getLabel())) {
                return MathOperation.build(node);
            } else if(RelationalOperation.accepts(node.getLabel())) {
                return RelationalOperation.build(node);
            } else {
                return Invoke.build(node);
            }
        } else if(value instanceof Symbol) {
            if(value.equals(new Symbol("return"))) {
                return Return.build(new Node(new Symbol("return")));
            } else if(value.equals(new Symbol("break"))) {
                return Break.build(new Node(new Symbol("break")));
            } else if(value.equals(new Symbol("continue"))) {
                // TODO
            }

            return Access.build((Symbol)value);
        } else if(value instanceof Boolean) {
            return new LiteralBoolean((Boolean)value);
        } else if(value instanceof Byte) {
            return new LiteralByte((Byte)value);
        } else if(value instanceof Character) {
            return new LiteralCharacter((Character)value);
        } else if(value instanceof Short) {
            return new LiteralShort((Short)value);
        } else if(value instanceof Integer) {
            return new LiteralInteger((Integer)value);
        } else if(value instanceof Long) {
            return new LiteralLong((Long)value);
        } else if(value instanceof Float) {
            return new LiteralFloat((Float)value);
        } else if(value instanceof Double) {
            return new LiteralDouble((Double)value);
        } else if(value instanceof String) {
            return new LiteralString((String)value);
        } else if(value == null) {
            return new LiteralNull();
        } else {
            throw new RuntimeException("Unhandled case..." + value.toString());
        }
    }

    public static void emit(Node node) {
        
    }

    public static boolean isCategory2(Class klass) {
        return klass.equals(Double.TYPE) || klass.equals(Long.TYPE);
    }

    public static void pop(Class klass, GeneratorAdapter generator) {
        if(isCategory2(klass)) {
            generator.pop2();
        } else if(!klass.equals(Void.TYPE)) {
            generator.pop();
        }
    }

    public static void dup(Class klass, GeneratorAdapter generator) {
        if(isCategory2(klass)) {
            generator.dup2();
        } else if(!klass.equals(Void.TYPE)) {
            generator.dup();
        }
    }

    public static Class resolveType(String qualifiedName, RuntimeClassLoader loader) {
        try {
            Class klass = Compiler.primitives.get(qualifiedName);

            if(klass == null) {
                klass = loader.loadClass(qualifiedName);
            }

            return klass;
        } catch(ClassNotFoundException e) {
            return null;
        }
    }

    public static Class resolveType(Vector<Symbol> path, CompilationContext context) {
        String name = null;
        for(Symbol symbol : path) {
            if(name == null) {
                name = symbol.toString();
            } else {
                name += "." + symbol.toString();
            }
        }

        for(String p : context.imports) {
            String qualifiedName = p;
            if(qualifiedName.equals("")) {
                qualifiedName = name;
            } else {
                qualifiedName += "." + name;
            }

            Class klass = resolveType(qualifiedName, context.runtime.loader);

            if(klass != null) {
                return klass;
            }
        }

        return null;
    }

    public static Class resolveType(Symbol name, CompilationContext context) {
        Vector<Symbol> path = new Vector<Symbol>();
        path.add(name);

        return resolveType(path, context);
    }

    public static Class resolveType(Node node, CompilationContext context) {
        if(node.getLabel().equals(new Symbol("array"))) {
            Vector children = node.getChildren();

            Object type = null;
            int depth = 1;

            if(children.size() == 1) {
                type = children.get(0);
            } else if(children.size() == 2) {
                Object o = children.get(1);
                if(!(o instanceof Integer)) {
                    throw new RuntimeException("Invalid type identifier: " + node.toString());
                }

                type = children.get(0);
                depth = ((Integer)o).intValue();
            } else {
                throw new RuntimeException("Invalid type identifier: " + node.toString());
            }

            Class temp = resolveType(type, context);

            if(temp != null) {
                for(int i = 0; i < depth; i++) {
                    temp = Array.newInstance(temp, 0).getClass();
                }
            }

            return temp;
        } else {
            Vector<Symbol> path = Node.symbolListFromNode(Node.flattenTree(node, new Symbol(".")));

            if(path == null) {
                throw new RuntimeException("Invalid type identifier: " + node.toString());
            } else {
                return resolveType(path, context);
            }
        }
    }

    public static Class resolveType(Object o, CompilationContext context) {
        if(o instanceof Symbol) {
            return resolveType((Symbol)o, context);
        } else if(o instanceof Node) {
            return resolveType((Node)o, context);
        } else {
            throw new RuntimeException("Could not resolve symbol: " + o + " of type: " + o.getClass());
        }
    }

    public static Vector resolveIdentifierPath(Vector<Symbol> path, CompilationContext context) {
        int index = 0;
        String name = null;

        for(Symbol symbol : path) {
            index++;

            if(name == null) {
                name = symbol.toString();
            } else {
                name += "." + symbol.toString();
            }

            for(String p : context.imports) {
                String qualifiedName = p;
                if(qualifiedName.equals("")) {
                    qualifiedName = name;
                } else {
                    qualifiedName += "." + name;
                }

                Class klass = resolveType(qualifiedName, context.runtime.loader);

                if(klass != null) {
                    Vector remaining = new Vector();
                    for(int i = index; i < path.size(); i++) {
                        remaining.add(path.get(i));
                    }

                    Vector vec = new Vector();
                    vec.add(klass);
                    vec.add(remaining);

                    return vec;
                }
            }
        }

        return null;
    }
}



