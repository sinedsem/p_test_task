package com.pixonic.test;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SchedulerTest {


    @Test
    public void testBasic() throws Exception {
        AtomicInteger counter = new AtomicInteger();

        Scheduler scheduler = new Scheduler();

        Future<Integer> future = scheduler.submit(LocalDateTime.now().plusSeconds(3), () -> counter.incrementAndGet() + 3);

        assertEquals(0, counter.get());
        Integer resultFromFuture = future.get();
        assertEquals(4, resultFromFuture.intValue());
        assertEquals(1, counter.get());
    }

    @Test
    public void testEarlierTaskCameSecond() throws Exception {
        AtomicInteger counter = new AtomicInteger();

        Scheduler scheduler = new Scheduler();

        LocalDateTime now = LocalDateTime.now();
        scheduler.submit(now.plusSeconds(1), counter::incrementAndGet);
        Random random = new Random();
        Thread.sleep(random.nextBoolean() ? random.nextInt(100) : 0, random.nextBoolean() ? random.nextInt(100) : 0);
        scheduler.submit(now, () -> counter.addAndGet(2));

        Thread.sleep(100);
        assertEquals(2, counter.get());
        Thread.sleep(1000);
        assertEquals(3, counter.get());

        assertTrue(scheduler.getAverageMiss() < 100);
        assertTrue(scheduler.getMaxMiss() < 1000);
    }

    @Test
    public void testConcurrency() throws Exception {
        AtomicInteger counter = new AtomicInteger();

        Scheduler scheduler = new Scheduler();

        Random random = new Random();

        Queue<Future> futures = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    Future<Object> future = scheduler.submit(LocalDateTime.now().plus(random.nextInt(10000), ChronoUnit.MILLIS), () -> {
                        counter.incrementAndGet();
                        return null;
                    });
                    futures.offer(future);
                }
            }).start();
        }

        Thread.sleep(1000);

        while (!futures.isEmpty()) {
            Future future = futures.poll();
            future.get();
        }

        assertEquals(1000, counter.get());

        assertTrue(scheduler.getAverageMiss() < 100);
        assertTrue(scheduler.getMaxMiss() < 1000);
    }

}