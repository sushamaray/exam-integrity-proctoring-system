package com.uiu.examintegrity.proctoring.domain;

import static com.uiu.examintegrity.proctoring.domain.EventType.ANSWER;
import static com.uiu.examintegrity.proctoring.domain.EventType.FS_EXIT;
import static com.uiu.examintegrity.proctoring.domain.EventType.HEARTBEAT;
import static com.uiu.examintegrity.proctoring.domain.EventType.PASTE;
import static com.uiu.examintegrity.proctoring.domain.EventType.TAB_HIDDEN;
import static com.uiu.examintegrity.proctoring.domain.EventType.TAB_VISIBLE;
import static com.uiu.examintegrity.proctoring.domain.EventType.WINDOW_BLUR;
import static com.uiu.examintegrity.proctoring.domain.EventType.WINDOW_FOCUS;
import static com.uiu.examintegrity.proctoring.domain.Events.at;
import static com.uiu.examintegrity.proctoring.domain.Events.event;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.uiu.examintegrity.proctoring.domain.detectors.FocusLossDetector;
import com.uiu.examintegrity.proctoring.domain.detectors.FullscreenExitDetector;
import com.uiu.examintegrity.proctoring.domain.detectors.HeartbeatGapDetector;
import com.uiu.examintegrity.proctoring.domain.detectors.IdleThenBurstDetector;
import com.uiu.examintegrity.proctoring.domain.detectors.LargePasteDetector;
import com.uiu.examintegrity.proctoring.domain.detectors.TabSwitchDetector;

class DetectorsTest {
    @Nested
    class LargePaste {
        @Test
        @DisplayName("a 240-character paste is a confident signal")
        void detectsLargePaste() {
            var window = SessionWindow.of(event(PASTE, "charCount", 240, at("14:41:02")));
            var signal = new LargePasteDetector().evaluate(window);
            assertThat(signal).isPresent();
            assertThat(signal.get().confidence()).isGreaterThan(0.8);
            assertThat(signal.get().detector()).isEqualTo("LARGE_PASTE");
        }

        @Test
        @DisplayName("a short paste is nothing")
        void ignoresSmallPaste() {
            var window = SessionWindow.of(event(PASTE, "charCount", 40, at("14:41:02")));
            assertThat(new LargePasteDetector().evaluate(window)).isEmpty();
        }

        @Test
        @DisplayName("a paste more than 30 seconds before the newest event is out of view")
        void oldPasteOutOfView() {
            var window = SessionWindow.of(
                    event(PASTE, "charCount", 400, at("14:40:00")),
                    event(HEARTBEAT, at("14:41:00")));
            assertThat(new LargePasteDetector().evaluate(window)).isEmpty();
        }

        @Test
        @DisplayName("bigger pastes are more confident")
        void confidenceGrowsWithSize() {
            var small = new LargePasteDetector().evaluate(
                    SessionWindow.of(event(PASTE, "charCount", 130, at("14:41:02")))).orElseThrow();
            var large = new LargePasteDetector().evaluate(
                    SessionWindow.of(event(PASTE, "charCount", 900, at("14:41:02")))).orElseThrow();
            assertThat(large.confidence()).isGreaterThan(small.confidence());
            assertThat(large.confidence()).isLessThan(1.0);
        }
    }

    @Nested
    class FocusLoss {
        @Test
        @DisplayName("fifteen seconds focused on another window is a signal")
        void longAbsence() {
            var window = SessionWindow.of(
                    event(WINDOW_BLUR, at("14:41:00")),
                    event(WINDOW_FOCUS, at("14:41:15")));
            var signal = new FocusLossDetector().evaluate(window);
            assertThat(signal).isPresent();
            assertThat(signal.get().eventIds()).hasSize(2);
        }

        @Test
        @DisplayName("a departure in which the page was hidden is a tab switch, not focus loss")
        void tabSwitchIsNotFocusLoss() {
            var window = SessionWindow.of(
                    event(WINDOW_BLUR, at("14:41:00")),
                    event(TAB_HIDDEN, at("14:41:01")),
                    event(TAB_VISIBLE, at("14:41:20")),
                    event(WINDOW_FOCUS, at("14:41:20")));
            assertThat(new FocusLossDetector().evaluate(window)).isEmpty();
        }

        @Test
        @DisplayName("a two-second glance is nothing")
        void briefGlance() {
            var window = SessionWindow.of(
                    event(WINDOW_BLUR, at("14:41:00")),
                    event(WINDOW_FOCUS, at("14:41:02")));
            assertThat(new FocusLossDetector().evaluate(window)).isEmpty();
        }

        @Test
        @DisplayName("three short departures in two minutes add up to a signal")
        void repeatedGlances() {
            var window = SessionWindow.of(
                    event(WINDOW_BLUR, at("14:41:00")), event(WINDOW_FOCUS, at("14:41:02")),
                    event(WINDOW_BLUR, at("14:41:20")), event(WINDOW_FOCUS, at("14:41:22")),
                    event(WINDOW_BLUR, at("14:41:40")), event(WINDOW_FOCUS, at("14:41:42")));
            assertThat(new FocusLossDetector().evaluate(window)).isPresent();
        }

