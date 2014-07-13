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

import silo.core.fiber.Fiber;

public class ExecutionContext {

    int currentFrame = -1;
    ExecutionFrame[] frames = new ExecutionFrame[16];

    public boolean yielding = false;
    public int programCounter = -1;

    public Fiber fiber;

    public void beginCall() {
        // We are beginning a new call so obviously we are not yielding anymore...
        yielding = false;

        currentFrame++;
        if(currentFrame >= frames.length) {
            ensureSize(frames.length * 2);
            frames[currentFrame] = null;

            programCounter = -1;
        } else {
            ExecutionFrame frame = getCurrentFrame();
            if(frame == null) {
                programCounter = -1;
            } else {
                programCounter = frame.programCounter;
            }
        }
    }

    public boolean endCall() {
        currentFrame--;

        if(!yielding) {
            ExecutionFrame frame = getCurrentFrame();

            if(frame != null) {
                frames[currentFrame + 1] = null;
                programCounter = frame.programCounter;
            }
        }

        return yielding;
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
