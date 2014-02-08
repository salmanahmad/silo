/*
 *
 *  Copyright 2012 by Salman Ahmad (salman@salmanahmad.com).
 *  All rights reserved.
 *
 *  Permission is granted for use, copying, modification, distribution,
 *  and distribution of modified versions of this work as long as the
 *  above copyright notice is included.
 *
 */

package silo.lang;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class ExecutionContext {

    public final int RUNNING = 1;
    public final int RESUMING = 2;
    public final int CAPTURING = 3;
    public final int YIELDING = 4;

    int currentFrame = -1;
    ExecutionFrame[] frames = new ExecutionFrame[16];

    public boolean yielding = false;
    public int programCounter = 0;

    public void beginCall() {
        // We are beginning a new call so obviously we are not yielding anymore...
        yielding = false;

        currentFrame++;
        if(currentFrame >= frames.length) {
            ensureSize(frames.length * 2);
            frames[currentFrame] = null;

            programCounter = 0;
        } else {
            ExecutionFrame frame = getCurrentFrame();
            if(frame == null) {
                programCounter = 0;
            } else {
                programCounter = frame.programCounter;
            }
        }
    }

    public int endCall() {
        currentFrame--;
        ExecutionFrame frame = getCurrentFrame();

        if(yielding) {
            if(frame == null) {
                return CAPTURING;
            } else {
                return YIELDING;
            }
        } else {
            if(frame == null) {
                programCounter = 0;
                return RUNNING;
            } else {
                frames[currentFrame + 1] = null;

                // TODO: Is there a reason why I need the programCounter
                //programCounter = frame.programCounter;
                programCounter = 0;
                return RESUMING;
            }
        }
    }

    public void setCurrentFrame(ExecutionFrame frame) {
        frames[currentFrame] = frame;
    }

    public ExecutionFrame getCurrentFrame() {
        if(currentFrame == -1) {
            return null;
        } else {
            return frames[currentFrame];
        }
    }

    private void ensureSize(int size) {
        ExecutionFrame[] frames = new ExecutionFrame[size];
        System.arraycopy(this.frames, 0, frames, 0, this.frames.length);
        this.frames = frames;
    }
}