        @Test
        @DisplayName("still away counts up to the newest event")
        void stillAway() {
            var window = SessionWindow.of(
                    event(WINDOW_BLUR, at("14:41:00")),
                    event(HEARTBEAT, at("14:41:30")));
            assertThat(new FocusLossDetector().evaluate(window)).isPresent();
        }
    }

    @Nested
    class TabSwitch {
        @Test
        @DisplayName("leaving the exam tab, even briefly, is a full-strength rule violation")
        void anySwitchCounts() {
            var window = SessionWindow.of(
                    event(TAB_HIDDEN, at("14:41:00")),
                    event(TAB_VISIBLE, at("14:41:02")));
            var detector = new TabSwitchDetector();
            var signal = detector.evaluate(window);
            assertThat(detector.isRuleViolation()).isTrue();
            assertThat(signal).isPresent();
            assertThat(signal.get().eventIds()).hasSize(2);

            assertThat(RiskTier.of(signal.get().weighted() * 100)).isEqualTo(RiskTier.REVIEW);
        }

        @Test
        @DisplayName("longer away is more certain")
        void longerIsSurer() {
            var brief = new TabSwitchDetector().evaluate(SessionWindow.of(
                    event(TAB_HIDDEN, at("14:41:00")), event(TAB_VISIBLE, at("14:41:02")))).orElseThrow();
            var long_ = new TabSwitchDetector().evaluate(SessionWindow.of(
                    event(TAB_HIDDEN, at("14:41:00")), event(TAB_VISIBLE, at("14:41:45")))).orElseThrow();
            assertThat(long_.confidence()).isGreaterThan(brief.confidence());
        }

        @Test
        @DisplayName("no hidden page, no signal")
        void none() {
            assertThat(new TabSwitchDetector().evaluate(SessionWindow.of(
                    event(WINDOW_BLUR, at("14:41:00")), event(WINDOW_FOCUS, at("14:41:30"))))).isEmpty();
        }
    }

    @Nested
    class FullscreenExit {
        @Test
        @DisplayName("leaving fullscreen is a low-weight signal that firms up with repeats")
        void exits() {
            var once = new FullscreenExitDetector().evaluate(
                    SessionWindow.of(event(FS_EXIT, at("14:41:00")))).orElseThrow();
            var thrice = new FullscreenExitDetector().evaluate(SessionWindow.of(
                    event(FS_EXIT, at("14:41:00")), event(FS_EXIT, at("14:41:20")),
                    event(FS_EXIT, at("14:41:40")))).orElseThrow();
            assertThat(once.weight()).isLessThan(0.3);
            assertThat(thrice.confidence()).isGreaterThan(once.confidence());
        }
    }

    @Nested
    class HeartbeatGap {
        @Test
        @DisplayName("a 40-second silence between heartbeats is a signal")
        void gap() {
            var window = SessionWindow.of(
                    event(HEARTBEAT, at("14:41:00")),
                    event(HEARTBEAT, at("14:41:05")),
                    event(HEARTBEAT, at("14:41:45")));
            var signal = new HeartbeatGapDetector().evaluate(window);
            assertThat(signal).isPresent();
            assertThat(signal.get().at()).isEqualTo(at("14:41:45"));
        }

        @Test
        @DisplayName("regular heartbeats are nothing")
        void regular() {
            var window = SessionWindow.of(
                    event(HEARTBEAT, at("14:41:00")),
                    event(HEARTBEAT, at("14:41:05")),
                    event(HEARTBEAT, at("14:41:10")));
            assertThat(new HeartbeatGapDetector().evaluate(window)).isEmpty();
        }
    }

    @Nested
    class IdleThenBurst {
        @Test
        @DisplayName("two idle minutes, then four questions answered in fifteen seconds")
        void burst() {
            var window = SessionWindow.of(
                    event(ANSWER, at("14:38:00"), 1L),
                    event(ANSWER, at("14:40:10"), 2L),
                    event(ANSWER, at("14:40:15"), 3L),
                    event(ANSWER, at("14:40:20"), 4L),
                    event(ANSWER, at("14:40:25"), 5L));
            var signal = new IdleThenBurstDetector().evaluate(window);
            assertThat(signal).isPresent();
            assertThat(signal.get().eventIds()).hasSize(4);
        }

        @Test
        @DisplayName("steady answering is nothing")
        void steady() {
            var window = SessionWindow.of(
                    event(ANSWER, at("14:38:00"), 1L),
                    event(ANSWER, at("14:39:00"), 2L),
                    event(ANSWER, at("14:40:00"), 3L),
                    event(ANSWER, at("14:41:00"), 4L));
            assertThat(new IdleThenBurstDetector().evaluate(window)).isEmpty();
        }

        @Test
        @DisplayName("changing one answer repeatedly is not a burst")
        void dithering() {
            var window = SessionWindow.of(
                    event(ANSWER, at("14:38:00"), 1L),
                    event(ANSWER, at("14:40:10"), 2L),
                    event(ANSWER, at("14:40:12"), 2L),
                    event(ANSWER, at("14:40:14"), 2L),
                    event(ANSWER, at("14:40:16"), 2L));
            assertThat(new IdleThenBurstDetector().evaluate(window)).isEmpty();
        }
    }
}
