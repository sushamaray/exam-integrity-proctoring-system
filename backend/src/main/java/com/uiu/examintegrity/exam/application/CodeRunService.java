package com.uiu.examintegrity.exam.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.uiu.examintegrity.common.BadRequestException;
import com.uiu.examintegrity.common.ConflictException;
import com.uiu.examintegrity.common.NotFoundException;
import com.uiu.examintegrity.exam.application.view.CodeRunView;
import com.uiu.examintegrity.exam.application.view.LanguageOption;
import com.uiu.examintegrity.exam.domain.ExamSession;
import com.uiu.examintegrity.exam.domain.QuestionNotOnPaper;
import com.uiu.examintegrity.exam.domain.SessionExpired;
import com.uiu.examintegrity.exam.domain.code.CodeExecutor;
import com.uiu.examintegrity.exam.domain.code.CodeRun;
import com.uiu.examintegrity.exam.domain.code.CodingProblem;
import com.uiu.examintegrity.exam.domain.code.Language;
import com.uiu.examintegrity.exam.domain.code.RunOutcome;
import com.uiu.examintegrity.exam.domain.code.SourceGuard;
import com.uiu.examintegrity.exam.domain.code.TestCase;
import com.uiu.examintegrity.exam.domain.code.TestOutcome;
import com.uiu.examintegrity.exam.infrastructure.code.Toolchains;
import com.uiu.examintegrity.exam.persistence.AnswerRepository;
import com.uiu.examintegrity.exam.persistence.CodeSubmission;
import com.uiu.examintegrity.exam.persistence.CodeSubmissionRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionMapper;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.question.Question;
import com.uiu.examintegrity.question.QuestionRepository;
import com.uiu.examintegrity.question.QuestionType;

/**
 * Runs a student submission and records it.
 *
 * <p>Run and Submit are the same path with a different test set: Run uses the sample cases
 * and is throwaway, Submit uses every case including hidden ones and is stored with
 * {@code is_final}, which is what {@code CodingGrading} later reads the marks from.
 *
 * <p>Compiling happens deliberately <em>between</em> two short transactions rather than inside
 * one. {@code SessionService} takes a pessimistic row lock on the session, and holding that
 * across a ten-second compile would block the student's own autosave and the auto-submit
 * sweeper.
 */
@Service
public class CodeRunService {
    private final SittingGuard guard;
    private final ExamSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final CodeSubmissionRepository submissionRepository;
    private final AnswerRepository answerRepository;
    private final List<CodeExecutor> executors;
    private final Toolchains toolchains;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public CodeRunService(SittingGuard guard, ExamSessionRepository sessionRepository,
            QuestionRepository questionRepository,
            CodeSubmissionRepository submissionRepository, AnswerRepository answerRepository,
            List<CodeExecutor> executors, Toolchains toolchains, Clock clock,
            PlatformTransactionManager transactionManager) {
        this.guard = guard;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.executors = executors;
        this.toolchains = toolchains;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * What the selector offers. Every language is listed, including the ones that cannot be run --
     * shown disabled rather than hidden, so a missing toolchain looks like a missing toolchain and
     * not like a feature nobody ever wrote.
     */
    public List<LanguageOption> languages() {
        return java.util.Arrays.stream(Language.values())
                .map(language -> LanguageOption.of(language, toolchains.ready(language)))
                .toList();
    }

    public CodeRunView run(Long sessionId, Long userId, Long questionId, String languageId, String source,
            boolean isFinal) {
        Language language = Language.parse(languageId)
                .orElseThrow(() -> new BadRequestException("There is no language called " + languageId));
        if (!toolchains.ready(language)) {
            throw new BadRequestException(toolchains.refusal(language));
        }
        SourceGuard.reject(language, source).ifPresent(why -> {
            throw new BadRequestException("That submission was not run: " + why);
        });
        CodeExecutor executor = executors.stream()
                .filter(candidate -> candidate.supports(language))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(toolchains.refusal(language)));

        CodingProblem problem = transaction.execute(status -> load(sessionId, userId, questionId));

        List<TestCase> tests = isFinal ? problem.tests() : problem.visibleTests();
        if (tests.isEmpty()) {
            throw new BadRequestException("This question has no sample cases to run; use Submit instead");
        }

        RunOutcome outcome = executor.run(new CodeRun(language, source, tests));

        Long submissionId = transaction.execute(
                status -> persist(sessionId, questionId, language, source, isFinal, outcome));
        return CodeRunView.of(submissionId, language.name(), isFinal, outcome);
    }

    private CodingProblem load(Long sessionId, Long userId, Long questionId) {
        Question question = guard.codingQuestion(sessionId, userId, questionId);

        CodingProblem problem = CodingProblem.from(questionId, question.getStarterCode(), question.getTestCases());
        if (!problem.runnable()) {
            throw new ConflictException("This question has no test cases yet");
        }
        return problem;
    }

    private Long persist(Long sessionId, Long questionId, Language language, String source, boolean isFinal,
            RunOutcome outcome) {
        if (isFinal) {
            answerRepository.upsert(sessionId, questionId, null, source, clock.instant());
        }

        CodeSubmission submission = new CodeSubmission(
                sessionRepository.getReferenceById(sessionId),
                questionRepository.getReferenceById(questionId),
                language.name(), source);

        submission.setFinal(isFinal);
        submission.setStatus(outcome.status());
        submission.setCompileOutput(blankToNull(outcome.compileOutput()));
        submission.setPassedCount(outcome.passedCount());
        submission.setTotalCount(outcome.totalCount());
        submission.setRuntimeMs(outcome.runtimeMs());
        submission.setResult(detail(outcome));

        return submissionRepository.save(submission).getId();
    }

    /**
     * The stored detail keeps hidden cases in full -- this is the server-side record faculty
     * can inspect, unlike {@code CodeRunView} which redacts them.
     */
    private static List<Map<String, Object>> detail(RunOutcome outcome) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TestOutcome test : outcome.tests()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", test.name());
            row.put("hidden", test.hidden());
            row.put("passed", test.passed());
            row.put("status", test.status().name());
            row.put("stdout", test.stdout());
            row.put("stderr", test.stderr());
            row.put("runtimeMs", test.runtimeMs());
            rows.add(row);
        }
        return rows;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
