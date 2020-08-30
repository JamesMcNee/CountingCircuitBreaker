/*
 * MIT License
 *
 * Copyright (c) 2020 James McNee
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
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
