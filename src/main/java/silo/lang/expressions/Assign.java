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

package silo.lang.expressions;

import silo.lang.*;
import silo.lang.compiler.Compiler;

import java.util.Vector;

import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.GeneratorAdapter;

public class Assign implements Expression {

    public final Node path;
    public final Expression head;
    public final Symbol field;
    public final Vector<Expression> args;
    public final Object type;
    public final Expression value;

    private final Node originalNode;

    public static Assign build(Node node) {
        if(node.getChildren().size() != 2) {
            throw new RuntimeException("Assignment requires at least 2 arguments.");
        }

        Node path = null;
        Expression head = null;
        Symbol field = null;
        Vector<Expression> args = new Vector<Expression>(); ;
        Object type = null;
        Expression value = null;

        value = Compiler.buildExpression(node.getSecondChild());

        Object o = node.getFirstChild();
        if(o instanceof Symbol) {
            field = (Symbol)o;
        } else if(o instanceof Node) {
            if(((Node)o).getLabel().equals(new Symbol(":"))) {
                type = ((Node)o).getSecondChild();
                o = ((Node)o).getFirstChild();
            }

            if(o instanceof Symbol) {
                head = null;
                field = (Symbol)o;
            } else if(o instanceof Node) {
                Node n = (Node)o;
                Object label = n.getLabel();

                if(label.equals(new Symbol("."))) {
                    if(null == Node.symbolListFromNode(Node.flattenTree(n, new Symbol(".")))) {
                        head = Compiler.buildExpression(n.getFirstChild());

                        if(n.getSecondChild() instanceof Symbol) {
                            field = (Symbol)n.getSecondChild();
                        } else {
                            throw new RuntimeException("Invalid assignment form.");
                        }
                    } else {
                        path = node;
                    }
                } else {
                    head = Compiler.buildExpression(label);

                    for(Object child : n.getChildren()) {
                        args.add(Compiler.buildExpression(child));
                    }
                }
            } else {
                throw new RuntimeException("Invalid assignment form.");
            }
        } else {
            throw new RuntimeException("Invalid assignment form. The first argument must be a variable identifier or a typed expression");
        }

        return new Assign(node, path, head, field, args, type, value);
    }

    public Assign(Node node, Node path, Expression head, Symbol field, Vector<Expression> args, Object type, Expression value) {
        this.originalNode = node;
        this.path = path;
        this.head = head;
        this.field = field;
        this.args = args;
        this.type = type;
        this.value = value;
    }

    public void performStaticPutField(Class klass, Class valueClass, Class typeClass, CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        Symbol f = (Symbol)path.getSecondChild();

        try {
            // TODO: Validate against typeClass?

            if(value != null) {
                value.emit(context);
            } else {
                generator.push((String)null);
                frame.operandStack.push(Null.class);
            }

            java.lang.reflect.Field staticField = klass.getField(f.toString());
            Class fieldClass = staticField.getType();

            if(!Compiler.isValidAssignment(fieldClass, valueClass)) {
                throw new RuntimeException("Invalid assignment to static field of type " + fieldClass + " from type of " + valueClass);
            }

            generator.putStatic(Type.getType(klass), staticField.getName(), Type.getType(staticField.getType()));

            frame.operandStack.pop();

            return;
        } catch(NoSuchFieldException e) {
            throw new RuntimeException("No such field was found: " + f.toString());
        }
    }

    public void performLocalVariableAssignment(Symbol field, Class valueClass, Class typeClass, CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        int local = -1;
        Symbol identifier = field;

        // Check if the variable is already defined
        if(!frame.locals.containsKey(identifier)) {
            // If it is not then define it with the type

            if(typeClass == null) {
                // TODO: Change this to the Var
                typeClass = Object.class;
            }

            local = frame.newLocal(identifier, typeClass);
        } else {
            // If it is defined, make sure the types match up.

            local = frame.locals.get(identifier).intValue();

            if(typeClass != null) {
                Class klass = frame.localTypes.get(identifier);
                if(!klass.equals(typeClass)) {
                    throw new RuntimeException("Attempting to re-define variable " + identifier.toString() + " with a different type.");
                }
            }

            if(typeClass == null) {
                typeClass = frame.localTypes.get(identifier);
            }
        }

        if(!Compiler.isValidAssignment(typeClass, valueClass)) {
            // TODO: Abstract this out so that it handles things likes autoboxing, and conversion, etc.
            throw new RuntimeException("Invalid assignment from type " + valueClass + " to " + typeClass);
        }

        // Perform assignment keeping one of the values on the stack
        generator.visitVarInsn(Type.getType(typeClass).getOpcode(Opcodes.ISTORE), local);

        frame.operandStack.pop();

        return;
    }

