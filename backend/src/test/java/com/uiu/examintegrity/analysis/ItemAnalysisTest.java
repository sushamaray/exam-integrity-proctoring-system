package com.uiu.examintegrity.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.analysis.application.ItemAnalysisService;
import com.uiu.examintegrity.analysis.application.view.ItemAnalysisView;
import com.uiu.examintegrity.analysis.persistence.ItemStatsRepository;
import com.uiu.examintegrity.common.NotFoundException;
import com.uiu.examintegrity.course.Course;
import com.uiu.examintegrity.course.CourseRepository;
import com.uiu.examintegrity.course.CourseRole;
import com.uiu.examintegrity.course.Enrollment;
import com.uiu.examintegrity.course.EnrollmentRepository;
import com.uiu.examintegrity.exam.persistence.AnswerEntity;
import com.uiu.examintegrity.exam.persistence.AnswerRepository;
import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.exam.persistence.ExamQuestion;
import com.uiu.examintegrity.exam.persistence.ExamQuestionRepository;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.exam.persistence.ExamSessionEntity;
import com.uiu.examintegrity.exam.persistence.ExamSessionRepository;
import com.uiu.examintegrity.exam.persistence.ExamStatus;
import com.uiu.examintegrity.exam.persistence.SessionStatus;
import com.uiu.examintegrity.question.Question;
import com.uiu.examintegrity.question.QuestionOption;
import com.uiu.examintegrity.question.QuestionOptionRepository;
import com.uiu.examintegrity.question.QuestionRepository;
import com.uiu.examintegrity.question.QuestionType;
import com.uiu.examintegrity.user.AppUserDetails;
import com.uiu.examintegrity.user.Role;
import com.uiu.examintegrity.user.User;
import com.uiu.examintegrity.user.UserRepository;

