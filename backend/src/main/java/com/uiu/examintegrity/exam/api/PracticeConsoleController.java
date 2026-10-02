package com.uiu.examintegrity.exam.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.uiu.examintegrity.exam.api.dto.ConsoleInputRequest;
import com.uiu.examintegrity.exam.api.dto.RunCodeRequest;
import com.uiu.examintegrity.exam.application.ConsoleService;
import com.uiu.examintegrity.exam.application.view.ConsoleStarted;
import com.uiu.examintegrity.user.AppUserDetails;

import jakarta.validation.Valid;

/**
 * The compiler outside any exam: write C, run it, type at it. Nothing is timed, graded, proctored
 * or stored.
 *
 * <p>These sit under {@code /api/code/**} rather than {@code /api/sessions/**} precisely because
 * there is no sitting to belong to, which also means they fall through to the
 * {@code /api/**} authenticated rule and are open to faculty as well as students -- deliberate,
 * so staff can sanity-check a solution before setting a question.
 *
 * <p>That makes this the widest surface for running arbitrary code in the app, with no exam window
 * bounding it. What holds it back is unchanged: the {@code SourceGuard}, one live program per
 * person, the global ceiling, and the idle and wall-clock caps in {@code ConsoleProperties}.
 *
 * <p>Start is two calls for the same reason as in an exam -- see {@link ConsoleController}.
 */
@RestController
public class PracticeConsoleController {
    private final ConsoleService consoleService;

    public PracticeConsoleController(ConsoleService consoleService) {
        this.consoleService = consoleService;
    }

    @PostMapping("/api/code/console")
    public ConsoleStarted prepare(@Valid @RequestBody RunCodeRequest body,
            @AuthenticationPrincipal AppUserDetails account) {
        return consoleService.preparePractice(account.getId(), body.language(), body.sourceCode());
    }

    @PostMapping("/api/code/console/{runId}/start")
    public ResponseEntity<Void> start(@PathVariable String runId,
            @AuthenticationPrincipal AppUserDetails account) {
        consoleService.launch(runId, account.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/code/console/{runId}/stdin")
    public ResponseEntity<Void> stdin(@PathVariable String runId, @Valid @RequestBody ConsoleInputRequest body,
            @AuthenticationPrincipal AppUserDetails account) {
        consoleService.write(runId, account.getId(), body.line());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/code/console/{runId}")
    public ResponseEntity<Void> stop(@PathVariable String runId,
            @AuthenticationPrincipal AppUserDetails account) {
        consoleService.stop(runId, account.getId());
        return ResponseEntity.noContent().build();
    }
}