    public void performSetField(Symbol field, Class valueClass, Class typeClass, CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        Symbol f = field;

        try {
            // TODO: Validate against typeClass?

            Class klass = frame.operandStack.peek();

            java.lang.reflect.Field staticField = klass.getField(f.toString());

            Class fieldClass = staticField.getType();

            if(!Compiler.isValidAssignment(fieldClass, valueClass)) {
                throw new RuntimeException("Invalid assignment to field of type " + fieldClass + " from type of " + valueClass);
            }

            generator.putField(Type.getType(klass), staticField.getName(), Type.getType(staticField.getType()));

            frame.operandStack.pop();
            frame.operandStack.pop();

            return;
        } catch(NoSuchFieldException e) {
            throw new RuntimeException("No such field was found: " + f.toString());
        }
    }

    public void performArrayAssignment(Class valueClass, Class typeClass, CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        Class klass = frame.operandStack.peek();

        if(args.size() != 1) {
            throw new RuntimeException("Cannot access array with more than one number...");
        }

        args.get(0).emit(context);

        if(!frame.operandStack.peek().equals(Integer.TYPE)) {
            throw new RuntimeException("Array access must be provided an integer index...");
        }

        generator.swap(Type.getType(valueClass), Type.getType(Integer.TYPE));

        if(!Compiler.isValidAssignment(klass.getComponentType(), valueClass)) {
            throw new RuntimeException("Attempting to assign value into array of invalid types");
        }

        generator.arrayStore(Type.getType(klass.getComponentType()));

        frame.operandStack.pop();
        frame.operandStack.pop();
        frame.operandStack.pop();

        return;
    }

    public Class type(CompilationContext context) {
        return this.value.type(context);
    }

    public void emitDeclaration(CompilationContext context) {
        for(Object child : originalNode.getChildren()) {
            Compiler.buildExpression(child).emitDeclaration(context);
        }
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        // TODO: Augment compilationcontext to indicate that an assignment is taking p lace for access and invoke use the proper immutable access principles...
        // This is trickier than you may think. What about cases liked `i = getUser(currentUser.id).name = "Bob"`

        Class typeClass = null;
        if(type != null) {
            typeClass = Compiler.resolveType(type, context);

            if(typeClass == null) {
                throw new RuntimeException("Could not find symbol: " + type.toString());
            }
        }

        if(value != null) {
            value.emit(context);
        } else {
            generator.push((String)null);
            frame.operandStack.push(Null.class);
        }

        Class valueClass = frame.operandStack.peek();
        Compiler.dup(valueClass, generator);
        frame.operandStack.push(valueClass);

        Expression h = head;
        Symbol f = field;

        if(path != null) {
            // Attempt static lookup
            Class klass = Compiler.resolveType(path.getFirstChild(), context);

            if(klass == null) {
                // Static put field
                performStaticPutField(klass, valueClass, typeClass, context);
                return;
            } else {
                h = Compiler.buildExpression(path.getFirstChild());
                f = (Symbol)path.getSecondChild();
            }
        }

        if(h == null) {
            if(f != null) {
                // Local variable
                performLocalVariableAssignment(f, valueClass, typeClass, context);
                return;
            } else if(args != null) {
                // TODO: Multiple-assignment / destructuring?
                throw new RuntimeException("Multiple assignment and destructuring has not been implemented yet...");
            }
        } else {
            h.emit(context);

            generator.swap(Type.getType(valueClass), Type.getType(frame.operandStack.peek()));

            if(f != null) {
                // Set field
                performSetField(f, valueClass, typeClass, context);
                return;
            } else if(args != null) {
                // Array and dynamic assignment

                if(frame.operandStack.peek().isArray()) {
                    performArrayAssignment(valueClass, typeClass, context);
                    return;
                } else {
                    // TODO: Handle Vars as well as the AssignableTrait
                    // In fact, is the AssignableTrait even possible? AccessibleTrait perhaps but does the assignable trait
                    // return the assigned value or the value was it was written into? Right now, it is the AssignedValue, but
                    // that makes this almost useless because you cannot get the mutated new value...

                    // TODO: Summary of the assignable trait with vars:
                    // v("hello")        // This will call Var#invoke
                    // v("hello") = d    // This will call Var#invokeAssignment
                    // v.hello           // This will call Var#getField
                    // v.hello = "a"     // This will call Var#setField

                    // TODO: Remember to duplicate this.value and pop the return value from the AccessibleTrait to ensure that the
                    // return type is ALWAYS this.value.
                    throw new RuntimeException("Vars and AssignableTrait are not yet supported...");
                }
            }
        }
    }
}
