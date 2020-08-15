![build](https://github.com/JamesMcNee/CountingCircuitBreaker/workflows/build/badge.svg)

# Counting Circuit Breaker
A count + time based circuit breaker that is useful for protecting logging infrastructure from being overloaded.

The concept behind this project is to stop systems like logging infrastructure from being overloaded in the event of an incident that would otherwise have generated thousands of log messages. Logging for eceptions that are exceptional and should rarely happen is very useful, this is not the case in a scenario where the majority of actions are causing an exception. This class provides a way to still log that there has been an incident but keeps a record of the number of occurrences, rather than each individually.

Example basic usage:
```java
private final CountingCircuitBreaker circuitBreaker = new CountingCircuitBreaker(5, Duration.ofMinutes(1),
                () -> LOGGER.error("Something catastrophic has happened! This needs looking into..."),
                (count) -> LOGGER.error(String.format("Something catastrophic has happened %d times... This is a disaster!!", count)));
                
public void handleSomeAsyncAction(Action a) {
  try {
    //...
  } catch (Exception e) {
    circuitBreaker.increment();
  }
}
```
