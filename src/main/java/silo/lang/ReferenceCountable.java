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

public interface ReferenceCountable {
    public void reset();
    public void retain();
    public void release();
    public ReferenceCountable copy();
    public ReferenceCountable copyForMutation();
}