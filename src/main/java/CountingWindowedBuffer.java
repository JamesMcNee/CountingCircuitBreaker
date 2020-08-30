/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

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
     * @param callbackIfBufferNotActive callback function that will be called if threshold has not been breached
     */
    public synchronized void increment(Runnable callbackIfBufferNotActive) {
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
            callbackIfBufferNotActive.run();
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
