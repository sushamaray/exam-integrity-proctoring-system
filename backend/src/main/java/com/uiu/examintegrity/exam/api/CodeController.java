package com.uiu.examintegrity.exam.api;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.uiu.examintegrity.exam.api.dto.RunCodeRequest;
import com.uiu.examintegrity.exam.application.CodeRunService;
import com.uiu.examintegrity.exam.application.view.CodeRunView;
import com.uiu.examintegrity.exam.application.view.LanguageOption;
import com.uiu.examintegrity.user.AppUserDetails;

import jakarta.validation.Valid;

/**
 * Running and submitting code.
 *
 * <p>Both run paths sit under {@code /api/sessions/**}, which {@code SecurityConfig} already
 * restricts to {@code ROLE_STUDENT}, so no new rule was needed. The language list falls
 * through to the {@code /api/**} authenticated rule so faculty can read it too.
 */
@RestController
public class CodeController {
    private final CodeRunService codeRunService;

    public CodeController(CodeRunService codeRunService) {
        this.codeRunService = codeRunService;
    }

    @GetMapping("/api/code/languages")
    public List<LanguageOption> languages() {
        return codeRunService.languages();
    }

    /** Sample cases only, and not kept for grading. */
    @PostMapping("/api/sessions/{id}/code/{questionId}/run")
    public CodeRunView run(@PathVariable Long id, @PathVariable Long questionId,
            @Valid @RequestBody RunCodeRequest body, @AuthenticationPrincipal AppUserDetails student) {
        return codeRunService.run(id, student.getId(), questionId, body.language(), body.sourceCode(), false);
    }

    /** Every case including hidden ones, stored as the submission the grade comes from. */
    @PostMapping("/api/sessions/{id}/code/{questionId}/submit")
    public CodeRunView submit(@PathVariable Long id, @PathVariable Long questionId,
            @Valid @RequestBody RunCodeRequest body, @AuthenticationPrincipal AppUserDetails student) {
        return codeRunService.run(id, student.getId(), questionId, body.language(), body.sourceCode(), true);
    }
}
