package com.uiu.examintegrity.config;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.uiu.examintegrity.course.CourseAccess;
import com.uiu.examintegrity.exam.infrastructure.code.InteractiveRunner;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.user.AppUserDetails;

/**
 * Decides who may subscribe to which STOMP destination.
 *
 * <p>Everything is checked here and <strong>anything unrecognised is refused</strong>. That
 * deny-by-default is the reason the HTTP rule on {@code /ws/**} can be merely "authenticated":
 * the handshake is open to any signed-in user, but the data behind each destination is not.
 *
 * <p>Separate from {@link WebSocketConfig} so it can be tested directly, rather than only through
 * a live socket.
 */
@Component
public class SubscriptionAuthorizer {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionAuthorizer.class);

    private static final Pattern EXAM_TOPIC = Pattern.compile("^/topic/exams/(\\d+)/risk$");
    private static final Pattern CONSOLE_TOPIC = Pattern.compile("^/topic/console/([0-9a-fA-F-]{36})$");

    private final CourseAccess courseAccess;
    private final ExamRepository examRepository;
    private final InteractiveRunner interactiveRunner;

    public SubscriptionAuthorizer(CourseAccess courseAccess, ExamRepository examRepository,
            InteractiveRunner interactiveRunner) {
        this.courseAccess = courseAccess;
        this.examRepository = examRepository;
        this.interactiveRunner = interactiveRunner;
    }

    /** @throws AccessDeniedException if this principal may not read this destination */
    public void authorize(String destination, Principal user) {
        if (!(user instanceof Authentication auth) || !(auth.getPrincipal() instanceof AppUserDetails account)) {
            throw refused(destination, "no signed-in principal ("
                    + (user == null ? "null" : user.getClass().getSimpleName()) + ")");
        }
        if (destination != null) {
            Matcher exam = EXAM_TOPIC.matcher(destination);
            if (exam.matches()) {
                authorizeRiskTopic(destination, account, Long.valueOf(exam.group(1)));
                return;
            }
            Matcher console = CONSOLE_TOPIC.matcher(destination);
            if (console.matches()) {
                authorizeConsoleTopic(destination, account, console.group(1));
                return;
            }
        }
        throw refused(destination, "unknown topic");
    }

    private void authorizeRiskTopic(String destination, AppUserDetails account, Long examId) {
        boolean allowed = examRepository.findById(examId)
                .map(exam -> courseAccess.isStaff(exam.getCourse().getId(), account.getId()))
                .orElse(false);
        if (!allowed) {
            throw refused(destination, "not staff on this exam's course");
        }
    }

    /**
     * A run id is an unguessable UUID, but that is not the control -- this is. Only the student
     * the run belongs to may read its output, and a run that has ended is refused outright rather
     * than left subscribable.
     */
    private void authorizeConsoleTopic(String destination, AppUserDetails account, String runId) {
        boolean allowed = interactiveRunner.ownerOf(runId)
                .map(owner -> owner.equals(account.getId()))
                .orElse(false);
        if (!allowed) {
            throw refused(destination, "not the owner of this run");
        }
    }

    private static AccessDeniedException refused(String destination, String reason) {
        log.debug("Refused subscription to {}: {}", destination, reason);
        return new AccessDeniedException(reason);
    }
}
