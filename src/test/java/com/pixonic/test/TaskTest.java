package com.pixonic.test;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.PriorityQueue;
import java.util.Queue;

import static org.junit.Assert.assertEquals;

public class TaskTest {

    @Test
    public void testCompareByIndex() {
        LocalDateTime now = LocalDateTime.now();

        Task<Object> t1 = new Task<>(now, null, 1);
        Task<Object> t2 = new Task<>(now, null, 2);

        assertEquals(-1, t1.compareTo(t2));
        assertEquals(1, t2.compareTo(t1));
    }

    @Test
    public void testCompareByTime() {
        LocalDateTime now = LocalDateTime.now();

        Task<Object> t1 = new Task<>(now.plusSeconds(54), null, 1);
        Task<Object> t2 = new Task<>(now, null, 2);

        assertEquals(1, t1.compareTo(t2));
        assertEquals(-1, t2.compareTo(t1));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testInPriorityQueue() {
        Queue<Task> queue = new PriorityQueue<>();

        LocalDateTime now = LocalDateTime.now();
        queue.offer(new Task<>(now.plusSeconds(54), null, 2));
        queue.offer(new Task<>(now.plusSeconds(54), null, 1));
        queue.offer(new Task<>(now.plusSeconds(3), null, 3));

        assertEquals(3, queue.poll().getIndex());
        assertEquals(1, queue.poll().getIndex());
        assertEquals(2, queue.poll().getIndex());
    }



}
