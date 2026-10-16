package org.ranobe.ranobe.export;

/**
 * Lets the queue stop a running job. The runner calls {@link #check()} between steps and sleeps
 * through {@link #sleep(long)}, both throw {@link StoppedException} as soon as a stop is requested.
 */
class JobControl {
    private volatile Stop stop;
    private volatile String reason;

    void stop(Stop stop, String reason) {
        synchronized (this) {
            // a cancel wins over a pause that is still winding down
            if (this.stop == Stop.CANCEL) return;
            this.stop = stop;
            this.reason = reason;
            notifyAll();
        }
    }

    Stop stopped() {
        return stop;
    }

    String reason() {
        return reason;
    }

    void check() {
        if (stop != null) throw new StoppedException();
    }

    synchronized void sleep(long millis) {
        long until = System.currentTimeMillis() + millis;
        long left = millis;
        while (stop == null && left > 0) {
            try {
                wait(left);
            } catch (InterruptedException e) {
                // the thread pool is shutting down, not a stop the user asked for
                Thread.currentThread().interrupt();
                throw new StoppedException();
            }
            left = until - System.currentTimeMillis();
        }
        check();
    }

    enum Stop {PAUSE, CANCEL}

    static class StoppedException extends RuntimeException {
        StoppedException() {
            super("Stopped", null, false, false);
        }
    }
}
