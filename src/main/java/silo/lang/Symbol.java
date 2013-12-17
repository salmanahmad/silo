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

public class Symbol {

    public final String name;

    public Symbol(String name) {
        this.name = name;
    }

    public boolean equals(Object o) {
        if(o instanceof Symbol) {
            Symbol symbol = (Symbol)o;
            return Helper.equals(this.name, symbol.name);
        } else {
            return super.equals(o);
        }
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String toString() {
        return this.name;
    }
}