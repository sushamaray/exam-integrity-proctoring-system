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
 * Driving a program interactively.
 *
 * <p>Output streams back over STOMP, but everything inbound is ordinary HTTP rather than a
 * {@code @MessageMapping}. That is deliberate: these paths sit under {@code /api/sessions/**},
 * so they inherit the existing {@code ROLE_STUDENT} rule, CSRF handling and the global error
 * mapping, and no authorization has to be reimplemented for inbound STOMP frames. A request per
 * typed <em>line</em> is cheap -- this is not per keystroke.
 *
 * <p>Starting is two calls on purpose. {@code POST .../console} compiles and reserves the run;
 * the client subscribes to its topic; {@code POST .../start} then launches it. The broker has no
 * replay, so a program launched before the client was listening would lose its first output --
 * which, for a program that prompts and exits quickly, is all of it.
 */
@RestController
public class ConsoleController {
    private final ConsoleService consoleService;

    public ConsoleController(ConsoleService consoleService) {
        this.consoleService = consoleService;
    }

    /** Compiles and reserves. Returns a null runId with diagnostics when it did not build. */
    @PostMapping("/api/sessions/{id}/code/{questionId}/console")
    public ConsoleStarted prepare(@PathVariable Long id, @PathVariable Long questionId,
            @Valid @RequestBody RunCodeRequest body, @AuthenticationPrincipal AppUserDetails student) {
        return consoleService.prepare(id, student.getId(), questionId, body.language(), body.sourceCode());
    }

    @PostMapping("/api/sessions/{id}/console/{runId}/start")
    public ResponseEntity<Void> start(@PathVariable Long id, @PathVariable String runId,
            @AuthenticationPrincipal AppUserDetails student) {
        consoleService.launch(runId, student.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/sessions/{id}/console/{runId}/stdin")
    public ResponseEntity<Void> stdin(@PathVariable Long id, @PathVariable String runId,
            @Valid @RequestBody ConsoleInputRequest body, @AuthenticationPrincipal AppUserDetails student) {
        consoleService.write(runId, student.getId(), body.line());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/sessions/{id}/console/{runId}")
    public ResponseEntity<Void> stop(@PathVariable Long id, @PathVariable String runId,
            @AuthenticationPrincipal AppUserDetails student) {
        consoleService.stop(runId, student.getId());
        return ResponseEntity.noContent().build();
    }
}
