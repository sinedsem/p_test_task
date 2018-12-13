package com.pixonic.test;

import com.pixonic.test.util.MillisAndNanos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal"})
public class Scheduler {

    private static final Logger log = LoggerFactory.getLogger(Scheduler.class);
    private static final TemporalAmount ALLOWED_TIME_TO_START_EARLIER = Duration.ofMillis(2);

    private final AtomicLong counter = new AtomicLong();

    private final BlockingQueue<Task> waiting = new PriorityBlockingQueue<>();

    private final Planner queue = new Planner();

    private volatile boolean stop = false;

    private final List<Long> misses = new ArrayList<>();


    public Scheduler() {
        queue.start();
    }

    public <V> Future<V> submit(LocalDateTime time, Callable<V> callable) {
        Task<V> task = new Task<>(time, callable, counter.getAndIncrement());
        waiting.add(task);
        queue.interrupt();
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
                    if (LocalDateTime.now().plus(ALLOWED_TIME_TO_START_EARLIER).isBefore(task.getTime())) {
                        log.debug("Waiting: task {} not ready to be executed", task);
                        waiting.put(task);
                        MillisAndNanos toSleep = MillisAndNanos.ofNanos(ChronoUnit.NANOS.between(LocalDateTime.now(), task.getTime()));
                        log.debug("Waiting: will sleep {} ms", toSleep.getMillis());
                        Thread.sleep(toSleep.getMillis(), toSleep.getNanos());
                    } else {
                        log.debug("Executing task {}", task);
                        long miss = ChronoUnit.MILLIS.between(task.getTime(), LocalDateTime.now());
                        misses.add(miss);
                        task.execute();
                        log.debug("Task completed {}", task);
                    }
                } catch (InterruptedException ignored) {
                    // that's fine
                }
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