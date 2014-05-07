/*
 *
 *  Copyright 2014 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */

package silo.lang;

import java.lang.reflect.Field;

public class Structure implements Cloneable {
    private int referenceCount;

    public void reset() {
        referenceCount = 0;
    }

    public void retain() {
        referenceCount++;
    }

    public void release() {
        referenceCount--;

        if(referenceCount < 0) {
            referenceCount = 0;
        }
    }

    public Structure copy() {
        try {
            Structure s = (Structure)super.clone();
            s.reset();
            return s;
        } catch(Exception e) {
            throw new RuntimeException("Could not copy structure");
        }
    }

    public Structure copyForMutation() {
        if(referenceCount <= 1) {
            // TODO: Once I implement reference counting I could uncomment this following line...
            //return this;
            return this.copy();
        } else {
            return this.copy();
        }
    }

    public String toString() {
        Node description = new Node(new Symbol(this.getClass().getName()));

        try {
            Field[] fields = this.getClass().getFields();
            for(Field field : fields) {
                description.addChild(field.get(this));
            }
        } catch(IllegalAccessException e) {
            // Ignore
        }


        return description.toString();
    }
}