package com.frameworkserver;

/**
 *
 * @author sathyam
 */
class SynchronizedMutex {

    private Thread curOwner = null;

    // This function is used to acquire the lock
    public synchronized void acquire() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        while (curOwner != null) {
            wait();
        }
        curOwner = Thread.currentThread();
    }
    // This function is used to release the lock

    public synchronized void release() {
        if (curOwner == Thread.currentThread()) {
            curOwner = null;
            notify();
        } else {
            throw new IllegalStateException("not owner of mutex");
        }
    }
}
