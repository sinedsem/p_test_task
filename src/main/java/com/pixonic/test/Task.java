package com.pixonic.test;

import com.pixonic.test.util.MillisAndNanos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

@SuppressWarnings("WeakerAccess")
public class Task<V> implements Comparable<Task>, Future<V> {

    private final LocalDateTime time;
    private final Callable<V> callable;
    private final long index;

    private Throwable exception;
    private boolean completed = false;
    private V result;

    private final Object monitor = new Object();

    public Task(LocalDateTime time, Callable<V> callable, long index) {
        this.time = time;
        this.callable = callable;
        this.index = index;
    }

    public LocalDateTime getTime() {
        return time;
    }

    long getIndex() {
        return index;
    }

    public void execute() {
        try {
            result = callable.call();
        } catch (Throwable e) {
            this.exception = e;
        }
        completed = true;
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return completed;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        synchronized (monitor) {
            while (!completed) {
                monitor.wait();
            }
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        final long deadline = System.nanoTime() + unit.toNanos(timeout);
        synchronized (monitor) {
            while (!completed) {
                long toWaitNanos = deadline - System.nanoTime();
                if (toWaitNanos < 0) {
                    throw new TimeoutException();
                }
                MillisAndNanos toWait = MillisAndNanos.ofNanos(toWaitNanos);
                monitor.wait(toWait.getMillis(), toWait.getNanos());
            }
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }
        return result;
    }

    @Override
    public int compareTo(Task o) {
        int byTime = time.compareTo(o.time);
        if (byTime != 0) {
            return byTime;
        }
        return Long.compare(index, o.index);
    }

    @Override
    public String toString() {
        return String.format("%d (%s)", index, time.format(DateTimeFormatter.ofPattern("hh:mm:ss")));
    }


}
