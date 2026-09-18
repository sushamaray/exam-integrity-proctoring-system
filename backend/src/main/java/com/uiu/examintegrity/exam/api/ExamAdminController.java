package com.uiu.examintegrity.exam.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.uiu.examintegrity.common.BadRequestException;
import com.uiu.examintegrity.exam.application.ExamImportService;
import com.uiu.examintegrity.exam.application.ExamImportService.CourseOption;
import com.uiu.examintegrity.exam.application.ExamImportService.NewExam;
import com.uiu.examintegrity.proctoring.application.MonitorService;
import com.uiu.examintegrity.proctoring.application.view.FacultyExamRow;
import com.uiu.examintegrity.user.AppUserDetails;

@RestController
public class ExamAdminController {
    private final ExamImportService importService;
    private final MonitorService monitorService;

    public ExamAdminController(ExamImportService importService, MonitorService monitorService) {
        this.importService = importService;
        this.monitorService = monitorService;
    }

    @GetMapping("/api/faculty/courses")
    public List<CourseOption> courses(@AuthenticationPrincipal AppUserDetails staff) {
        return importService.staffedCourses(staff.getId());
    }

    @PostMapping(value = "/api/faculty/exams", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FacultyExamRow create(@RequestParam Long courseId,
            @RequestParam String title,
            @RequestParam(required = false) Instant startsAt,
            @RequestParam int durationMinutes,
            @RequestParam(defaultValue = "24") int availableHours,
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal AppUserDetails staff) throws IOException {
        if (title.isBlank() || title.length() > 200) {
            throw new BadRequestException("Give the exam a title of up to 200 characters");
        }
        if (durationMinutes < 1 || durationMinutes > 300) {
            throw new BadRequestException("Duration must be between 1 and 300 minutes");
        }
        if (availableHours < 1 || availableHours > 168) {
            throw new BadRequestException("The exam can be open for 1 to 168 hours");
        }
        if (durationMinutes > availableHours * 60) {
            throw new BadRequestException("The duration is longer than the time the exam is open");
        }
        if (file.isEmpty()) {
            throw new BadRequestException("Choose a CSV file of questions");
        }

        Long examId = importService.create(staff.getId(),
                new NewExam(courseId, title, startsAt, durationMinutes, availableHours),
                new String(file.getBytes(), StandardCharsets.UTF_8));
        return monitorService.row(examId);
    }
}
