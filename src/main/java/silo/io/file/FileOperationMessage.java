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

package silo.io.file;

import silo.lang.Actor;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;

class FileOperationMessage {
    public AsynchronousFileChannel channel;

    public String operationId;
    public Actor actor;

    public ByteBuffer buffer;
    public int count;

    public Throwable exception;
}