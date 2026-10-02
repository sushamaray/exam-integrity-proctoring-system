package com.uiu.examintegrity.exam.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.common.BadRequestException;
import com.uiu.examintegrity.common.ConflictException;
import com.uiu.examintegrity.common.NotFoundException;
import com.uiu.examintegrity.exam.domain.ExamSession;
import com.uiu.examintegrity.exam.domain.QuestionNotOnPaper;
import com.uiu.examintegrity.exam.domain.SessionExpired;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionMapper;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.question.Question;
import com.uiu.examintegrity.question.QuestionRepository;
import com.uiu.examintegrity.question.QuestionType;

/**
 * The checks every "do something with my code" endpoint has to make first: the sitting is yours,
 * it is still open, and that question is on your paper and is answered with code.
 *
 * <p>Shared by {@link CodeRunService} and {@link ConsoleService} so the two cannot disagree about
 * who is allowed to run what.
 *
 * <p>Deliberately uses {@code findById}, not the pessimistic {@code findLockedById} that
 * {@code SessionService} uses. Compiling and running take seconds, and holding a write lock on the
 * session row for that long would block the student's own autosave and the auto-submit sweeper.
 */
@Service
public class SittingGuard {
    private final ExamSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final Clock clock;

    public SittingGuard(ExamSessionRepository sessionRepository, QuestionRepository questionRepository,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.clock = clock;
    }

    /**
     * @return the coding question, once the sitting has been vouched for
     * @throws NotFoundException if the sitting is missing <em>or belongs to somebody else</em> --
     *                           reported the same way on purpose, so ids are not probeable
     */
    @Transactional(readOnly = true)
    public Question codingQuestion(Long sessionId, Long userId, Long questionId) {
        ExamSessionEntity entity = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("No such session"));
        if (!entity.getUser().getId().equals(userId)) {
            throw new NotFoundException("No such session");
        }

        ExamSession session = ExamSessionMapper.toDomain(entity);
        Instant now = clock.instant();
        if (!session.state().acceptsAnswers()) {
            throw new ConflictException("This exam is closed");
        }
        if (session.isExpired(now)) {
            throw new SessionExpired(session.expiresAt());
        }
        if (!session.paper().contains(questionId)) {
            throw new QuestionNotOnPaper(questionId);
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("No such question"));
        if (question.getType() != QuestionType.CODING) {
            throw new BadRequestException("That question is not answered with code");
        }
        return question;
    }
}
