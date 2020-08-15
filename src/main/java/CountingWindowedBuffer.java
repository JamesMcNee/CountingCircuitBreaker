import java.time.Duration;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * An implementation of a count based windowed buffer.
 *
 * Example usage:
 * This class could be used to prevent warn/error logs from overloading logging infrastructure in the event of an incident.
 */
public class CountingWindowedBuffer implements ThresholdBuffer {

    private final int threshold;
    private final Duration thresholdWindow;
    private final Consumer<Integer> callbackAfterThresholdBreached;

    private int count;
    private boolean thresholdBreached;
    private Timer timer;

    /**
     * @param threshold the maximum number of occurrences before the threshold is breached
     * @param thresholdWindow the period of time for which the buffer should be held open once breached
     * @param callbackAfterThresholdBreached the function that will be called on increment once the buffer is active
     */
    public CountingWindowedBuffer(int threshold, Duration thresholdWindow, Consumer<Integer> callbackAfterThresholdBreached) {
        Objects.requireNonNull(callbackAfterThresholdBreached, "Param callbackAfterThresholdBreached must be provided");

        this.threshold = threshold;
        this.thresholdWindow = thresholdWindow;
        this.callbackAfterThresholdBreached = callbackAfterThresholdBreached;
    }

    /**
     * Increment the number of occurrences.
     * @param callbackBeforeThresholdBreached callback function that will be called if threshold has not been breached
     */
    public synchronized void increment(Runnable callbackBeforeThresholdBreached) {
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
            callbackBeforeThresholdBreached.run();
        }
    }

    private synchronized void handleWindowExpire() {
        if (thresholdBreached) {
            callbackAfterThresholdBreached.accept(count);
        }

        reset();
    }

    public synchronized void reset() {
        this.count = 0;
        this.thresholdBreached = false;
        if (nonNull(this.timer)) this.timer.cancel();
        this.timer = null;
    }
}
