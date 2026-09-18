package com.uiu.examintegrity.exam.api;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.uiu.examintegrity.exam.api.dto.SaveAnswerRequest;
import com.uiu.examintegrity.exam.application.ReviewService;
import com.uiu.examintegrity.exam.application.SessionService;
import com.uiu.examintegrity.exam.application.view.OpenExamView;
import com.uiu.examintegrity.exam.application.view.ReviewView;
import com.uiu.examintegrity.exam.application.view.SavedAnswer;
import com.uiu.examintegrity.exam.application.view.SessionView;
import com.uiu.examintegrity.exam.application.view.SittingSummary;
import com.uiu.examintegrity.exam.application.view.SubmitResult;
import com.uiu.examintegrity.user.AppUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class SessionController {
    private final SessionService sessionService;
    private final ReviewService reviewService;

    public SessionController(SessionService sessionService, ReviewService reviewService) {
        this.sessionService = sessionService;
        this.reviewService = reviewService;
    }

    @GetMapping("/api/student/exams")
    public List<OpenExamView> openExams(@AuthenticationPrincipal AppUserDetails student) {
        return sessionService.listOpenExams(student.getId());
    }

    @PostMapping("/api/exams/{examId}/sessions")
    public SessionView start(@PathVariable Long examId, @AuthenticationPrincipal AppUserDetails student,
            HttpServletRequest request) {
        return sessionService.startOrResume(examId, student.getId(),
                request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    @GetMapping("/api/sessions/{id}")
    public SessionView resume(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails student) {
        return sessionService.resume(id, student.getId());
    }

    @PutMapping("/api/sessions/{id}/answers/{questionId}")
    public SavedAnswer saveAnswer(@PathVariable Long id, @PathVariable Long questionId,
            @Valid @RequestBody SaveAnswerRequest body, @AuthenticationPrincipal AppUserDetails student) {
        return sessionService.saveAnswer(id, student.getId(), questionId, body.toValue());
    }

    @PostMapping("/api/sessions/{id}/submit")
    public SubmitResult submit(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails student) {
        return sessionService.submit(id, student.getId());
    }

    @GetMapping("/api/student/sittings")
    public List<SittingSummary> sittings(@AuthenticationPrincipal AppUserDetails student) {
        return reviewService.sittings(student.getId());
    }

    @GetMapping("/api/sessions/{id}/review")
    public ReviewView review(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails student) {
        return reviewService.review(id, student.getId());
    }
}
