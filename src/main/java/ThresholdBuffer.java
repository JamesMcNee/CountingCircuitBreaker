public interface ThresholdBuffer {

    default void increment() {
        increment(() -> {});
    }

    /**
     * Increment the number of occurrences.
     * @param callbackIfBufferNotActive callback function that will be called if buffer is not yet active
     */
    void increment(Runnable callbackIfBufferNotActive);
}
