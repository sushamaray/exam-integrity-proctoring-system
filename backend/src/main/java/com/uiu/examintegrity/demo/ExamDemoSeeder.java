package com.uiu.examintegrity.demo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.analysis.application.ItemAnalysisService;
import com.uiu.examintegrity.similarity.application.SimilarityService;
import com.uiu.examintegrity.course.Course;
import com.uiu.examintegrity.course.CourseRepository;
import com.uiu.examintegrity.course.CourseRole;
import com.uiu.examintegrity.course.Enrollment;
import com.uiu.examintegrity.course.EnrollmentRepository;
import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.exam.persistence.ExamQuestion;
import com.uiu.examintegrity.exam.persistence.ExamQuestionRepository;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.exam.persistence.ExamStatus;
import com.uiu.examintegrity.exam.domain.Paper;
import com.uiu.examintegrity.exam.domain.QuestionDraw.DrawRules;
import com.uiu.examintegrity.exam.domain.QuestionDraw.PoolItem;
import com.uiu.examintegrity.exam.domain.draw.RandomDraw;
import com.uiu.examintegrity.proctoring.persistence.Flag;
import com.uiu.examintegrity.proctoring.persistence.FlagRepository;
import com.uiu.examintegrity.proctoring.persistence.ProctoringEventEntity;
import com.uiu.examintegrity.proctoring.persistence.ProctoringEventRepository;
import com.uiu.examintegrity.proctoring.persistence.RiskScore;
import com.uiu.examintegrity.proctoring.persistence.RiskScoreRepository;
import com.uiu.examintegrity.proctoring.domain.RiskTier;
import com.uiu.examintegrity.question.Question;
import com.uiu.examintegrity.question.QuestionOption;
import com.uiu.examintegrity.question.QuestionOptionRepository;
import com.uiu.examintegrity.question.QuestionRepository;
import com.uiu.examintegrity.question.QuestionType;
import com.uiu.examintegrity.exam.persistence.AnswerEntity;
import com.uiu.examintegrity.exam.persistence.AnswerRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.GradingStatus;
import com.uiu.examintegrity.exam.persistence.SessionStatus;
import com.uiu.examintegrity.user.Role;
import com.uiu.examintegrity.user.User;
import com.uiu.examintegrity.user.UserRepository;

