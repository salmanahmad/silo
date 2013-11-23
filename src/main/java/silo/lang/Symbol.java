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

public class Symbol {

    public String name;

    public Symbol(String name) {
        this.name = name;
    }

    public boolean equals(Object o) {
        if(Symbol.class.equals(o.getClass())) {
            Symbol symbol = (Symbol)o;
            return Helper.equals(this.name, symbol.name);
        } else {
            return super.equals(o);
        }
    }

    public String toString() {
        return this.name;
    }
}