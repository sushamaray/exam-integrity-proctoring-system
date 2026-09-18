package com.uiu.examintegrity.exam.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.common.ConflictException;
import com.uiu.examintegrity.common.NotFoundException;
import com.uiu.examintegrity.exam.application.view.ReviewView;
import com.uiu.examintegrity.exam.application.view.ReviewView.ReviewedQuestion;
import com.uiu.examintegrity.exam.application.view.SessionView.OptionView;
import com.uiu.examintegrity.exam.application.view.SittingSummary;
import com.uiu.examintegrity.exam.domain.Paper;
import com.uiu.examintegrity.exam.persistence.AnswerEntity;
import com.uiu.examintegrity.exam.persistence.AnswerRepository;
import com.uiu.examintegrity.exam.persistence.ExamQuestion;
import com.uiu.examintegrity.exam.persistence.ExamQuestionRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionMapper;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.SessionStatus;
import com.uiu.examintegrity.question.QuestionOption;
import com.uiu.examintegrity.question.QuestionOptionRepository;

@Service
@Transactional(readOnly = true)
public class ReviewService {
    private static final Set<SessionStatus> CLOSED = EnumSet.of(SessionStatus.SUBMITTED, SessionStatus.AUTO_SUBMITTED);

    private final ExamSessionRepository sessionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionOptionRepository optionRepository;

    public ReviewService(ExamSessionRepository sessionRepository, ExamQuestionRepository examQuestionRepository,
            AnswerRepository answerRepository, QuestionOptionRepository optionRepository) {
        this.sessionRepository = sessionRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.answerRepository = answerRepository;
        this.optionRepository = optionRepository;
    }

    public List<SittingSummary> sittings(Long userId) {
        return sessionRepository.findByUserId(userId).stream()
                .filter(s -> CLOSED.contains(s.getStatus()))
                .sorted(Comparator.comparing(ExamSessionEntity::getSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(s -> new SittingSummary(s.getId(), s.getExam().getId(), s.getExam().getTitle(),
                        s.getExam().getCourse().getCode(), s.getSubmittedAt(), s.getStatus().name(),
                        s.getTotalMarks(), maxMarks(s), s.getGradingStatus().name()))
                .toList();
    }

    public ReviewView review(Long sessionId, Long userId) {
        ExamSessionEntity session = sessionRepository.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("No such session"));
        if (!CLOSED.contains(session.getStatus())) {
            throw new ConflictException("The review opens once this exam is submitted");
        }

        Paper paper = ExamSessionMapper.toDomain(session).paper();
        Map<Long, ExamQuestion> pool = new HashMap<>();
        examQuestionRepository.findPool(session.getExam().getId())
                .forEach(eq -> pool.put(eq.getQuestion().getId(), eq));

        Map<Long, Map<String, QuestionOption>> options = new HashMap<>();
        for (QuestionOption option : optionRepository.findByQuestionIdInOrderByLabel(paper.questionOrder())) {
            options.computeIfAbsent(option.getQuestion().getId(), id -> new HashMap<>())
                    .put(option.getLabel(), option);
        }
        Map<Long, AnswerEntity> answers = new HashMap<>();
        answerRepository.findWithOptionBySessionId(sessionId)
                .forEach(a -> answers.put(a.getQuestion().getId(), a));

        List<ReviewedQuestion> questions = new ArrayList<>();
        for (Long id : paper.questionOrder()) {
            ExamQuestion eq = pool.get(id);
            if (eq == null) {
                continue;
            }
            var question = eq.getQuestion();
            Map<String, QuestionOption> byLabel = options.getOrDefault(id, Map.of());
            List<OptionView> shown = paper.optionsFor(id).stream()
                    .map(label -> new OptionView(label, byLabel.get(label).getBody()))
                    .toList();
            String correct = byLabel.values().stream()
                    .filter(QuestionOption::isCorrect).map(QuestionOption::getLabel).findFirst().orElse(null);

            AnswerEntity answer = answers.get(id);
            String yours = answer == null ? null
                    : answer.getOption() != null ? answer.getOption().getLabel() : answer.getTextAnswer();

            questions.add(new ReviewedQuestion(id, question.getType().name(), question.getTopic(),
                    question.getBloomLevel(), question.getBody(), shown, correct, yours, question.getExplanation(),
                    answer == null ? BigDecimal.ZERO : answer.getMarksAwarded(), marksFor(eq)));
        }

        return new ReviewView(sessionId, session.getExam().getTitle(), session.getSubmittedAt(),
                session.getTotalMarks(), maxMarks(session), questions);
    }

    private BigDecimal maxMarks(ExamSessionEntity session) {
        Set<Long> onPaper = session.getQuestionOrder() == null ? Set.of() : Set.copyOf(session.getQuestionOrder());
        return examQuestionRepository.findPool(session.getExam().getId()).stream()
                .filter(eq -> onPaper.contains(eq.getQuestion().getId()))
                .map(ReviewService::marksFor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal marksFor(ExamQuestion eq) {
        return eq.getMarks() != null ? eq.getMarks() : eq.getQuestion().getMaxMarks();
    }
}
