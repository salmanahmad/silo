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

import silo.lang.*;
import silo.lang.Runtime;
import silo.lang.expressions.*;
import silo.lang.expressions.Package;

import java.util.Vector;
import java.util.HashMap;
import java.lang.reflect.Array;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.github.krukow.clj_lang.IPersistentVector;
import com.github.krukow.clj_lang.PersistentVector;

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

    public static Object expandCode(CompilationContext context, Object code) {
        return buildExpression(code).scaffold(context);
    }

    public static Vector<Class> compileExpandedCode(CompilationContext context, Object code) {
        buildExpression(code).emit(context);
        return context.classes;
    }

    public static Vector<Class> compile(CompilationContext context, Object code) {
        // TODO: Next consider updating this logic with the "macro-extract" algorithm described
        // in the TODO.md file... That way I have two steps: "macro-extract", "macro-expand", and "scaffold".
        // Macro-Extract just extracts out the macros and populates the symbol table... that I need and makes them available... macro-expand just
        // expands with the current defined macros (independent of macro-extract). Scaffold populates the symbol
        // table...

        // TODO: Also consider adding a special form for "proto" or "declare" to forward declare types and function definitions.
        // This is really only useful for macros since other functions do not need forward declaration.

        code = buildExpression(code).scaffold(context);
        buildExpression(code).emit(context);

        return context.classes;

        /*
        buildExpression(node).scaffold(context);

        for(String name : context.symbolTable.keySet()) {
            CompilationContext.SymbolEntry entry = context.symbolTable.get(name);
            context.namespaces.push(entry.namespace);

            buildExpression(entry.code).emit(context);

            context.namespaces.pop();
            context.symbolTable.remove(name);
        }

        return context.classes;
        */

        /*
        Object expanded = expandMacros(node, context);
        Expression expression = buildExpression(expanded);

        expression.emitDeclaration(context);
        expression.emit(context);

        return context.classes;
        */

        /*
        node = expandMacros(node);

        Vector<Symbol> declarations = extractDeclarations(node);
        checkForDuplicates(declarations);

        Vector<Node> containsUncompiledMacros(node)

        Object expressionTree = transformToExpressions(node);

        emit(expressionTree);
        */
    }

    public static Object scaffoldNode(Node node, CompilationContext context) {
        Object l = buildExpression(node.getLabel()).scaffold(context);
        Node n = new Node(l, node.getChildren());
        n.meta = node.meta;

        return scaffoldNodeChildren(n, context);
    }

    public static Object scaffoldNodeChildren(Node node, CompilationContext context) {
        Node n = null;
        Vector children = node.getChildren();

        if(children == null) {
            n = new Node(node.getLabel(), new Vector());
        } else {
            Vector newChildren = new Vector();

            for(Object child : children) {
                child = buildExpression(child).scaffold(context);
                newChildren.add(child);
            }

            n = new Node(node.getLabel(), newChildren);
        }

        n.meta = node.meta;
        return n;
    }

    // TODO: Should I remove this old macro-expansion code now that
    // macro expansion is done during scaffolding?
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
            } catch(NoClassDefFoundError e) {
                // TODO: This sometimes can generator a NoClassDefFoundError that can NOT be caught.
                // The reproduce this errors, compile a program that uses `Node(null, null)`. Keep the
                // CompilationContext imports the same as the commit where this comment was introduced.
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

    public static Object defaultValueForType(Class klass) {
        if(klass.equals(Boolean.TYPE)) {
            return new Boolean(false);
        } else if(klass.equals(Character.TYPE)) {
            return new Character((char)0);
        } else if(klass.equals(Byte.TYPE)) {
            return new Byte((byte)0);
        } else if(klass.equals(Short.TYPE)) {
            return new Short((short)0);
        } else if(klass.equals(Integer.TYPE)) {
            return new Integer(0);
        } else if(klass.equals(Long.TYPE)) {
            return new Long(0);
        } else if(klass.equals(Float.TYPE)) {
            return new Float(0);
        } else if(klass.equals(Double.TYPE)) {
            return new Double(0);
        } else {
            return null;
        }
    }

    /*
    public static void extractDeclarations(Object o, CompilationContext context) {
        // You cannot re-define a special form or use it as a named identifier
        // so I do not need to worry about "function" and the like appearing accidentally.

        // TODO: What about special forms that have "function" inside of them? For example,
        // Suppose I define an array that has an anonymous "structure"  as a type name or something... won't that
        // screw stuff up? Instead of processing an AST, shouldn't this function process an expression
        // instead?

        // TODO: Change this function to process expressions instead of Nodes. --- This may be problematic
        // because Expression has been re-worked so that Expression.build does very little and most of the
        // work is inside "emit" now... This may be easier once I go back to having "one way of doing everything"
        // so "::" is scope and "." is JUST get field. I may have a "java" special form that allows the
        // use of java expression in a more natural way...

        Runtime runtime = new Runtime(context.declarations);
        CompilationContext declarationsContext = new CompilationContext(runtime);

        if(o instanceof Node) {
            Node node = (Node)o;
            IPersistentVector vector = node.getDescendants(new Symbol("package"), new Symbol("import"), new Symbol("alias"), new Symbol("function"), new Symbol("structure"), new Symbol("quote"));

            for(int i = 0; i < vector.length(); i++) {
                Node n = (Node)vector.nth(i);

                if(n.getLabel().equals(new Symbol("function"))) {
                    //FunctionExpression e = FunctionExpression.build(removeBodyFromFunction(n, declarationsContext));
                    //e.emitDeclaration(declarationsContext);
                } else if(n.getLabel().equals(new Symbol("package"))) {
                    //e = new Package(n);
                    //e.emitDeclaration(declarationsContext);
                } else if(n.getLabel().equals(new Symbol("import"))) {
                    //e = new Import(n);
                    //e.emitDeclaration(declarationsContext);
                } else if(n.getLabel().equals(new Symbol("alias"))) {
                    //e = new Alias(n);
                    //e.emitDeclaration(declarationsContext);
                } else {
                    throw new RuntimeException("Unhandled Case!");
                }
            }
        }
    }
    */

    public static Expression buildExpression(Object value) {
        if(value instanceof Node) {
            Node node = (Node)value;
            Object label = node.getLabel();

            // TODO: Create an "ExpressionBuilder" interface that all the Expressions have
            // as a nested class. Then, just create a list of "ExpressionBuilder" as "Special-Forms"
            // and literate over them instead of having this huge if-else statement.

            // TODO: If you do not create an ExpressionBuilder atleast re-organize this switch table...

            if(node.getLabel() == null) {
                return new Block(node);
            } else if(label.equals(new Symbol("do"))) {
                return new Block(node);
            } else if(label.equals(new Symbol("array"))) {
                return new LiteralArrayType(node);
            } else if(label.equals(new Symbol("arraynew"))) {
                return new LiteralArray(node);
            } else if(label.equals(new Symbol("arraylength"))) {
                return new ArrayLength(node);
            } else if(label.equals(new Symbol("package"))) {
                return new Package(node);
            } else if(label.equals(new Symbol("alias"))) {
                return new Alias(node);
            } else if(label.equals(new Symbol("import"))) {
                return new Import(node);
            } else if(label.equals(new Symbol("defineclass"))) {
                return new DefineClass(node);
            } else if(label.equals(new Symbol("function"))) {
                return new FunctionExpression(node);
            } else if(label.equals(new Symbol("declare"))) {
                return new Declare(node);
            } else if(label.equals(new Symbol("quotecontext"))) {
                return new QuoteContext(node);
            } else if(label.equals(new Symbol("throw"))) {
                return new Throw(node);
            } else if(label.equals(new Symbol("try"))) {
                return new Try(node);
            } else if(label.equals(new Symbol("catch"))) {
                return new Catch(node);
            } else if(label.equals(new Symbol("finally"))) {
                return new Finally(node);
            } else if(label.equals(new Symbol("loop"))) {
                return new Loop(node);
            } else if(label.equals(new Symbol("break"))) {
                return new Break(node);
            } else if(label.equals(new Symbol("branch"))) {
                return new Branch(node);
            } else if(label.equals(new Symbol("monitorenter"))) {
                return new MonitorEnter(node);
            } else if(label.equals(new Symbol("monitorexit"))) {
                return new MonitorExit(node);
            } else if(label.equals(new Symbol("return"))) {
                return new Return(node);
            } else if(label.equals(new Symbol("checkcast"))) {
                return new CheckCast(node);
            } else if(label.equals(new Symbol("instanceof"))) {
                return new InstanceOf(node);
            } else if(label.equals(new Symbol("uniquesymbol"))) {
                return new UniqueSymbol(node);
            } else if(label.equals(new Symbol("invokevirtual"))) {
                // TODO: Add macro called "dispatch" to wrap this...
                return new InvokeVirtual(node);
            } else if(label.equals(new Symbol("#"))) {
                return new InvokeVirtual(node);
            } else if(label.equals(new Symbol("."))) {
                return new Access(node);
            } else if(label.equals(new Symbol("="))) {
                return new Assign(node);
            } else if(label.equals(new Symbol(":="))) {
                return new Assign(node);
            } else if(label.equals(new Symbol("|"))) {
                return Pipe.build(node);
            } else if(label.equals(new Symbol("|="))) {
                return PipeAssignment.build(node);
            } else if(LogicalOperation.accepts(node.getLabel())) {
                return LogicalOperation.build(node);
            } else if(MathOperation.accepts(node.getLabel())) {
                return new MathOperation(node);
            } else if(RelationalOperation.accepts(node.getLabel())) {
                return new RelationalOperation(node);
            } else {
                return new Invoke(node);
            }
        } else if(value instanceof Symbol) {
            if(value.equals(new Symbol("return"))) {
                return new Return(new Node(new Symbol("return")));
            } else if(value.equals(new Symbol("break"))) {
                return new Break(new Node(new Symbol("break")));
            } else if(value.equals(new Symbol("continue"))) {
                // TODO
            } else if(value.equals(new Symbol("throw"))) {
                return new Throw(new Node(new Symbol("throw")));
            }

            return new Access(value);
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

    public static void pushInitializationValue(Class klass, GeneratorAdapter generator) {
        if(klass.isPrimitive()) {
            if(klass.equals(Double.TYPE)) {
                generator.push(0.0d);
            } else if(klass.equals(Float.TYPE)) {
                generator.push(0.0f);
            } else if(klass.equals(Long.TYPE)) {
                generator.push(0L);
            } else {
                generator.push(0);
            }
        } else {
            generator.push((String)null);
        }
    }

    public static Class resolveType(String qualifiedName, RuntimeClassLoader... loaders) {
        Class klass = Compiler.primitives.get(qualifiedName);

        for(RuntimeClassLoader loader : loaders) {
            if(klass != null) {
                break;
            }

            try {
                klass = loader.resolveType(qualifiedName);
            } catch(NoClassDefFoundError e) {
                //System.out.println("No Class");
                klass = null;
            }
        }

        return klass;
    }

    public static Class resolveType(Vector<Symbol> path, CompilationContext context) {
        if(path.size() == 1) {
            String alias = context.currentNamespace().aliases.get(path.get(0).toString());
            if(alias != null) {
                Class klass = resolveType(alias, context.runtime.loader);
                if(klass != null) {
                    return klass;
                }
            }
        }

        String name = null;
        for(Symbol symbol : path) {
            if(name == null) {
                name = symbol.toString();
            } else {
                name += "." + symbol.toString();
            }
        }

        String qualifiedName = name;
        Class klass = null;

        if(context.currentNamespace().packageName == null || context.currentNamespace().packageName.equals("")) {
            qualifiedName = name;
        } else {
            qualifiedName = context.currentNamespace().packageName + "." + name;
        }

        klass = resolveType(qualifiedName, context.runtime.loader, context.symbolLoader);
        if(klass != null) {
            return klass;
        }

        for(String p : context.currentNamespace().imports) {
            qualifiedName = p;
            if(qualifiedName.equals("")) {
                qualifiedName = name;
            } else {
                qualifiedName += "." + name;
            }

            klass = resolveType(qualifiedName, context.runtime.loader, context.symbolLoader);

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

    // TODO: All of the methods that call this function should be prepared to catch the Exception
    // so that they they can report a better error message to the user with the line number / context of the
    // error...
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

        Vector<Symbol> buffer = new Vector<Symbol>();

        for(Symbol symbol : path) {
            index++;

            buffer.add(symbol);
            Class klass = resolveType(buffer, context);

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

        return null;
    }

    public static Vector<Symbol> symbolList(Object o) {
        if(o instanceof Symbol) {
            Vector<Symbol> v = new Vector<Symbol>();
            v.add((Symbol)o);
            return v;
        } else if(o instanceof Node) {
            Vector<Symbol> list = Node.symbolListFromNode(Node.flattenTree((Node)o, new Symbol(".")));
            if(list != null) {
                return list;
            }
        }

        return null;
    }

    public static void loadExecutionContext(CompilationContext context) {
        context.currentFrame().generator.visitVarInsn(Opcodes.ALOAD, 0);
    }

    public static void loadExecutionFrame(CompilationContext context) {
        loadExecutionContext(context);
        context.currentFrame().generator.invokeVirtual(Type.getType(ExecutionContext.class), org.objectweb.asm.commons.Method.getMethod("silo.lang.ExecutionFrame getCurrentFrame()"));
    }

    public static void lineNumber(Node node, CompilationContext context) {
        if(node.getMeta() == null) {
            return;
        }

        Object line = PersistentMapHelper.get(node.getMeta(), "line");

        if(line != null) {
            if(line instanceof Integer) {
                context.currentFrame().generator.visitLineNumber(
                    ((Integer)line).intValue(),
                    context.currentFrame().generator.mark()
                );
            }
        }
    }

    public static boolean isValidAssignment(Class target, Class value) {
        if(!target.isPrimitive() && value.equals(Null.class)) {
            // TODO: Should null be a valid assignment to a primitive? So there is a
            // standardized way to make assignments to the default value?
            return true;
        } else {
            return target.isAssignableFrom(value);
        }
    }

    public static enum AssignmentOperation {
        INVALID,
        VALID,
        BOX,
        UNBOX,
    }

    public static AssignmentOperation assignmentValidation(Class target, Class source) {
        // TODO: Var support here?

        if(!target.isPrimitive() && source.equals(Null.class)) {
            return AssignmentOperation.VALID;
        }

        if(target.isAssignableFrom(source)) {
            return AssignmentOperation.VALID;
        }

        if(target.isAssignableFrom(assignmentUnboxType(source))) {
            return AssignmentOperation.UNBOX;
        }

        if(target.isAssignableFrom(assignmentBoxType(source))) {
            return AssignmentOperation.BOX;
        }

        return AssignmentOperation.INVALID;
    }

    public static Class assignmentBox(Class source, CompilationContext context) {
        context.currentFrame().generator.valueOf(Type.getType(source));
        context.currentFrame().operandStack.pop();
        context.currentFrame().operandStack.push(Compiler.assignmentBoxType(source));
        return assignmentBoxType(source);
    }

    public static Class assignmentUnbox(Class source, CompilationContext context) {
        context.currentFrame().generator.unbox(Type.getType(source));
        context.currentFrame().operandStack.pop();
        context.currentFrame().operandStack.push(Compiler.assignmentUnboxType(source));
        return assignmentUnboxType(source);
    }

    public static Class assignmentBoxType(Class klass) {
        // TODO: Var support here?

        if(klass.equals(Boolean.TYPE)) {
            return Boolean.class;
        } else if(klass.equals(Character.TYPE)) {
            return Character.class;
        } else if(klass.equals(Byte.TYPE)) {
            return Byte.class;
        } else if(klass.equals(Short.TYPE)) {
            return Short.class;
        } else if(klass.equals(Integer.TYPE)) {
            return Integer.class;
        } else if(klass.equals(Long.TYPE)) {
            return Long.class;
        } else if(klass.equals(Float.TYPE)) {
            return Float.class;
        } else if(klass.equals(Double.TYPE)) {
            return Double.class;
        } else {
            return klass;
        }
    }

    public static Class assignmentUnboxType(Class klass) {
        // TODO: Var support here?

        if(klass.equals(Boolean.class)) {
            return Boolean.TYPE;
        } else if(klass.equals(Character.class)) {
            return Character.TYPE;
        } else if(klass.equals(Byte.class)) {
            return Byte.TYPE;
        } else if(klass.equals(Short.class)) {
            return Short.TYPE;
        } else if(klass.equals(Integer.class)) {
            return Integer.TYPE;
        } else if(klass.equals(Long.class)) {
            return Long.TYPE;
        } else if(klass.equals(Float.class)) {
            return Float.TYPE;
        } else if(klass.equals(Double.class)) {
            return Double.TYPE;
        } else {
            return klass;
        }
    }

    public static void autobox(Class target, CompilationContext context, boolean shouldEmit) {
        Class operandClass = context.currentFrame().operandStack.peek();
        Compiler.AssignmentOperation op = Compiler.assignmentValidation(target, operandClass);

        if(op == Compiler.AssignmentOperation.BOX) {
            if(shouldEmit) {
                Compiler.assignmentBox(operandClass, context);
            } else {
                context.currentFrame().operandStack.pop();
                context.currentFrame().operandStack.push(Compiler.assignmentBoxType(operandClass));
            }
        } else if(op == Compiler.AssignmentOperation.UNBOX) {
            if(shouldEmit) {
                Compiler.assignmentUnbox(operandClass, context);
            } else {
                context.currentFrame().operandStack.pop();
                context.currentFrame().operandStack.push(Compiler.assignmentBoxType(operandClass));
            }
        }
    }

    public static String fullyQualifiedName(Symbol name, CompilationContext context) {
        String fullyQualifiedName = context.currentNamespace().packageName;
        if(fullyQualifiedName == null || fullyQualifiedName.equals("")) {
            fullyQualifiedName = name.toString();
        } else {
            fullyQualifiedName = fullyQualifiedName + "." + name.toString();
        }

        return fullyQualifiedName;
    }
}



