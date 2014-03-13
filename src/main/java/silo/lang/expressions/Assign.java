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
import java.lang.reflect.Modifier;

import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.GeneratorAdapter;

// TODO: Multiple-assignment / destructuring?

public class Assign implements Expression {

    Node node;

    public Assign(Node node) {
        this.node = node;
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
                if(node.getLabel().equals(new Symbol(":="))) {
                    typeClass = valueClass;
                } else {
                    // TODO: Change this to the Var
                    typeClass = Object.class;
                }
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

    public void performStaticPutField(Symbol field, Class klass, Class valueClass, Class typeClass, CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        try {
            // TODO: Validate against typeClass?

            java.lang.reflect.Field staticField = klass.getField(field.toString());
            Class fieldClass = staticField.getType();

            if(!Compiler.isValidAssignment(fieldClass, valueClass)) {
                throw new RuntimeException("Invalid assignment to static field of type " + fieldClass + " from type of " + valueClass);
            }

            generator.putStatic(Type.getType(klass), staticField.getName(), Type.getType(staticField.getType()));

            frame.operandStack.pop();

            return;
        } catch(NoSuchFieldException e) {
            throw new RuntimeException("No such field was found: " + field.toString());
        }
    }

    public void performSetField(Symbol field, Class valueClass, Class typeClass, CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        try {
            // TODO: Validate against typeClass?

            Class klass = frame.operandStack.peek();

            java.lang.reflect.Field staticField = klass.getField(field.toString());
            Class fieldClass = staticField.getType();

            if(Modifier.isStatic(staticField.getModifiers())) {
                throw new RuntimeException("Invalid assignment to a static field where a instance was provided.");
            }

            if(!Compiler.isValidAssignment(fieldClass, valueClass)) {
                throw new RuntimeException("Invalid assignment to field of type " + fieldClass + " from type of " + valueClass);
            }

            generator.putField(Type.getType(klass), staticField.getName(), Type.getType(staticField.getType()));

            frame.operandStack.pop();
            frame.operandStack.pop();

            return;
        } catch(NoSuchFieldException e) {
            throw new RuntimeException("No such field was found: " + field.toString());
        }
    }

    public void performArrayAssignment(Vector index, Class valueClass, Class typeClass, CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        Class klass = frame.operandStack.peek();

        if(index.size() != 1) {
            throw new RuntimeException("Cannot access array with more than one number...");
        }

        Compiler.buildExpression(index.get(0)).emit(context);

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

    public Symbol assignmentLocalVariableSymbol() {
        if(node.getFirstChild() instanceof Symbol) {
            return (Symbol)node.getFirstChild();
        } else if(node.getFirstChild() instanceof Node) {
            Node n = (Node)node.getFirstChild();
            if(n.getLabel().equals(new Symbol(":"))) {
                if(n.getFirstChild() instanceof Symbol) {
                    return (Symbol)n.getFirstChild();
                }
            }
        }

        return null;
    }

    public Class assignmentTypeClass(CompilationContext context) {
        if(node.getFirstChild() instanceof Node) {
            Node n = (Node)node.getFirstChild();
            if(n.getLabel().equals(new Symbol(":"))) {
                Class klass = Compiler.resolveType(n.getSecondChild(), context);
                if(klass == null) {
                    throw new RuntimeException("Could not resolve type: " + n.getSecondChild());
                } else {
                    return klass;
                }
            }
        }

        return null;
    }

    public Class type(CompilationContext context) {
        Symbol local = assignmentLocalVariableSymbol();
        if(local != null) {
            if(!context.currentFrame().locals.containsKey(local)) {
                Class klass = assignmentTypeClass(context);
                if(klass == null) {
                    // TODO: Change this to Var
                    klass = Object.class;
                }

                context.currentFrame().newLocal(local, klass);
            }
        }

        return Compiler.buildExpression(node.getSecondChild()).type(context);
    }

    public Object scaffold(CompilationContext context) {
        return Compiler.scaffoldNodeChildren(node, context);
    }

    public void emit(CompilationContext context) {
        GeneratorAdapter generator = context.currentFrame().generator;
        RuntimeClassLoader loader = context.runtime.loader;
        CompilationFrame frame = context.currentFrame();

        if(node.getChildren().size() != 2) {
            throw new RuntimeException("Assignment requires at least 2 arguments.");
        }

        if(node.getSecondChild() != null) {
            Compiler.buildExpression(node.getSecondChild()).emit(context);
        } else {
            generator.push((String)null);
            frame.operandStack.push(Null.class);
        }

        Class valueClass = frame.operandStack.peek();
        Compiler.dup(valueClass, generator);
        frame.operandStack.push(valueClass);

        if(node.getFirstChild() instanceof Symbol) {
            performLocalVariableAssignment((Symbol)node.getFirstChild(), valueClass, null, context);
            return;
        } else if(node.getFirstChild() instanceof Node) {
            Node n = (Node)node.getFirstChild();
            Class typeClass = null;

            if(n.getLabel().equals(new Symbol(":"))) {
                typeClass = Compiler.resolveType(n.getSecondChild(), context);
                if(typeClass == null) {
                    throw new RuntimeException("Could not resolve type: " + n.getSecondChild());
                }

                if(n.getFirstChild() instanceof Symbol) {
                    performLocalVariableAssignment((Symbol)n.getFirstChild(), valueClass, typeClass, context);
                    return;
                } else if(n.getFirstChild() instanceof Node){
                    n = (Node)n.getFirstChild();
                } else {
                    throw new RuntimeException("Invalid Assignment Form.");
                }
            }

            Object label = n.getLabel();
            if(label.equals(new Symbol("."))) {
                Object structure = n.getFirstChild();
                Object field = n.getSecondChild();

                if(!(field instanceof Symbol)) {
                    throw new RuntimeException("Invalid Assignment Form.");
                }

                Class klass = Compiler.resolveType(structure, context);
                if(klass != null) {
                    performStaticPutField((Symbol)field, klass, valueClass, typeClass, context);
                    return;
                }

                if(structure instanceof Symbol || (structure instanceof Node && ((Node)structure).getLabel().equals("."))) {
                    // TODO: Note: with the "willMutate" field in Access, if the "left" most leaf in
                    // the tree is NOT a Symbol, but some other non-"dot"-node, then I should throw
                    // an exception...
                    // TODO: The above todo is not actually correct - what if I have a java interop expression
                    // like user.getAccount().name = "foo"
                    (new Access(structure, true)).emit(context);
                } else {
                    Compiler.buildExpression(structure).emit(context);
                }

                generator.swap(Type.getType(valueClass), Type.getType(frame.operandStack.peek()));

                // TODO: Support mutated field type if a "Structure" or "Var" is on the operandStack
                if(true) {
                    // Normal Field Put
                    performSetField((Symbol)field, valueClass, typeClass, context);
                    return;
                } else {
                    // Mutated Field Put
                    throw new RuntimeException("Unimplemented");
                }

            } else {
                // DynamicSet or Array Set
                Compiler.buildExpression(label).emit(context);
                generator.swap(Type.getType(valueClass), Type.getType(frame.operandStack.peek()));

                if(frame.operandStack.peek().isArray()) {
                    performArrayAssignment(n.getChildren(), valueClass, typeClass, context);
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

                return;
            }
        } else {
            throw new RuntimeException("Invalid assignment form. The first argument must be a symbol or a node.");
        }
    }
}
