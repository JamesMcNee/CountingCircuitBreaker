import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.function.Consumer;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountingThresholdBufferTest {

    @Mock
    private Consumer<Void> beforeThresholdBreached;

    @Mock
    private Consumer<Integer> afterThresholdBreached;

    @Test
    void givenThatThresholdNotBreached_shouldTriggerCorrectCallback() {
        // given
        CountingWindowedBuffer buffer = new CountingWindowedBuffer(1, Duration.ofMinutes(1), afterThresholdBreached);

        // when
        buffer.increment(beforeThresholdBreached);
        buffer.increment(beforeThresholdBreached);

        // then
        verify(beforeThresholdBreached, times(1)).accept(any());
        buffer.reset();
    }

    @Test
    void givenThatThresholdBreached_shouldTriggerCorrectCallback() {
        // given
        CountingWindowedBuffer buffer = new CountingWindowedBuffer(1, Duration.ofSeconds(1), afterThresholdBreached);

        // when
        buffer.increment(beforeThresholdBreached);
        buffer.increment(beforeThresholdBreached);

        // then
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            verify(beforeThresholdBreached, times(1)).accept(any());
            verify(afterThresholdBreached, times(1)).accept(2);
        });
        buffer.reset();
    }

    @Test
    void givenThatThresholdBreached_shouldTriggerCallbackWhenWindowExpires() throws InterruptedException {
        // given
        CountingWindowedBuffer buffer = new CountingWindowedBuffer(1, Duration.ofSeconds(3), afterThresholdBreached);

        // when
        buffer.increment(beforeThresholdBreached);
        buffer.increment(beforeThresholdBreached);
        buffer.increment(beforeThresholdBreached);
        Thread.sleep(350); // Wait for window to expire
        buffer.increment(beforeThresholdBreached);

        // then
        await().atMost(Duration.ofSeconds(4)).untilAsserted(() -> {
            verify(beforeThresholdBreached, times(2)).accept(any());
            verify(afterThresholdBreached, times(1)).accept(3);
        });
        buffer.reset();
    }

    @Test
    void givenThatThresholdNotBreached_afterWindowExpires_shouldNotTriggerCallback() throws InterruptedException {
        // given
        CountingWindowedBuffer buffer = new CountingWindowedBuffer(5, Duration.ofSeconds(3), afterThresholdBreached);

        // when
        buffer.increment(beforeThresholdBreached);
        buffer.increment(beforeThresholdBreached);
        buffer.increment(beforeThresholdBreached);
        Thread.sleep(350); // Wait for window to expire

        // then
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            verify(beforeThresholdBreached, times(3)).accept(any());
            verify(afterThresholdBreached, never()).accept(any());
        });
        buffer.reset();
    }

    @Test
    void givenAfterBreachedCallbackNotProvided_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> new CountingWindowedBuffer(1, Duration.ZERO, null));
    }
}