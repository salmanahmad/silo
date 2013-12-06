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

    public Runtime runtime;

    // TODO - should this be -1?
    int currentFrame = -1;
    ArrayList<ExecutionFrame> frames = new ArrayList<ExecutionFrame>();

    public boolean yielding = false;
    public int programCounter = 0;

    public ExecutionContext() {

    }

    public ExecutionContext(Function f) {
        this.beginCall();
        this.setCurrentFrame(new ExecutionFrame(f));
        this.yielding = true;
        this.endCall();
    }

    public void beginCall() {
        // Reset yielding so that next time we beginCall it will work correctly
        yielding = false;
        currentFrame++;
        if(currentFrame >= frames.size()) {
            // TODO - Optimize this.
            frames.ensureCapacity(currentFrame + 1);
            frames.add(null);

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
                frames.set(currentFrame + 1, null);
                programCounter = frame.programCounter;
                return RESUMING;
            }
        }
    }

    public void setCurrentFrame(ExecutionFrame frame) {
        frames.set(currentFrame, frame);
    }

    public ExecutionFrame getCurrentFrame() {
        if(currentFrame == -1) {
            return null;
        }

        if(currentFrame >= frames.size()) {
            return null;
        } else {
            return frames.get(currentFrame);
        }
    }
}
