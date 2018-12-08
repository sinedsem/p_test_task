package com.pixonic.test.util;

import java.util.concurrent.TimeUnit;

public class MillisAndNanos {
    private final long millis;
    private final int nanos;

    private MillisAndNanos(long millis, int nanos) {
        this.millis = millis;
        this.nanos = nanos;
    }

    public long getMillis() {
        return millis;
    }

    public int getNanos() {
        return nanos;
    }

    public static MillisAndNanos ofNanos(long nanos) {
        return new MillisAndNanos(TimeUnit.NANOSECONDS.toMillis(nanos), (int) (nanos % 1_000_000));
    }
}
