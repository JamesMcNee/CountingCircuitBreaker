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
class CountingWindowedBufferTest {

    @Mock
    private Runnable beforeThresholdBreached;

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
        verify(beforeThresholdBreached, times(1)).run();
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
            verify(beforeThresholdBreached, times(1)).run();
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
            verify(beforeThresholdBreached, times(2)).run();
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
            verify(beforeThresholdBreached, times(3)).run();
            verify(afterThresholdBreached, never()).accept(any());
        });
        buffer.reset();
    }

    @Test
    void givenAfterBreachedCallbackNotProvided_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> new CountingWindowedBuffer(1, Duration.ZERO, null));
    }
}