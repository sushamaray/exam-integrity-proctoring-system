package com.uiu.examintegrity.analysis.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uiu.examintegrity.analysis.application.ItemAnalysisService;
import com.uiu.examintegrity.analysis.application.view.ItemAnalysisView;
import com.uiu.examintegrity.user.AppUserDetails;

/**
 * How each question on an exam performed.
 *
 * <p>Shaped like {@code SimilarityController}: a GET that serves whatever was last computed, and an
 * explicit run, because these numbers only mean anything once a cohort has actually sat the paper.
 *
 * <p>Staff only, and enforced twice over -- by the URL rule in {@code SecurityConfig} and again by
 * {@code CourseAccess.requireStaff} per exam. The response carries the spread of answers across
 * every option, which is close enough to an answer key to be worth guarding properly.
 */
@RestController
public class ItemAnalysisController {
    private final ItemAnalysisService itemAnalysisService;

    public ItemAnalysisController(ItemAnalysisService itemAnalysisService) {
        this.itemAnalysisService = itemAnalysisService;
    }

    @PostMapping("/api/exams/{id}/item-analysis/run")
    public ItemAnalysisView run(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails staff) {
        return itemAnalysisService.recompute(id, staff.getId());
    }

    @GetMapping("/api/exams/{id}/item-analysis")
    public ItemAnalysisView view(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails staff) {
        return itemAnalysisService.view(id, staff.getId());
    }
}
