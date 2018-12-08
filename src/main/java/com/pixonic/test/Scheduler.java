package com.pixonic.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal"})
public class Scheduler {

    private static final Logger log = LoggerFactory.getLogger(Scheduler.class);

    private final AtomicLong counter = new AtomicLong();

    private final BlockingQueue<Task> waiting = new PriorityBlockingQueue<>();
    private final BlockingQueue<Task> toExecute = new PriorityBlockingQueue<>();

    private final Planner planner = new Planner();
    private final Executor executor = new Executor();

    private volatile boolean stop = false;

    private final List<Long> misses = new ArrayList<>();


    public Scheduler() {
        planner.start();
        executor.start();
    }

    public <V> Future<V> submit(LocalDateTime time, Callable<V> callable) {
        Task<V> task = new Task<>(time, callable, counter.getAndIncrement());
        waiting.add(task);
        planner.interrupt();
        return task;
    }

    @SuppressWarnings("unused")
    public void stop() {
        this.stop = true;
    }

    private class Planner extends Thread {

        @Override
        public void run() {
            while (!stop) {
                try {
                    Task task = waiting.take();
                    log.debug("Waiting: got task {}", task);
                    if (task.getTime().isAfter(LocalDateTime.now().plus(2, ChronoUnit.MILLIS))) {
                        log.debug("Waiting: task {} not ready to be executed", task);
                        waiting.put(task);
                        long nanosToSleep = ChronoUnit.NANOS.between(LocalDateTime.now(), task.getTime());
                        long millisToSleep = TimeUnit.NANOSECONDS.toMillis(nanosToSleep);
                        log.debug("Waiting: will sleep {} ms", millisToSleep);
                        Thread.sleep(millisToSleep, (int) (nanosToSleep % 1_000_000));
                    } else {
                        log.debug("Waiting: sending task {} for execution", task);
                        toExecute.put(task);
                    }
                } catch (InterruptedException ignored) {
                    // that's fine
                }
            }
        }
    }

    private class Executor extends Thread {

        @Override
        public void run() {
            while (!stop) {
                Task task;
                try {
                    task = toExecute.take();
                    log.debug("Executing: got task {}", task);
                } catch (InterruptedException e) {
                    continue;
                }

                long miss = ChronoUnit.MILLIS.between(task.getTime(), LocalDateTime.now());
                misses.add(miss);
                task.execute();
                log.debug("Executing: task completed {}", task);
            }
        }
    }

    public long getMaxMiss() {
        return misses.stream().mapToLong(Long::longValue).map(Math::abs).max().orElse(0);
    }

    public double getAverageMiss() {
        return misses.stream().mapToLong(Long::longValue).map(Math::abs).average().orElse(0.0);
    }
}