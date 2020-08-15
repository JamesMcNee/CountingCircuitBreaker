import java.time.Duration;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import static java.util.Objects.isNull;

/**
 * An implementation of a count based windowed circuit breaker.
 *
 * Example usage:
 * This class could be used to prevent warn/error logs from overloading logging infrastructure in the event of an incident.
 */
public class CountingCircuitBreaker {

    private final int threshold;
    private final Duration thresholdWindow;
    private final Consumer<Void> callbackBeforeThresholdBreached;
    private final Consumer<Integer> callbackAfterThresholdBreached;

    private int count;
    private boolean thresholdBreached;
    private Timer timer;

    /**
     * @param threshold the maximum number of occurrences before the circuit breaker is tripped
     * @param thresholdWindow the period of time for which the circuit should be held open once breached
     * @param callbackBeforeThresholdBreached the function that will be called until the circuit has been opened
     * @param callbackAfterThresholdBreached the function that will be called after the circuit has been opened
     */
    public CountingCircuitBreaker(int threshold, Duration thresholdWindow, Consumer<Void> callbackBeforeThresholdBreached,
                                  Consumer<Integer> callbackAfterThresholdBreached) {
        Objects.requireNonNull(callbackBeforeThresholdBreached, "Param callbackBeforeThresholdBreached must be provided");
        Objects.requireNonNull(callbackAfterThresholdBreached, "Param callbackAfterThresholdBreached must be provided");

        this.threshold = threshold;
        this.thresholdWindow = thresholdWindow;
        this.callbackBeforeThresholdBreached = callbackBeforeThresholdBreached;
        this.callbackAfterThresholdBreached = callbackAfterThresholdBreached;
    }

    /**
     * Increment the number of occurrences.
     */
    public synchronized void increment() {
        if (isNull(timer)) {
            timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handleWindowExpire();
                }
            }, 0, thresholdWindow.toMillis());
        }

        this.count += 1;
        this.thresholdBreached = count > threshold;
        if (!thresholdBreached) {
            callbackBeforeThresholdBreached.accept(null);
        }
    }

    private synchronized void handleWindowExpire() {
        if (thresholdBreached) {
            callbackAfterThresholdBreached.accept(count);
        }

        reset();
    }

    private synchronized void reset() {
        this.count = 0;
        this.thresholdBreached = false;
        this.timer.cancel();
        this.timer = null;
    }
}