@Component
@Profile("local")
@Order(2)
public class ExamDemoSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ExamDemoSeeder.class);

    private static final long SEED = 20240918L;
    private static final int STUDENT_COUNT = 40;
    private static final String DEMO_PASSWORD = "password123";
    private static final String COURSE_CODE = "CSE 4103";

    private static final int TRAP_QUESTION = 7;

    private static final int CLUSTERED_SESSIONS = 4;
    private static final Duration CLUSTER_WINDOW = Duration.ofSeconds(90);

    private static final int TEXT_SOURCE = 3;
    private static final int TEXT_COPIER = 11;
    private static final int MCQ_SOURCE = 17;
    private static final int MCQ_COPIER = 24;

    private static final String DISTINCTIVE =
            "A useful way to picture it is a box of LEGO bricks, where one brick is swapped"
            + " without re-moulding the whole castle.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamSessionRepository examSessionRepository;
    private final AnswerRepository answerRepository;
    private final ProctoringEventRepository proctoringEventRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final FlagRepository flagRepository;
    private final SimilarityService similarityService;
    private final ItemAnalysisService itemAnalysisService;

    private final Random random = new Random(SEED);

    public ExamDemoSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
            CourseRepository courseRepository, EnrollmentRepository enrollmentRepository,
            QuestionRepository questionRepository, QuestionOptionRepository questionOptionRepository,
            ExamRepository examRepository, ExamQuestionRepository examQuestionRepository,
            ExamSessionRepository examSessionRepository, AnswerRepository answerRepository,
            ProctoringEventRepository proctoringEventRepository, RiskScoreRepository riskScoreRepository,
            FlagRepository flagRepository, SimilarityService similarityService,
            ItemAnalysisService itemAnalysisService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.examSessionRepository = examSessionRepository;
        this.answerRepository = answerRepository;
        this.proctoringEventRepository = proctoringEventRepository;
        this.riskScoreRepository = riskScoreRepository;
        this.flagRepository = flagRepository;
        this.similarityService = similarityService;
        this.itemAnalysisService = itemAnalysisService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (courseRepository.count() > 0) {
            return;
        }

        Course course = courseRepository.save(
                new Course(COURSE_CODE, "Advanced Object Oriented Programming"));

        User faculty = requireAccount("faculty@uiu.ac.bd");
        User invigilator = requireAccount("invigilator@uiu.ac.bd");

        List<User> cohort = new ArrayList<>();
        cohort.add(requireAccount("student@uiu.ac.bd"));
        cohort.addAll(seedStudents());

        enrol(course, cohort, faculty, invigilator);

        List<Question> questions = seedQuestions(course, faculty);
        Map<Long, List<QuestionOption>> optionsByQuestion = loadOptions(questions);

        Map<Long, DemoQuestionBank.WrittenItem> written = seedWrittenQuestions(course, faculty);
        List<Question> pastPool = new ArrayList<>(questions);
        pastPool.addAll(questionRepository.findAllById(written.keySet()));

        Instant now = Instant.now();

        Exam past = seedExam(course, faculty, "Midterm -- Design Patterns and Generics",
                ExamStatus.CLOSED, now.minus(21, ChronoUnit.DAYS), 45, 75, pastPool);

        Exam live = seedExam(course, faculty, "Final -- Collections, Concurrency and Exceptions",
                ExamStatus.LIVE, now.minus(35, ChronoUnit.MINUTES), 60, 8 * 60, questions);

        // Its own exam rather than a question on the Final: a coding sitting runs in the editor
        // at /student/coding, so a mixed paper would have nowhere to render half of itself.
        List<Question> codingPool = seedCodingQuestions(course, faculty);
        Exam lab = seedExam(course, faculty, "Lab Test 1 -- Loops and Arrays in C",
                ExamStatus.LIVE, now.minus(10, ChronoUnit.MINUTES), 45, 8 * 60, codingPool, null);

        Map<Long, Double> ability = new HashMap<>();
        for (User student : cohort) {
            ability.put(student.getId(), 0.25 + random.nextDouble() * 0.7);
        }

        ability.put(cohort.get(MCQ_SOURCE).getId(), 0.3);

        List<ExamSessionEntity> pastSittings =
                seedPastSittings(past, cohort, pastPool, optionsByQuestion, ability, written);
        plantCollusion(pastSittings, written);
        itemAnalysisService.recomputeUnchecked(past.getId());

        List<ExamSessionEntity> liveSittings = seedLiveSittings(live, cohort.subList(1, cohort.size()),
                questions, optionsByQuestion, ability);
        seedProctoring(liveSittings, questions);

        var report = similarityService.runFor(past.getId());

        log.info("Seeded demo course {} -- {} students, {} questions ({} coding), {} sittings, "
                + "{} events, {} similarity pairs, lab test #{}",
                COURSE_CODE, cohort.size(), pastPool.size() + codingPool.size(), codingPool.size(),
                pastSittings.size() + liveSittings.size(), proctoringEventRepository.count(),
                report.edges().size(), lab.getId());
    }

    private User requireAccount(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new IllegalStateException(
                "DemoDataSeeder must run first; no account for " + email));
    }

    private static final List<String> FIRST_NAMES = List.of(
            "Tanvir", "Nusrat", "Sadia", "Rafid", "Mehjabin", "Arifur", "Samiha", "Shahriar",
            "Farhan", "Tasnim", "Ishrat", "Rakibul", "Anika", "Mahmud", "Sumaiya", "Zahid",
            "Nabila", "Imran", "Raisa", "Shuvo");
    private static final List<String> LAST_NAMES = List.of(
            "Rahman", "Hossain", "Chowdhury", "Karim", "Akter", "Islam", "Haque", "Siddique",
            "Mahmood", "Bhuiyan");

    private List<User> seedStudents() {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        List<User> students = new ArrayList<>(STUDENT_COUNT);

        for (int i = 0; i < STUDENT_COUNT; i++) {
            String studentId = "0112210%03d".formatted(i + 1);
            String name = FIRST_NAMES.get(i % FIRST_NAMES.size()) + " "
                    + LAST_NAMES.get((i / FIRST_NAMES.size() + i) % LAST_NAMES.size());

            User student = new User(name, studentId + "@bscse.uiu.ac.bd", hash, Role.STUDENT);
            student.setStudentId(studentId);
            students.add(student);
        }

        return userRepository.saveAll(students);
    }

    private void enrol(Course course, List<User> cohort, User faculty, User invigilator) {
        List<Enrollment> enrollments = new ArrayList<>();
        for (User student : cohort) {
            enrollments.add(new Enrollment(course, student, CourseRole.STUDENT));
        }
        enrollments.add(new Enrollment(course, faculty, CourseRole.INSTRUCTOR));
        enrollments.add(new Enrollment(course, invigilator, CourseRole.INSTRUCTOR));
        enrollmentRepository.saveAll(enrollments);
    }

    private static final List<String> LABELS = List.of("a", "b", "c", "d");

    private List<Question> seedQuestions(Course course, User author) {
        List<Question> saved = new ArrayList<>();

        for (DemoQuestionBank.Item item : DemoQuestionBank.ITEMS) {
            Question question = new Question(course, QuestionType.MCQ, item.body(), BigDecimal.ONE);
            question.setTopic(item.topic());
            question.setBloomLevel(item.bloomLevel());
            question.setDifficulty(item.difficulty());
            question.setExplanation(item.explanation());
            question.setCreatedBy(author);
            question = questionRepository.save(question);

            List<String> bodies = new ArrayList<>(item.options());
            String correctBody = bodies.get(item.correctIndex());
            java.util.Collections.shuffle(bodies, random);

            List<QuestionOption> options = new ArrayList<>();
            for (int i = 0; i < bodies.size(); i++) {
                String body = bodies.get(i);
                options.add(new QuestionOption(question, LABELS.get(i), body, body.equals(correctBody)));
            }
            questionOptionRepository.saveAll(options);

            saved.add(question);
        }

        return saved;
    }

    private Map<Long, DemoQuestionBank.WrittenItem> seedWrittenQuestions(Course course, User author) {
        Map<Long, DemoQuestionBank.WrittenItem> byId = new LinkedHashMap<>();
        for (DemoQuestionBank.WrittenItem item : DemoQuestionBank.WRITTEN) {
            Question question = new Question(course, QuestionType.SHORT_ANSWER, item.body(), new BigDecimal("3"));
            question.setTopic(item.topic());
            question.setBloomLevel(item.bloomLevel());
            question.setDifficulty(item.difficulty());
            question.setExplanation(item.explanation());
            question.setCreatedBy(author);
            byId.put(questionRepository.save(question).getId(), item);
        }
        return byId;
    }

    private String composeAnswer(DemoQuestionBank.WrittenItem item) {
        List<String> parts = new ArrayList<>();
        parts.add(item.openers().get(random.nextInt(item.openers().size())));
        List<List<String>> ideas = new ArrayList<>(item.ideas());
        java.util.Collections.shuffle(ideas, random);
        for (List<String> paraphrases : ideas.subList(0, 3)) {
            parts.add(paraphrases.get(random.nextInt(paraphrases.size())));
        }
        parts.add(item.examples().get(random.nextInt(item.examples().size())));
        return String.join(" ", parts);
    }

    private Map<Long, List<QuestionOption>> loadOptions(List<Question> questions) {
        Map<Long, List<QuestionOption>> byQuestion = new LinkedHashMap<>();
        for (Question question : questions) {
            byQuestion.put(question.getId(),
                    questionOptionRepository.findByQuestionIdOrderByLabel(question.getId()));
        }
        return byQuestion;
    }

    private Exam seedExam(Course course, User author, String title, ExamStatus status,
            Instant startsAt, int durationMinutes, int windowMinutes, List<Question> pool) {
        return seedExam(course, author, title, status, startsAt, durationMinutes, windowMinutes, pool,
                BigDecimal.ONE);
    }

    /**
     * @param marksPerQuestion the flat override to put on every pooled question, or null to keep
     *                         each question's own max -- a coding question is worth far more than
     *                         one mark, and flattening it would make the grade meaningless
     */
    private Exam seedExam(Course course, User author, String title, ExamStatus status,
            Instant startsAt, int durationMinutes, int windowMinutes, List<Question> pool,
            BigDecimal marksPerQuestion) {
        Exam exam = new Exam(course, title, startsAt,
                startsAt.plus(windowMinutes, ChronoUnit.MINUTES), durationMinutes);
        exam.setStatus(status);
        exam.setCreatedBy(author);
        exam = examRepository.save(exam);

        List<ExamQuestion> pooled = new ArrayList<>();
        for (Question question : pool) {
            pooled.add(new ExamQuestion(exam, question,
                    marksPerQuestion != null ? marksPerQuestion : question.getMaxMarks()));
        }
        examQuestionRepository.saveAll(pooled);

        return exam;
    }

    /**
     * The coding questions, with their starter code and cases. These are seeded rather than
     * authored because the CSV import path only understands MCQ and written questions.
     */
    private List<Question> seedCodingQuestions(Course course, User author) {
        List<Question> saved = new ArrayList<>();
        for (DemoQuestionBank.CodingItem item : DemoQuestionBank.CODING) {
            Question question = new Question(course, QuestionType.CODING, item.body(),
                    new BigDecimal(item.marks()));
            question.setTopic(item.topic());
            question.setBloomLevel(item.bloomLevel());
            question.setDifficulty(item.difficulty());
            question.setExplanation(item.explanation());
            question.setStarterCode(item.starterCode());
            question.setTestCases(testCases(item));
            question.setCreatedBy(author);
            saved.add(questionRepository.save(question));
        }
        return saved;
    }

    private static List<Map<String, Object>> testCases(DemoQuestionBank.CodingItem item) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (DemoQuestionBank.Case testCase : item.cases()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", testCase.name());
            row.put("input", testCase.input());
            row.put("expected", testCase.expected());
            row.put("hidden", testCase.hidden());
            rows.add(row);
        }
        return rows;
    }

    private void drawPaper(ExamSessionEntity sitting, List<Question> questions,
            Map<Long, List<QuestionOption>> optionsByQuestion) {
        List<PoolItem> pool = new ArrayList<>();
        for (Question question : questions) {
            List<String> labels = new ArrayList<>();
            for (QuestionOption option : optionsByQuestion.getOrDefault(question.getId(), List.of())) {
                labels.add(option.getLabel());
            }
            pool.add(new PoolItem(question.getId(), question.getTopic(), labels));
        }

        Paper paper = new RandomDraw().draw(pool, new DrawRules(null, true, true), random);

        Map<String, List<String>> optionOrder = new LinkedHashMap<>();
        paper.optionOrder().forEach((id, labels) -> optionOrder.put(String.valueOf(id), labels));
        sitting.setQuestionOrder(new ArrayList<>(paper.questionOrder()));
        sitting.setOptionOrder(optionOrder);
    }

    private boolean answersCorrectly(double ability, int questionIndex, Short difficulty) {
        double hardness = (difficulty == null ? 3 : difficulty) / 6.0;

        double p = questionIndex == TRAP_QUESTION
                ? 0.92 - ability * 0.95
                : Math.min(0.96, Math.max(0.08, ability - hardness + 0.55));
        return random.nextDouble() < p;
    }

    private List<ExamSessionEntity> seedPastSittings(Exam exam, List<User> cohort, List<Question> questions,
            Map<Long, List<QuestionOption>> optionsByQuestion, Map<Long, Double> ability,
            Map<Long, DemoQuestionBank.WrittenItem> written) {
        List<ExamSessionEntity> sittings = new ArrayList<>();

        for (User student : cohort) {
            Instant startedAt = exam.getStartsAt().plusSeconds(random.nextInt(300));
            Instant expiresAt = startedAt.plus(exam.getDurationMinutes(), ChronoUnit.MINUTES);

            ExamSessionEntity sitting = new ExamSessionEntity(exam, student);
            sitting.setStatus(SessionStatus.SUBMITTED);
            sitting.setConsentAt(startedAt.minusSeconds(40));
            sitting.setStartedAt(startedAt);
            sitting.setExpiresAt(expiresAt);
            sitting.setSubmittedAt(startedAt.plusSeconds(900 + random.nextInt(1500)));
            sitting.setIpAddress("10.12.%d.%d".formatted(random.nextInt(8), 2 + random.nextInt(250)));
            sitting.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/140.0.0.0");

            sitting.setGradingStatus(written.isEmpty() ? GradingStatus.COMPLETE : GradingStatus.PARTIAL);
            drawPaper(sitting, questions, optionsByQuestion);
            sitting = examSessionRepository.save(sitting);

            BigDecimal total = writeAnswers(sitting, questions, optionsByQuestion,
                    ability.get(student.getId()), startedAt, questions.size(), written);
            sitting.setTotalMarks(total);
            sittings.add(examSessionRepository.save(sitting));
        }

        return sittings;
    }

    private List<ExamSessionEntity> seedLiveSittings(Exam exam, List<User> cohort, List<Question> questions,
            Map<Long, List<QuestionOption>> optionsByQuestion, Map<Long, Double> ability) {
        List<ExamSessionEntity> sittings = new ArrayList<>();

        for (User student : cohort) {
            Instant startedAt = exam.getStartsAt().plusSeconds(random.nextInt(240));

            ExamSessionEntity sitting = new ExamSessionEntity(exam, student);
            sitting.setStatus(SessionStatus.IN_PROGRESS);
            sitting.setConsentAt(startedAt.minusSeconds(30));
            sitting.setStartedAt(startedAt);
            sitting.setExpiresAt(startedAt.plus(exam.getDurationMinutes(), ChronoUnit.MINUTES));
            sitting.setIpAddress("10.12.%d.%d".formatted(random.nextInt(8), 2 + random.nextInt(250)));
            sitting.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/140.0.0.0");
            drawPaper(sitting, questions, optionsByQuestion);
            sitting = examSessionRepository.save(sitting);

            int answered = 3 + random.nextInt(questions.size() - 3);
            writeAnswers(sitting, questions, optionsByQuestion,
                    ability.get(student.getId()), startedAt, answered, Map.of());

            sittings.add(sitting);
        }

        return sittings;
    }

    private BigDecimal writeAnswers(ExamSessionEntity sitting, List<Question> questions,
            Map<Long, List<QuestionOption>> optionsByQuestion, double ability,
            Instant startedAt, int count, Map<Long, DemoQuestionBank.WrittenItem> written) {
        Map<Long, Integer> indexById = new HashMap<>();
        Map<Long, Question> byId = new HashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            indexById.put(questions.get(i).getId(), i);
            byId.put(questions.get(i).getId(), questions.get(i));
        }

        List<AnswerEntity> answers = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        Instant cursor = startedAt;

        for (Long questionId : sitting.getQuestionOrder().subList(0, count)) {
            Question question = byId.get(questionId);

            if (written.containsKey(questionId)) {
                int seconds = 120 + random.nextInt(360);
                AnswerEntity answer = new AnswerEntity(sitting, question);
                answer.setTextAnswer(composeAnswer(written.get(questionId)));
                answer.setFirstSeenAt(cursor);
                answer.setAnsweredAt(cursor.plusSeconds(seconds));
                answers.add(answer);
                cursor = cursor.plusSeconds(seconds + 3L);
                continue;
            }

            List<QuestionOption> options = optionsByQuestion.get(questionId);

            boolean correct = answersCorrectly(ability, indexById.get(questionId), question.getDifficulty());
            QuestionOption chosen = pickOption(options, correct);

            int seconds = 25 + random.nextInt(140);
            AnswerEntity answer = new AnswerEntity(sitting, question);
            answer.setOption(chosen);
            answer.setFirstSeenAt(cursor);
            answer.setAnsweredAt(cursor.plusSeconds(seconds));

            answer.setFlagged(random.nextInt(8) == 0);

            if (sitting.getStatus() == SessionStatus.SUBMITTED) {
                answer.setMarksAwarded(chosen.isCorrect() ? BigDecimal.ONE : BigDecimal.ZERO);
                answer.setGradedAt(sitting.getSubmittedAt());
            }
            if (chosen.isCorrect()) {
                total = total.add(BigDecimal.ONE);
            }

            answers.add(answer);
            cursor = cursor.plusSeconds(seconds + 3L);
        }

        answerRepository.saveAll(answers);
        return total;
    }

    private QuestionOption pickOption(List<QuestionOption> options, boolean correct) {
        List<QuestionOption> candidates = new ArrayList<>();
        for (QuestionOption option : options) {
            if (option.isCorrect() == correct) {
                candidates.add(option);
            }
        }
        return candidates.get(random.nextInt(candidates.size()));
    }


    private void plantCollusion(List<ExamSessionEntity> sittings, Map<Long, DemoQuestionBank.WrittenItem> written) {
        Long writtenId = written.keySet().iterator().next();

        AnswerEntity source = answerTo(sittings.get(TEXT_SOURCE), writtenId);
        AnswerEntity copy = answerTo(sittings.get(TEXT_COPIER), writtenId);
        source.setTextAnswer(source.getTextAnswer() + " " + DISTINCTIVE);

        String ownOpener = copy.getTextAnswer().substring(0, copy.getTextAnswer().indexOf('.') + 1);
        String borrowed = source.getTextAnswer().substring(source.getTextAnswer().indexOf('.') + 1);
        copy.setTextAnswer((ownOpener + borrowed).replace(" because ", " since ").replace(" every ", " each "));
        answerRepository.saveAll(List.of(source, copy));

        Map<Long, QuestionOption> sourceChoices = new HashMap<>();
        for (AnswerEntity answer : answerRepository.findBySessionId(sittings.get(MCQ_SOURCE).getId())) {
            if (answer.getOption() != null) {
                sourceChoices.put(answer.getQuestion().getId(), answer.getOption());
            }
        }
        ExamSessionEntity copier = sittings.get(MCQ_COPIER);
        BigDecimal total = BigDecimal.ZERO;
        List<AnswerEntity> copied = answerRepository.findBySessionId(copier.getId());
        for (AnswerEntity answer : copied) {
            QuestionOption choice = sourceChoices.get(answer.getQuestion().getId());
            if (choice != null) {
                answer.setOption(choice);
                answer.setMarksAwarded(choice.isCorrect() ? BigDecimal.ONE : BigDecimal.ZERO);
                total = total.add(answer.getMarksAwarded());
            }
        }
        answerRepository.saveAll(copied);
        copier.setTotalMarks(total);
        examSessionRepository.save(copier);
    }

    private AnswerEntity answerTo(ExamSessionEntity sitting, Long questionId) {
        return answerRepository.findBySessionIdAndQuestionId(sitting.getId(), questionId).orElseThrow();
    }

    private static final int HEARTBEAT_SECONDS = 60;

    private static final List<String> AMBIENT_EVENTS = List.of(
            "WINDOW_BLUR", "WINDOW_FOCUS", "TAB_HIDDEN", "TAB_VISIBLE", "COPY", "FS_EXIT");

    private void seedProctoring(List<ExamSessionEntity> sittings, List<Question> questions) {
        List<Integer> clustered = List.of(2, 9, 15, 28).subList(0, CLUSTERED_SESSIONS);

        List<Integer> borderline = List.of(5, 12, 20, 26, 33, 37);
        Instant clusterStart = sittings.get(0).getStartedAt().plus(18, ChronoUnit.MINUTES);

        List<RiskScore> scores = new ArrayList<>();
        for (int i = 0; i < sittings.size(); i++) {
            ExamSessionEntity sitting = sittings.get(i);
            List<ProctoringEventEntity> events = ambientEvents(sitting, questions);

            boolean inCluster = clustered.contains(i);
            if (inCluster) {
                events.addAll(correlatedBurst(sitting, questions, clusterStart));
            } else if (borderline.contains(i)) {
                events.addAll(scatteredPastes(sitting, questions));
            }

            events = proctoringEventRepository.saveAll(events);
            scores.add(scoreSession(sitting, events, inCluster, clusterStart));
        }

        assignPercentiles(scores);
    }

    private List<ProctoringEventEntity> ambientEvents(ExamSessionEntity sitting, List<Question> questions) {
        List<ProctoringEventEntity> events = new ArrayList<>();
        Instant end = sitting.getStartedAt().plus(35, ChronoUnit.MINUTES);

        for (Instant at = sitting.getStartedAt(); at.isBefore(end); at = at.plusSeconds(HEARTBEAT_SECONDS)) {
            events.add(event(sitting, "HEARTBEAT", at, null, Map.of()));
        }

        int ambient = 8 + random.nextInt(14);
        for (int i = 0; i < ambient; i++) {
            Instant at = sitting.getStartedAt().plusSeconds(random.nextInt(35 * 60));
            String type = AMBIENT_EVENTS.get(random.nextInt(AMBIENT_EVENTS.size()));
            Question question = questions.get(random.nextInt(questions.size()));

            Map<String, Object> payload = switch (type) {
                case "WINDOW_BLUR", "TAB_HIDDEN" -> Map.of("durationMs", 400 + random.nextInt(9000));
                case "COPY" -> Map.of("charCount", 10 + random.nextInt(90));
                default -> Map.of();
            };
            events.add(event(sitting, type, at, question, payload));
        }

        return events;
    }

    private List<ProctoringEventEntity> correlatedBurst(ExamSessionEntity sitting, List<Question> questions,
            Instant clusterStart) {
        List<ProctoringEventEntity> burst = new ArrayList<>();
        Question question = questions.get(4);

        for (int i = 0; i < 5 + random.nextInt(3); i++) {
            Instant at = clusterStart.plusSeconds(random.nextInt((int) CLUSTER_WINDOW.toSeconds()));
            burst.add(event(sitting, "PASTE", at, question,
                    Map.of("charCount", 240 + random.nextInt(700))));
            burst.add(event(sitting, "TAB_HIDDEN", at.minusSeconds(2 + random.nextInt(6)), question,
                    Map.of("durationMs", 4000 + random.nextInt(14000))));
        }

        return burst;
    }

    private List<ProctoringEventEntity> scatteredPastes(ExamSessionEntity sitting, List<Question> questions) {
        List<ProctoringEventEntity> pastes = new ArrayList<>();
        for (int i = 0; i < 2 + random.nextInt(2); i++) {
            Instant at = sitting.getStartedAt().plusSeconds(random.nextInt(35 * 60));
            pastes.add(event(sitting, "PASTE", at, questions.get(random.nextInt(questions.size())),
                    Map.of("charCount", 220 + random.nextInt(380))));
        }
        return pastes;
    }

    private ProctoringEventEntity event(ExamSessionEntity sitting, String type, Instant occurredAt,
            Question question, Map<String, Object> payload) {
        UUID clientEventId = new UUID(random.nextLong(), random.nextLong());
        ProctoringEventEntity event = new ProctoringEventEntity(sitting, clientEventId, type, occurredAt);
        event.setQuestion(question);
        event.setPayload(payload);
        return event;
    }

    private static final int LARGE_PASTE_CHARS = 200;

    private RiskScore scoreSession(ExamSessionEntity sitting, List<ProctoringEventEntity> events,
            boolean inCluster, Instant clusterStart) {
        List<Long> largePastes = new ArrayList<>();
        List<Long> awayEvents = new ArrayList<>();

        for (ProctoringEventEntity event : events) {
            Object charCount = event.getPayload().get("charCount");
            if ("PASTE".equals(event.getEventType()) && charCount instanceof Number n
                    && n.intValue() > LARGE_PASTE_CHARS) {
                largePastes.add(event.getId());
            } else if ("TAB_HIDDEN".equals(event.getEventType())
                    || "WINDOW_BLUR".equals(event.getEventType())) {
                awayEvents.add(event.getId());
            }
        }

        List<Map<String, Object>> breakdown = new ArrayList<>();
        double score = 0;

        if (!largePastes.isEmpty()) {
            double weight = Math.min(45, largePastes.size() * 7.5);
            score += weight;
            breakdown.add(signal("LARGE_PASTE", weight, 0.86, largePastes));
        }
        if (!awayEvents.isEmpty()) {
            double weight = Math.min(22, awayEvents.size() * 1.4);
            score += weight;
            breakdown.add(signal("FOCUS_LOSS", weight, 0.54, awayEvents));
        }
        if (inCluster) {
            score += 28;
            breakdown.add(signal("CORRELATED_CLUSTER", 28, 0.91, largePastes));
        }

        RiskScore risk = new RiskScore(sitting);
        risk.setScore(BigDecimal.valueOf(Math.min(99, score)).setScale(2, RoundingMode.HALF_UP));
        risk.setTier(RiskTier.of(Math.min(99, score)));
        risk.setBreakdown(breakdown);

        if (inCluster) {
            raiseFlag(sitting, "CORRELATED_CLUSTER", "0.910", clusterStart,
                    clusterStart.plus(CLUSTER_WINDOW), largePastes);
        } else if (largePastes.size() >= 2) {
            raiseFlag(sitting, "LARGE_PASTE", "0.640", sitting.getStartedAt(),
                    sitting.getExpiresAt(), largePastes);
        }

        return risk;
    }

    private Map<String, Object> signal(String name, double weight, double confidence, List<Long> eventIds) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("signal", name);
        entry.put("weight", BigDecimal.valueOf(weight).setScale(2, RoundingMode.HALF_UP));
        entry.put("confidence", confidence);
        entry.put("eventIds", eventIds);
        return entry;
    }

    private void raiseFlag(ExamSessionEntity sitting, String reason, String confidence,
            Instant windowStart, Instant windowEnd, List<Long> eventIds) {
        Flag flag = new Flag(sitting, reason);
        flag.setConfidence(new BigDecimal(confidence));
        flag.setWindowStart(windowStart);
        flag.setWindowEnd(windowEnd);
        flag.setEventIds(eventIds);
        flagRepository.save(flag);
    }

    private void assignPercentiles(List<RiskScore> scores) {
        List<RiskScore> ranked = new ArrayList<>(scores);
        ranked.sort(Comparator.comparing(RiskScore::getScore));

        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).setCohortPercentile(BigDecimal.valueOf(i * 100.0 / ranked.size())
                    .setScale(2, RoundingMode.HALF_UP));
        }
        riskScoreRepository.saveAll(scores);
    }
}
