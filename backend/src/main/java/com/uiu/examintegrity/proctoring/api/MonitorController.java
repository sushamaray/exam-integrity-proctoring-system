package com.uiu.examintegrity.proctoring.api;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.uiu.examintegrity.proctoring.api.dto.DecisionRequest;
import com.uiu.examintegrity.proctoring.application.FlagReviewService;
import com.uiu.examintegrity.proctoring.application.MonitorService;
import com.uiu.examintegrity.proctoring.application.view.FacultyExamRow;
import com.uiu.examintegrity.proctoring.application.view.FlagView;
import com.uiu.examintegrity.proctoring.application.view.MonitorView;
import com.uiu.examintegrity.proctoring.application.view.QueueRow;
import com.uiu.examintegrity.proctoring.application.view.RiskDetailView;
import com.uiu.examintegrity.user.AppUserDetails;

import jakarta.validation.Valid;

@RestController
public class MonitorController {
    private final MonitorService monitorService;
    private final FlagReviewService flagReviewService;

    public MonitorController(MonitorService monitorService, FlagReviewService flagReviewService) {
        this.monitorService = monitorService;
        this.flagReviewService = flagReviewService;
    }

    @GetMapping("/api/faculty/exams")
    public List<FacultyExamRow> exams(@AuthenticationPrincipal AppUserDetails staff) {
        return monitorService.exams(staff.getId());
    }

    @GetMapping("/api/exams/{id}/monitor")
    public MonitorView monitor(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails staff) {
        return monitorService.monitor(id, staff.getId());
    }

    @GetMapping("/api/sessions/{id}/risk")
    public RiskDetailView risk(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails staff) {
        return monitorService.riskDetail(id, staff.getId());
    }

    @GetMapping("/api/faculty/review-queue")
    public List<QueueRow> reviewQueue(@AuthenticationPrincipal AppUserDetails staff) {
        return flagReviewService.queue(staff.getId());
    }

    @PostMapping("/api/faculty/sessions/{id}/decision")
    public FlagView decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest body,
            @AuthenticationPrincipal AppUserDetails staff) {
        return flagReviewService.decide(id, staff.getId(), body.decision(), body.note());
    }
}
