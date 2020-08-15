import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.function.Consumer;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountingCircuitBreakerTest {

    @Mock
    private Consumer<Void> beforeThresholdBreached;

    @Mock
    private Consumer<Integer> afterThresholdBreached;

    @Test
    void givenThatThresholdNotBreached_shouldTriggerCorrectCallback() {
        // given
        CountingCircuitBreaker circuitBreaker = new CountingCircuitBreaker(1, Duration.ofMinutes(1), beforeThresholdBreached, afterThresholdBreached);

        // when
        circuitBreaker.increment();
        circuitBreaker.increment();

        // then
        verify(beforeThresholdBreached, times(1)).accept(any());
    }

    @Test
    void givenThatThresholdBreached_shouldTriggerCorrectCallback() {
        // given
        CountingCircuitBreaker circuitBreaker = new CountingCircuitBreaker(1, Duration.ofSeconds(1), beforeThresholdBreached, afterThresholdBreached);

        // when
        circuitBreaker.increment();
        circuitBreaker.increment();

        // then
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            verify(beforeThresholdBreached, times(1)).accept(any());
            verify(afterThresholdBreached, times(1)).accept(2);
        });
    }

    @Test
    void givenThatThresholdBreached_shouldTriggerCallbackWhenWindowExpires() throws InterruptedException {
        // given
        CountingCircuitBreaker circuitBreaker = new CountingCircuitBreaker(1, Duration.ofSeconds(3), beforeThresholdBreached, afterThresholdBreached);

        // when
        circuitBreaker.increment();
        circuitBreaker.increment();
        circuitBreaker.increment();
        Thread.sleep(350); // Wait for window to expire
        circuitBreaker.increment();

        // then
        await().atMost(Duration.ofSeconds(4)).untilAsserted(() -> {
            verify(beforeThresholdBreached, times(2)).accept(any());
            verify(afterThresholdBreached, times(1)).accept(3);
        });
    }

    @Test
    void givenThatThresholdNotBreached_afterWindowExpires_shouldNotTriggerCallback() throws InterruptedException {
        // given
        CountingCircuitBreaker circuitBreaker = new CountingCircuitBreaker(5, Duration.ofSeconds(3), beforeThresholdBreached, afterThresholdBreached);

        // when
        circuitBreaker.increment();
        circuitBreaker.increment();
        circuitBreaker.increment();
        Thread.sleep(350); // Wait for window to expire

        // then
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            verify(beforeThresholdBreached, times(3)).accept(any());
            verify(afterThresholdBreached, never()).accept(any());
        });
    }
}