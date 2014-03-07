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

import java.nio.channels.CompletionHandler;

public class FileOperationHandler implements CompletionHandler<Integer, FileOperationMessage> {
    public void completed(Integer result, FileOperationMessage attachment) {
        attachment.count = result.intValue();
        attachment.actor.inboxPut(attachment);
    }

    public void failed(Throwable exception, FileOperationMessage attachment) {
        attachment.exception = exception;
        attachment.actor.inboxPut(attachment);
    }
}