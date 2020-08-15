import java.util.function.Consumer;

public interface ThresholdBuffer {

    default void increment() {
        increment((unused) -> {});
    }

    /**
     * Increment the number of occurrences.
     * @param callbackWhenClosed callback function that will be called if buffer is not yet active
     */
    void increment(Consumer<Void> callbackWhenClosed);
}
