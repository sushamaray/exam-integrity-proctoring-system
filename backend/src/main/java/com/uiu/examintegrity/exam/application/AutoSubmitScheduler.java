package com.uiu.examintegrity.exam.application;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.uiu.examintegrity.exam.domain.IllegalStateTransition;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.SessionStatus;

@Component
public class AutoSubmitScheduler {
    private static final Logger log = LoggerFactory.getLogger(AutoSubmitScheduler.class);

    private final ExamSessionRepository sessionRepository;
    private final SessionService sessionService;
    private final Clock clock;

    public AutoSubmitScheduler(ExamSessionRepository sessionRepository, SessionService sessionService,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.sessionService = sessionService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${exam.auto-submit.interval-ms:30000}")
    public void closeExpiredSessions() {
        var expired = sessionRepository.findByStatusAndExpiresAtBefore(SessionStatus.IN_PROGRESS, clock.instant());
        for (ExamSessionEntity session : expired) {
            try {
                sessionService.autoSubmit(session.getId());
            } catch (IllegalStateTransition submittedMeanwhile) {
                log.debug("Session {} closed before the sweep reached it", session.getId());
            } catch (RuntimeException e) {
                log.warn("Could not auto-submit session {}", session.getId(), e);
            }
        }
        if (!expired.isEmpty()) {
            log.info("Auto-submit sweep closed up to {} expired sessions", expired.size());
        }
    }
}
