![build](https://github.com/JamesMcNee/CountingWindowedBuffer/workflows/build/badge.svg)

# Threshold Buffer
A count + time based buffer that is useful for protecting logging infrastructure from being overloaded.

The concept behind this project is to stop systems like logging infrastructure from being overloaded in the event of an incident that would otherwise have generated thousands of log messages. Logging for eceptions that are exceptional and should rarely happen is very useful, this is not the case in a scenario where the majority of actions are causing an exception. This class provides a way to still log that there has been an incident but keeps a record of the number of occurrences, rather than each individually.

The callback provided in the `acknowledge(Runnable ...)` method is called until the threshold defined in the buffer is exceeded at which point the callback provided in the constructor is used.

Example basic usage:
```java
private final ThresholdBuffer buffer = new CountingWinowedBuffer(5, Duration.ofMinutes(1),
                (count) -> LOGGER.error(String.format("Something catastrophic has happened %d times... This is a disaster!!", count)));
                
public void handleSomeAsyncAction(Action a) {
  try {
    //...
  } catch (Exception e) {
    buffer.increment(() -> LOGGER.error("Some more specific text about this particular error + the original exception", e));
  }
}
```
