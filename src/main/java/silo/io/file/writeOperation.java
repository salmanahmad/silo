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

import silo.lang.Function;
import silo.lang.ExecutionContext;

import java.io.IOException;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Function.Definition
public class writeOperation extends Function {

    @Function.Body
    public static String invoke(ExecutionContext context, String file, String content, int position) {
        try {
            AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get(file), StandardOpenOption.WRITE, StandardOpenOption.CREATE);

            // TODO: Becareful about the casting channel.size() to an int
            FileOperationMessage op = new FileOperationMessage();
            op.operationId = UUID.randomUUID().toString();
            op.actor = context.fiber.actor;
            op.buffer = ByteBuffer.wrap(content.getBytes());
            op.channel = channel;

            channel.write(op.buffer, position, op, new FileOperationHandler());
            return op.operationId;
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}