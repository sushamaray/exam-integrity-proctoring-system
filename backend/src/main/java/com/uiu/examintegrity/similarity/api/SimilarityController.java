package com.uiu.examintegrity.similarity.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uiu.examintegrity.similarity.application.SimilarityService;
import com.uiu.examintegrity.similarity.application.view.GraphView;
import com.uiu.examintegrity.similarity.application.view.PairDetailView;
import com.uiu.examintegrity.user.AppUserDetails;

@RestController
public class SimilarityController {
    private final SimilarityService similarityService;

    public SimilarityController(SimilarityService similarityService) {
        this.similarityService = similarityService;
    }

    @PostMapping("/api/exams/{id}/similarity/run")
    public GraphView run(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails staff) {
        return similarityService.run(id, staff.getId());
    }

    @GetMapping("/api/exams/{id}/similarity")
    public GraphView graph(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails staff) {
        return similarityService.graph(id, staff.getId());
    }

    @GetMapping("/api/similarity/pairs/{pairId}")
    public PairDetailView pair(@PathVariable Long pairId, @AuthenticationPrincipal AppUserDetails staff) {
        return similarityService.pair(pairId, staff.getId());
    }
}
