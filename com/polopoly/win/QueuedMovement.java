package com.polopoly.win;

public abstract class QueuedMovement {
    boolean started = false;
    
    public void start() {
        started = true;
    }
    
    public boolean isStarted() {
        return started;
    }
    abstract boolean isDone();
}
