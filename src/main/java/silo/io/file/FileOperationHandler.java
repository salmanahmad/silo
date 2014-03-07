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

import java.io.IOException;
import java.nio.channels.CompletionHandler;

public class FileOperationHandler implements CompletionHandler<Integer, FileOperationMessage> {
    public void completed(Integer result, FileOperationMessage attachment) {
        try {
            attachment.channel.close();
            attachment.count = result.intValue();
            attachment.actor.inboxPut(attachment);
        } catch(IOException e) {
            attachment.exception = e;
            attachment.actor.inboxPut(attachment);
        }
    }

    public void failed(Throwable exception, FileOperationMessage attachment) {
        try {
            attachment.exception = exception;
            attachment.actor.inboxPut(attachment);
            attachment.channel.close();
        } catch(IOException e) {
            attachment.exception = e;
            attachment.actor.inboxPut(attachment);
        }
    }
}