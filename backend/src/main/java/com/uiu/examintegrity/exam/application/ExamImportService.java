package com.uiu.examintegrity.exam.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uiu.examintegrity.common.NotFoundException;
import com.uiu.examintegrity.course.Course;
import com.uiu.examintegrity.course.CourseAccess;
import com.uiu.examintegrity.course.CourseRepository;
import com.uiu.examintegrity.exam.domain.csv.ParsedQuestion;
import com.uiu.examintegrity.exam.domain.csv.QuestionCsvParser;
import com.uiu.examintegrity.exam.persistence.Exam;
import com.uiu.examintegrity.exam.persistence.ExamQuestion;
import com.uiu.examintegrity.exam.persistence.ExamQuestionRepository;
import com.uiu.examintegrity.exam.persistence.ExamRepository;
import com.uiu.examintegrity.exam.persistence.ExamStatus;
import com.uiu.examintegrity.question.Question;
import com.uiu.examintegrity.question.QuestionOption;
import com.uiu.examintegrity.question.QuestionOptionRepository;
import com.uiu.examintegrity.question.QuestionRepository;
import com.uiu.examintegrity.user.User;
import com.uiu.examintegrity.user.UserRepository;

@Service
public class ExamImportService {
    private static final List<String> LABELS = List.of("a", "b", "c", "d");

    public record NewExam(Long courseId, String title, Instant startsAt, int durationMinutes, int availableHours) {
    }

    private final CourseRepository courseRepository;
    private final CourseAccess courseAccess;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ExamImportService(CourseRepository courseRepository, CourseAccess courseAccess,
            QuestionRepository questionRepository, QuestionOptionRepository optionRepository,
            ExamRepository examRepository, ExamQuestionRepository examQuestionRepository,
            UserRepository userRepository, Clock clock) {
        this.courseRepository = courseRepository;
        this.courseAccess = courseAccess;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.examRepository = examRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public Long create(Long staffId, NewExam form, String csv) {
        courseAccess.requireStaff(form.courseId(), staffId);
        Course course = courseRepository.findById(form.courseId())
                .orElseThrow(() -> new NotFoundException("Not found"));

        List<ParsedQuestion> parsed = QuestionCsvParser.parse(csv);

        User author = userRepository.getReferenceById(staffId);
        Instant now = clock.instant();
        Instant startsAt = form.startsAt() == null ? now : form.startsAt();

        Exam exam = new Exam(course, form.title().strip(), startsAt,
                startsAt.plus(Duration.ofHours(form.availableHours())), form.durationMinutes());
        exam.setStatus(startsAt.isAfter(now) ? ExamStatus.SCHEDULED : ExamStatus.LIVE);
        exam.setCreatedBy(author);
        exam = examRepository.save(exam);

        List<ExamQuestion> pool = new ArrayList<>();
        for (ParsedQuestion row : parsed) {
            Question question = new Question(course, row.type(), row.question(), row.marks());
            question.setTopic(row.topic());
            question.setCreatedBy(author);
            question = questionRepository.save(question);
            pool.add(new ExamQuestion(exam, question, row.marks()));

            if (row.isWritten()) {
                continue;
            }
            List<QuestionOption> options = new ArrayList<>();
            for (int i = 0; i < LABELS.size(); i++) {
                String label = LABELS.get(i);
                options.add(new QuestionOption(question, label, row.options().get(i), label.equals(row.correctLabel())));
            }
            optionRepository.saveAll(options);
        }
        examQuestionRepository.saveAll(pool);
        return exam.getId();
    }

    public record CourseOption(Long id, String code, String title) {
    }

    @Transactional(readOnly = true)
    public List<CourseOption> staffedCourses(Long staffId) {
        return courseRepository.findAllById(courseAccess.staffedCourseIds(staffId)).stream()
                .map(c -> new CourseOption(c.getId(), c.getCode(), c.getTitle()))
                .toList();
    }
}