/**
 * Item analysis end to end.
 *
 * <p>The fixture is deliberately rigged rather than random: four students, one question the strong
 * half gets right and one that only the weak half does, so discrimination has a known sign.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemAnalysisTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ItemAnalysisService service;
    @Autowired private ItemStatsRepository itemStatsRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionOptionRepository optionRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private ExamQuestionRepository examQuestionRepository;
    @Autowired private ExamSessionRepository sessionRepository;
    @Autowired private AnswerRepository answerRepository;

    private User faculty;
    private User outsider;
    private User student;
    private Exam exam;
    private Question sound;
    private Question broken;
    private Question ungraded;
    private final Map<Long, QuestionOption> rightOption = new LinkedHashMap<>();

    @BeforeEach
    void fixtures() {
        faculty = userRepository.save(new User("IA Faculty", "ia-faculty@uiu.ac.bd", "x", Role.FACULTY));
        outsider = userRepository.save(new User("IA Outsider", "ia-outsider@uiu.ac.bd", "x", Role.FACULTY));
        student = userRepository.save(new User("IA Student", "ia-student@uiu.ac.bd", "x", Role.STUDENT));

        Course course = courseRepository.save(new Course("TST 5555", "Item analysis course"));
        enrollmentRepository.save(new Enrollment(course, faculty, CourseRole.INSTRUCTOR));
        enrollmentRepository.save(new Enrollment(course, student, CourseRole.STUDENT));

        sound = mcq(course, "Sound item");
        broken = mcq(course, "Broken item");
        ungraded = questionRepository.save(
                new Question(course, QuestionType.SHORT_ANSWER, "Nobody has marked this", new BigDecimal("3")));

        Instant now = Instant.now();
        exam = new Exam(course, "Item analysis exam", now.minus(2, ChronoUnit.HOURS),
                now.minus(1, ChronoUnit.HOURS), 60);
        exam.setStatus(ExamStatus.CLOSED);
        exam = examRepository.save(exam);
        examQuestionRepository.save(new ExamQuestion(exam, sound, BigDecimal.ONE));
        examQuestionRepository.save(new ExamQuestion(exam, broken, BigDecimal.ONE));
        examQuestionRepository.save(new ExamQuestion(exam, ungraded, BigDecimal.ONE));
    }

    private Question mcq(Course course, String body) {
        Question question = questionRepository.save(new Question(course, QuestionType.MCQ, body, BigDecimal.ONE));
        List<QuestionOption> options = optionRepository.saveAll(List.of(
                new QuestionOption(question, "a", "first", false),
                new QuestionOption(question, "b", "second", true),
                new QuestionOption(question, "c", "third", false),
                new QuestionOption(question, "d", "fourth", false)));
        rightOption.put(question.getId(), options.get(1));
        return question;
    }

    /**
     * @param soundRight    whether this sitting got the sound item right
     * @param brokenRight   whether it got the broken one right -- rigged to be the opposite
     * @param totalMarks    what ranks them into the strong and weak groups
     */
    private ExamSessionEntity sitting(String email, boolean soundRight, boolean brokenRight, String totalMarks) {
        User who = userRepository.save(new User("S " + email, email, "x", Role.STUDENT));
        enrollmentRepository.save(new Enrollment(exam.getCourse(), who, CourseRole.STUDENT));

        ExamSessionEntity session = new ExamSessionEntity(exam, who);
        session.setStatus(SessionStatus.SUBMITTED);
        session.setStartedAt(Instant.now().minus(90, ChronoUnit.MINUTES));
        session.setExpiresAt(Instant.now().minus(30, ChronoUnit.MINUTES));
        session.setSubmittedAt(Instant.now().minus(31, ChronoUnit.MINUTES));
        session.setQuestionOrder(List.of(sound.getId(), broken.getId(), ungraded.getId()));
        session.setTotalMarks(new BigDecimal(totalMarks));
        session = sessionRepository.save(session);

        answerRepository.save(answer(session, sound, soundRight));
        answerRepository.save(answer(session, broken, brokenRight));

        // Written, answered, and never graded: marks_awarded stays null.
        AnswerEntity pending = new AnswerEntity();
        pending.setSession(session);
        pending.setQuestion(ungraded);
        pending.setTextAnswer("an essay nobody has read");
        pending.setFirstSeenAt(Instant.now().minus(80, ChronoUnit.MINUTES));
        pending.setAnsweredAt(Instant.now().minus(79, ChronoUnit.MINUTES));
        answerRepository.save(pending);

        return session;
    }

    private AnswerEntity answer(ExamSessionEntity session, Question question, boolean right) {
        AnswerEntity answer = new AnswerEntity();
        answer.setSession(session);
        answer.setQuestion(question);
        answer.setOption(right ? rightOption.get(question.getId())
                : optionRepository.findByQuestionIdAndLabel(question.getId(), "a").orElseThrow());
        answer.setMarksAwarded(right ? BigDecimal.ONE : BigDecimal.ZERO);
        answer.setFirstSeenAt(Instant.now().minus(80, ChronoUnit.MINUTES));
        answer.setAnsweredAt(Instant.now().minus(79, ChronoUnit.MINUTES));
        return answer;
    }

    private void cohort() {
        // Strong students: get the sound item right, the broken one wrong.
        sitting("ia-top1@uiu.ac.bd", true, false, "9");
        sitting("ia-top2@uiu.ac.bd", true, false, "8");
        // Weak students: the other way round.
        sitting("ia-low1@uiu.ac.bd", false, true, "2");
        sitting("ia-low2@uiu.ac.bd", false, true, "1");
    }

    @Test
    @DisplayName("discrimination is positive for a sound item and negative for a miskeyed one")
    void discriminationHasTheRightSign() {
        cohort();

        ItemAnalysisView view = service.recompute(exam.getId(), faculty.getId());

        ItemAnalysisView.Item soundItem = itemFor(view, sound.getId());
        ItemAnalysisView.Item brokenItem = itemFor(view, broken.getId());

        assertThat(soundItem.discrimination()).isPositive();
        assertThat(brokenItem.discrimination())
                .as("the weaker half outscored the stronger half, which is what a broken item looks like")
                .isNegative();
        assertThat(soundItem.difficulty()).isEqualByComparingTo("0.5000");
        assertThat(view.sittings()).isEqualTo(4);
    }

    @Test
    @DisplayName("an answer nobody has marked is left out, not scored zero")
    void ungradedAnswersAreExcluded() {
        cohort();

        ItemAnalysisView view = service.recompute(exam.getId(), faculty.getId());

        assertThat(view.items()).extracting(ItemAnalysisView.Item::questionId)
                .as("the written question was answered by everyone but graded by nobody")
                .doesNotContain(ungraded.getId())
                .contains(sound.getId(), broken.getId());
    }

    @Test
    @DisplayName("the option spread marks the correct answer, for MCQ only")
    void optionSharesMarkTheCorrectAnswer() {
        cohort();

        ItemAnalysisView.Item item = itemFor(service.recompute(exam.getId(), faculty.getId()), sound.getId());

        assertThat(item.options()).hasSize(2);
        assertThat(item.options()).filteredOn(ItemAnalysisView.OptionShare::correct)
                .singleElement()
                .satisfies(share -> {
                    assertThat(share.label()).isEqualTo("b");
                    assertThat(share.share()).isEqualByComparingTo("0.5000");
                });
    }

    @Test
    @DisplayName("a question only half the cohort was shown is averaged over those who saw it")
    void poolingDoesNotPenaliseUnseenQuestions() {
        sitting("ia-top1@uiu.ac.bd", true, false, "9");
        sitting("ia-top2@uiu.ac.bd", true, false, "8");
        sitting("ia-low1@uiu.ac.bd", false, true, "2");

        // A fourth sitting that never saw either question at all.
        User who = userRepository.save(new User("S skip", "ia-skip@uiu.ac.bd", "x", Role.STUDENT));
        enrollmentRepository.save(new Enrollment(exam.getCourse(), who, CourseRole.STUDENT));
        ExamSessionEntity blank = new ExamSessionEntity(exam, who);
        blank.setStatus(SessionStatus.SUBMITTED);
        blank.setQuestionOrder(List.of());
        blank.setTotalMarks(BigDecimal.ZERO);
        sessionRepository.save(blank);

        ItemAnalysisView.Item item = itemFor(service.recompute(exam.getId(), faculty.getId()), sound.getId());

        assertThat(item.attempts())
                .as("only the three who were shown it count")
                .isEqualTo(3);
        assertThat(item.difficulty())
                .as("2 of 3, not 2 of 4 -- the empty sitting must not look like a wrong answer")
                .isEqualByComparingTo("0.6667");
    }

    @Test
    @DisplayName("recomputing replaces the rows rather than appending a second set")
    void recomputeIsIdempotent() {
        cohort();

        service.recompute(exam.getId(), faculty.getId());
        int afterFirst = itemStatsRepository.findByExamId(exam.getId()).size();
        service.recompute(exam.getId(), faculty.getId());

        assertThat(afterFirst).isEqualTo(2);
        assertThat(itemStatsRepository.findByExamId(exam.getId())).hasSize(2);
    }

    @Test
    @DisplayName("faculty who do not staff the course are told it is missing, not forbidden")
    void nonStaffIsNotFound() {
        cohort();

        assertThatThrownBy(() -> service.view(exam.getId(), outsider.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.recompute(exam.getId(), outsider.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("a student cannot read item analysis -- it is close enough to an answer key")
    void studentsAreForbidden() throws Exception {
        mockMvc.perform(get("/api/exams/{id}/item-analysis", exam.getId())
                        .with(user(new AppUserDetails(student))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/exams/{id}/item-analysis/run", exam.getId())
                        .with(csrf()).with(user(new AppUserDetails(student))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("staff can read and run it")
    void staffCanUseIt() throws Exception {
        cohort();

        mockMvc.perform(post("/api/exams/{id}/item-analysis/run", exam.getId())
                        .with(csrf()).with(user(new AppUserDetails(faculty))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/exams/{id}/item-analysis", exam.getId())
                        .with(user(new AppUserDetails(faculty))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("an exam nobody has sat reports nothing rather than failing")
    void noSittingsIsEmpty() {
        ItemAnalysisView view = service.recompute(exam.getId(), faculty.getId());

        assertThat(view.items()).isEmpty();
        assertThat(view.sittings()).isZero();
    }

    private static ItemAnalysisView.Item itemFor(ItemAnalysisView view, Long questionId) {
        return view.items().stream()
                .filter(item -> item.questionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no item for question " + questionId
                        + " in " + new ArrayList<>(view.items())));
    }
}
