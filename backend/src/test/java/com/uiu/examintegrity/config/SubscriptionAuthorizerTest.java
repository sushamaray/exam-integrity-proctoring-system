package com.uiu.examintegrity.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.course.Course;
import com.uiu.examintegrity.course.CourseRepository;
import com.uiu.examintegrity.course.CourseRole;
import com.uiu.examintegrity.course.Enrollment;
import com.uiu.examintegrity.course.EnrollmentRepository;
import com.uiu.examintegrity.exam.domain.code.Language;
import com.uiu.examintegrity.exam.infrastructure.code.InteractiveRunner;
import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.exam.persistence.ExamStatus;
import com.uiu.examintegrity.support.ToolchainProbe;
import com.uiu.examintegrity.user.AppUserDetails;
import com.uiu.examintegrity.user.Role;
import com.uiu.examintegrity.user.User;
import com.uiu.examintegrity.user.UserRepository;

/**
 * Guards the one genuinely risky edit in the console work: {@code /ws/**} is no longer staff-only
 * at the HTTP layer, so <strong>this</strong> is what keeps a student away from proctoring data
 * and away from other students' programs.
 */
@SpringBootTest
@Transactional
class SubscriptionAuthorizerTest {
    @Autowired private SubscriptionAuthorizer authorizer;
    @Autowired private InteractiveRunner runner;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private ExamRepository examRepository;

    private User student;
    private User otherStudent;
    private User faculty;
    private Exam exam;

    @BeforeEach
    void fixtures() {
        student = userRepository.save(new User("Sub Student", "sub-student@uiu.ac.bd", "x", Role.STUDENT));
        otherStudent = userRepository.save(new User("Sub Other", "sub-other@uiu.ac.bd", "x", Role.STUDENT));
        faculty = userRepository.save(new User("Sub Faculty", "sub-faculty@uiu.ac.bd", "x", Role.FACULTY));

        Course course = courseRepository.save(new Course("TST 7777", "Subscription test course"));
        enrollmentRepository.save(new Enrollment(course, student, CourseRole.STUDENT));
        enrollmentRepository.save(new Enrollment(course, otherStudent, CourseRole.STUDENT));
        enrollmentRepository.save(new Enrollment(course, faculty, CourseRole.INSTRUCTOR));

        Instant now = Instant.now();
        exam = new Exam(course, "Subscription test exam", now.minus(5, ChronoUnit.MINUTES),
                now.plus(2, ChronoUnit.HOURS), 45);
        exam.setStatus(ExamStatus.LIVE);
        exam = examRepository.save(exam);
    }

    @AfterEach
    void cleanUp() {
        runner.liveRunIds().forEach(runId -> runner.stop(runId, "test teardown"));
    }

    private static Principal as(User who) {
        AppUserDetails details = new AppUserDetails(who);
        return new UsernamePasswordAuthenticationToken(details, "n/a", details.getAuthorities());
    }

    private static String riskTopic(Long examId) {
        return "/topic/exams/" + examId + "/risk";
    }

    // ---- these hold with or without a compiler ----

    @Test
    @DisplayName("a student still cannot read proctoring risk, which is the whole point of the old rule")
    void studentsCannotReadRiskTopics() {
        assertThatThrownBy(() -> authorizer.authorize(riskTopic(exam.getId()), as(student)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("staff on the course still can")
    void staffCanStillReadRiskTopics() {
        assertThatCode(() -> authorizer.authorize(riskTopic(exam.getId()), as(faculty)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("anything that is not a known topic is refused outright")
    void unknownDestinationsAreRefused() {
        for (String destination : new String[] {
                null, "", "/topic", "/topic/console", "/topic/console/not-a-uuid",
                "/topic/exams/1/risk/extra", "/topic/**", "/queue/anything",
        }) {
            assertThatThrownBy(() -> authorizer.authorize(destination, as(student)))
                    .as("destination %s", destination)
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    @DisplayName("a console topic with no run behind it is refused, so ids cannot be fished for")
    void unknownRunIsRefused() {
        String nobodys = "/topic/console/" + java.util.UUID.randomUUID();

        assertThatThrownBy(() -> authorizer.authorize(nobodys, as(student)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("an anonymous or unrecognised principal is refused")
    void anonymousIsRefused() {
        assertThatThrownBy(() -> authorizer.authorize(riskTopic(exam.getId()), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> authorizer.authorize(riskTopic(exam.getId()), () -> "someone"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---- needs a compiler, because it needs a real reserved run ----

    @Test
    @DisplayName("only the student who owns a run may read its console; nobody else, staff included")
    void consoleTopicBelongsToItsOwner() {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");

        String runId = runner.reserve(InteractiveRunner.sessionScope(1L), student.getId(), Language.C,
                "int main(void) { return 0; }");
        String topic = "/topic/console/" + runId;

        assertThat(runner.ownerOf(runId)).contains(student.getId());
        assertThatCode(() -> authorizer.authorize(topic, as(student))).doesNotThrowAnyException();
        assertThatThrownBy(() -> authorizer.authorize(topic, as(otherStudent)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> authorizer.authorize(topic, as(faculty)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("once a run ends its topic stops being readable, rather than lingering")
    void endedRunsAreNoLongerReadable() {
        assumeTrue(ToolchainProbe.hasGcc(), "no C compiler on this machine");

        String runId = runner.reserve(InteractiveRunner.sessionScope(1L), student.getId(), Language.C,
                "int main(void) { return 0; }");
        String topic = "/topic/console/" + runId;
        assertThatCode(() -> authorizer.authorize(topic, as(student))).doesNotThrowAnyException();

        runner.stop(runId, "test");

        assertThatThrownBy(() -> authorizer.authorize(topic, as(student)))
                .isInstanceOf(AccessDeniedException.class);
    }
}
